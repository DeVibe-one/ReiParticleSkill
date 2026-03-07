// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.helper.SequencedCompositionAnimationHelper;
import com.reiasu.reiparticlesapi.utils.storage.Memo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

/**
 * Abstract composition that manages particles in a sequenced (indexed) manner.
 * <p>
 * Each particle slot is tracked by a bit in a {@code long[]} bit-set. Particles
 * can be added/removed one at a time via {@link #addSingle()}/{@link #removeSingle()},
 * or in batches via {@link #addMultiple(int)}/{@link #removeMultiple(int)}.
 * <p>
 * The {@link #getAnimate() animate} helper allows defining predicate-gated
 * animation steps that automatically add/remove particles over time.
 */
public abstract class SequencedParticleComposition extends ParticleComposition {

    // ─── Static encode/decode ────────────────────────────────────────────

    public static void encodeBase(SequencedParticleComposition data, FriendlyByteBuf buf) {
        ParticleComposition.encodeBase(data, buf);
        buf.writeInt(data.count);
        buf.writeInt(data.displayedParticleCount);
        buf.writeInt(data.serverCurrentIndex);
        buf.writeLongArray(data.index.get());
    }

    public static void decodeBase(SequencedParticleComposition instance, FriendlyByteBuf buf) {
        ParticleComposition.decodeBase(instance, buf);
        instance.count = buf.readInt();
        instance.displayedParticleCount = buf.readInt();
        instance.serverCurrentIndex = buf.readInt();
        instance.index.setMemoValue(buf.readLongArray());
    }

    // ─── Fields ──────────────────────────────────────────────────────────

    private final SequencedCompositionAnimationHelper<SequencedParticleComposition> animate;
    private int count;
    private Memo<long[]> index;
    private int displayedParticleCount;
    private int serverCurrentIndex;
    private final ArrayList<Map.Entry<CompositionData, RelativeLocation>> sequencedParticlesData = new ArrayList<>();
    private UUID[] indexToUuid = new UUID[0];

    // ─── Constructors ────────────────────────────────────────────────────

    protected SequencedParticleComposition(Vec3 position, Level world) {
        super(position, world);
        this.animate = new SequencedCompositionAnimationHelper<SequencedParticleComposition>().loadComposition(this);
        this.index = new Memo<>(() -> {
            int page = count / 64;
            if (count % 64 > 0) page++;
            return new long[page];
        });
    }

    protected SequencedParticleComposition(Vec3 position) {
        this(position, null);
    }

    // ─── Property accessors ──────────────────────────────────────────────

    public SequencedCompositionAnimationHelper<SequencedParticleComposition> getAnimate() {
        return animate;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Memo<long[]> getIndex() {
        return index;
    }

    public void setIndex(Memo<long[]> index) {
        this.index = index;
    }

    public int getDisplayedParticleCount() {
        return displayedParticleCount;
    }

    protected void setDisplayedParticleCount(int displayedParticleCount) {
        this.displayedParticleCount = displayedParticleCount;
    }

    public int getServerCurrentIndex() {
        return serverCurrentIndex;
    }

    protected void setServerCurrentIndex(int serverCurrentIndex) {
        this.serverCurrentIndex = serverCurrentIndex;
    }

    protected ArrayList<Map.Entry<CompositionData, RelativeLocation>> getSequencedParticlesData() {
        return sequencedParticlesData;
    }

    // ─── Abstract method ─────────────────────────────────────────────────

    /**
     * Returns the sequenced particle map (ordered by CompositionData).
     */
    public abstract SortedMap<CompositionData, RelativeLocation> getParticleSequenced();

    // ─── Overrides from ParticleComposition ──────────────────────────────

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        return getParticleSequenced();
    }

    @Override
    public final void tick() {
        super.tick();
    }

    @Override
    public void flush() {
        if (!getParticles().isEmpty()) {
            clear(false);
        }
        displayParticles();
    }

    @Override
    public void clear(boolean cancel) {
        super.clear(cancel);
        sequencedParticlesData.clear();
        getParticleRotatedLocations().clear();
    }

    @Override
    public void display() {
        if (getDisplayed()) return;
        setDisplayed(true);
        Level w = getWorld();
        if (w != null) {
            setClient(w.isClientSide());
        }
        flush();
        onDisplay();
    }

    /**
     * Called before the sequenced particles are displayed. Subclasses
     * can override to modify the map before display.
     */
    public void beforeDisplaySequenced(SortedMap<CompositionData, RelativeLocation> map) {
        // Override point
    }

    @Override
    protected void displayParticles() {
        Map<CompositionData, RelativeLocation> locations = getParticles();
        int newCount = locations.size();
        ensureIndexCapacity(newCount);
        count = newCount;
        if (!getClient()) return;

        beforeDisplaySequenced((SortedMap<CompositionData, RelativeLocation>) locations);
        toggleScale(locations);
        Math3DUtil.rotateAsAxis(
                new ArrayList<>(locations.values()), getAxis(), getRoll()
        );
        sequencedParticlesData.clear();
        for (Map.Entry<CompositionData, RelativeLocation> entry : locations.entrySet()) {
            sequencedParticlesData.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        getParticleRotatedLocations().addAll(locations.values());
        if (indexToUuid.length != count) {
            indexToUuid = new UUID[count];
        }
    }

    @Override
    public void update(ParticleComposition other) {
        super.update(other);
        if (!(other instanceof SequencedParticleComposition seq)) return;

        long[] oldIndex = Arrays.copyOf(index.get(), index.get().length);
        int oldCount = count;
        count = seq.count;
        displayedParticleCount = seq.displayedParticleCount;
        serverCurrentIndex = seq.serverCurrentIndex;
        index.setMemoValue(seq.index.get());

        if (!getClient()) return;
        if (oldCount != count || sequencedParticlesData.size() != count) {
            flush();
        }
        applyIndexDiff(oldIndex, index.get());
    }

    // ─── Rotation overrides ──────────────────────────────────────────────

    @Override
    public void rotateToPoint(RelativeLocation to) {
        if (!getClient()) {
            setAxis(to);
            return;
        }
        Math3DUtil.rotatePointsToPoint(getParticleRotatedLocations(), to, getAxis());
        setAxis(to);
        toggleRelative();
    }

    @Override
    public void rotateToWithAngle(RelativeLocation to, double radian) {
        setRoll(getRoll() + radian);
        if (getRoll() >= Math.PI * 2) {
            setRoll(getRoll() - Math.PI * 2);
        } else if (getRoll() <= -Math.PI * 2) {
            setRoll(getRoll() + Math.PI * 2);
        }
        if (!getClient()) {
            setAxis(to);
            return;
        }
        Math3DUtil.rotateAsAxis(getParticleRotatedLocations(), getAxis(), radian);
        Math3DUtil.rotatePointsToPoint(getParticleRotatedLocations(), to, getAxis());
        setAxis(to);
        toggleRelative();
    }

    @Override
    public void rotateAsAxis(double radian) {
        setRoll(getRoll() + radian);
        if (getRoll() >= Math.PI * 2) {
            setRoll(getRoll() - Math.PI * 2);
        } else if (getRoll() <= -Math.PI * 2) {
            setRoll(getRoll() + Math.PI * 2);
        }
        if (!getClient()) return;
        Math3DUtil.rotateAsAxis(getParticleRotatedLocations(), getAxis(), radian);
        toggleRelative();
    }

    // ─── Sequenced add/remove API ────────────────────────────────────────

    /**
     * Displays the particle at the current index and advances.
     */
    public void addSingle() {
        if (count <= 0) return;
        if (serverCurrentIndex < 0 || serverCurrentIndex >= count) return;

        int idx = serverCurrentIndex;
        if (!isParticleDisplayed(idx)) {
            setParticleStatus(idx, true);
            displayedParticleCount++;
            if (getClient()) {
                createWithIndex(idx);
            }
        }
        serverCurrentIndex = Math.min(idx + 1, Math.max(count - 1, 0));
    }

    public void addMultiple(int amount) {
        for (int i = 0; i < amount; i++) {
            addSingle();
        }
    }

    /**
     * Hides the particle at the current index and retreats.
     */
    public void removeSingle() {
        if (count <= 0) return;
        int idx = Math.min(serverCurrentIndex, count - 1);
        if (idx < 0 || idx >= count) return;

        if (isParticleDisplayed(idx)) {
            setParticleStatus(idx, false);
            displayedParticleCount--;
            if (getClient()) {
                removeWithIndex(idx);
            }
        }
        serverCurrentIndex = Math.max(idx - 1, 0);
    }

    public void removeMultiple(int amount) {
        for (int i = 0; i < amount; i++) {
            removeSingle();
        }
    }

    /**
     * Resets all particles to hidden and resets counters.
     */
    public void resetAll() {
        if (getClient() && count > 0) {
            int pages = pagesFor(count);
            long[] bits = index.get();
            int n = Math.min(bits.length, pages);
            for (int page = 0; page < n; page++) {
                long v = bits[page];
                if (v == 0L) continue;
                int base = page << 6;
                while (v != 0L) {
                    int bit = Long.numberOfTrailingZeros(v);
                    int i = base + bit;
                    if (i < count) {
                        removeWithIndex(i);
                    }
                    v &= v - 1L;
                }
            }
        }
        long[] arr = index.get();
        Arrays.fill(arr, 0L);
        displayedParticleCount = 0;
        serverCurrentIndex = 0;
    }

    // ─── Bit-set status management ───────────────────────────────────────

    public void setParticleStatus(int index, boolean generated) {
        if (index < 0 || index >= count) return;
        setBit(this.index.get(), index, generated);
    }

    public boolean isParticleDisplayed(int index) {
        if (index < 0 || index > count) return false;
        return getBit(this.index.get(), index);
    }

    // ─── Index-based create/remove (client-side) ─────────────────────────

    private void createWithIndex(int i) {
        if (!getClient()) return;
        if (i < 0 || i >= sequencedParticlesData.size()) return;
        if (indexToUuid.length != count) {
            indexToUuid = new UUID[count];
        }
        if (indexToUuid[i] != null) return;

        Map.Entry<CompositionData, RelativeLocation> entry = sequencedParticlesData.get(i);
        CompositionData data = entry.getKey();
        RelativeLocation rl = entry.getValue();
        displayEntry(data, rl);
        indexToUuid[i] = data.getUuid();
    }

    private void removeWithIndex(int i) {
        if (!getClient()) return;
        if (i < 0 || i >= count) return;
        if (indexToUuid.length != count) return;
        UUID uuid = indexToUuid[i];
        if (uuid == null) return;

        if (i < sequencedParticlesData.size()) {
            CompositionData data = sequencedParticlesData.get(i).getKey();
            com.reiasu.reiparticlesapi.particles.Controllable<?> ctrl = data.getControllable();
            if (ctrl != null) {
                ctrl.remove();
                data.setControllable(null);
            }
        }
        indexToUuid[i] = null;
    }

    // ─── Index diff application ──────────────────────────────────────────

    private void applyIndexDiff(long[] oldBits, long[] newBits) {
        if (!getClient()) return;
        if (count <= 0) return;
        if (sequencedParticlesData.size() != count) return;

        int pages = pagesFor(count);
        long[] oldSafe = oldBits.length == pages ? oldBits : Arrays.copyOf(oldBits, pages);
        long[] newSafe = newBits.length == pages ? newBits : Arrays.copyOf(newBits, pages);

        for (int page = 0; page < pages; page++) {
            long diff = oldSafe[page] ^ newSafe[page];
            if (diff == 0L) continue;
            int base = page << 6;
            while (diff != 0L) {
                int bit = Long.numberOfTrailingZeros(diff);
                int i = base + bit;
                if (i >= count) break;
                boolean newGen = ((newSafe[page] >>> bit) & 1L) != 0L;
                if (newGen) {
                    createWithIndex(i);
                } else {
                    removeWithIndex(i);
                }
                diff &= diff - 1L;
            }
        }
    }

    // ─── Internal helpers ────────────────────────────────────────────────

    private void ensureIndexCapacity(int newCount) {
        int pages = pagesFor(newCount);
        long[] current = index.get();
        if (current.length == pages) return;
        long[] resized = new long[pages];
        int len = Math.min(current.length, resized.length);
        System.arraycopy(current, 0, resized, 0, len);
        index.setMemoValue(resized);
    }

    private int pagesFor(int cnt) {
        if (cnt <= 0) return 0;
        return (cnt + 63) / 64;
    }

    private boolean getBit(long[] arr, int index) {
        if (arr.length == 0) return false;
        int page = index >>> 6;
        if (page < 0 || page >= arr.length) return false;
        int bit = index & 0x3F;
        return ((arr[page] >>> bit) & 1L) == 1L;
    }

    private void setBit(long[] arr, int index, boolean value) {
        if (arr.length == 0) return;
        int page = index >>> 6;
        if (page < 0 || page >= arr.length) return;
        int bit = index & 0x3F;
        long mask = 1L << bit;
        arr[page] = value ? (arr[page] | mask) : (arr[page] & ~mask);
    }
}
