// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.control.group;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.MathDataUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A particle group that supports sequenced (ordered) display of particles.
 * <p>
 * Particles can be added/removed one by one or in batches, with bit-packed
 * status arrays tracking which particles are currently displayed.
 *
 * @deprecated Use ParticleGroupStyle instead.
 */
@Deprecated
public abstract class SequencedParticleGroup extends ControllableParticleGroup {

    private static final Logger LOGGER = LogUtils.getLogger();
    private long[] displayedStatus;
    private int particleDisplayedCount;
    private int particleLinkageDisplayCurrentIndex;
    private final ArrayList<Pair<SequencedParticleRelativeData, RelativeLocation>> sequencedParticles = new ArrayList<>();

    public SequencedParticleGroup(UUID uuid) {
        super(uuid);
    }

    // ---- Lazy init for displayedStatus ----

    public long[] getDisplayedStatus() {
        if (displayedStatus == null) {
            displayedStatus = new long[Math.max(1, sequencedParticles.size())];
        }
        return displayedStatus;
    }

    public int getParticleDisplayedCount() { return particleDisplayedCount; }

    public int getParticleLinkageDisplayCurrentIndex() { return particleLinkageDisplayCurrentIndex; }

    protected ArrayList<Pair<SequencedParticleRelativeData, RelativeLocation>> getSequencedParticles() {
        return sequencedParticles;
    }

    // ---- Override parent methods to delegate to sequenced versions ----

    @Override
    public final Map<ParticleRelativeData, RelativeLocation> loadParticleLocations() {
        return Map.of();
    }

    @Override
    public final void beforeDisplay(Map<ParticleRelativeData, RelativeLocation> locations) {
        // No-op, sequenced uses SortedMap overload
    }

    public void beforeDisplay(SortedMap<SequencedParticleRelativeData, RelativeLocation> locations) {
        // Default no-op
    }

    /**
     * Return the ordered particle layout.
     */
    public abstract SortedMap<SequencedParticleRelativeData, RelativeLocation> loadParticleLocationsWithIndex();

    // ---- Display lifecycle ----

    @Override
    public void flush() {
        if (getCanceled() || !getValid() || !getDisplayed()) {
            return;
        }
        remove();
        setValid(true);
        setAxis(new RelativeLocation(0.0, 1.0, 0.0));
        particleDisplayedCount = 0;
        displayParticles();
    }

    @Override
    public void display(Vec3 pos, ClientLevel world) {
        if (getDisplayed()) return;
        setDisplayed(true);
        setOrigin(pos);
        setWorld(world);
        displayParticles();
        onGroupDisplay();
    }

    private void displayParticles() {
        SortedMap<SequencedParticleRelativeData, RelativeLocation> locations = loadParticleLocationsWithIndex();
        for (Map.Entry<SequencedParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
            sequencedParticles.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        beforeDisplay(locations);
        toggleScale(locations);
    }

    // ---- Factory helper ----

    public SequencedParticleRelativeData withEffect(
            Function<UUID, ParticleDisplayer> effect,
            Consumer<ControllableParticle> invoker,
            int order
    ) {
        return new SequencedParticleRelativeData(effect, invoker, order);
    }

    // ---- Status management ----

    public void setSingleStatus(int index, boolean status) {
        int page = MathDataUtil.getStoragePageLong(index);
        int bit = MathDataUtil.getStorageWithBitLong(index);
        long container = getDisplayedStatus()[page];
        boolean currentStatus = MathDataUtil.getStatusLong(container, bit) == 1;
        if (currentStatus == status) return;
        toggleFromStatus(index, status);
        getDisplayedStatus()[page] = MathDataUtil.setStatusLong(container, bit, status);
    }

    public boolean addSingle() {
        if (particleLinkageDisplayCurrentIndex >= sequencedParticles.size() || sequencedParticles.isEmpty()) {
            return false;
        }
        particleDisplayedCount++;
        int idx = particleLinkageDisplayCurrentIndex++;
        return createWithIndex(idx);
    }

    public boolean addMultiple(int count) {
        if (particleLinkageDisplayCurrentIndex >= sequencedParticles.size() || sequencedParticles.isEmpty()) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            if (!addSingle()) return true;
        }
        return true;
    }

    public boolean addAll() {
        return addMultiple(sequencedParticles.size() - particleLinkageDisplayCurrentIndex);
    }

    public boolean removeSingle() {
        if (particleLinkageDisplayCurrentIndex <= 0 || sequencedParticles.isEmpty()) {
            return false;
        }
        particleDisplayedCount--;
        int idx = --particleLinkageDisplayCurrentIndex;
        Pair<SequencedParticleRelativeData, RelativeLocation> pair = sequencedParticles.get(idx);
        UUID currentUUID = pair.first.getUuid();
        Controllable<?> controllable = getParticles().get(currentUUID);
        if (controllable == null) return true;
        getParticlesLocations().remove(controllable);
        controllable.remove();
        return true;
    }

    public boolean removeMultiple(int count) {
        if (particleLinkageDisplayCurrentIndex <= 0 || sequencedParticles.isEmpty()) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            if (!removeSingle()) return true;
        }
        return true;
    }

    public boolean removeAll() {
        return removeMultiple(particleDisplayedCount);
    }

    public void toggle(int count) {
        if (particleLinkageDisplayCurrentIndex == count) return;
        if (particleLinkageDisplayCurrentIndex == 0) {
            addMultiple(count);
            return;
        }
        if (count > particleLinkageDisplayCurrentIndex) {
            removeMultiple(count - particleLinkageDisplayCurrentIndex);
        } else {
            removeMultiple(particleLinkageDisplayCurrentIndex - count);
        }
    }

    public void toggleStatus(long[] statusArray) {
        if (statusArray.length == 0) return;
        if (getDisplayedStatus().length == 0) return;
        if (!getDisplayed()) return;

        for (int page = 0; page < statusArray.length; page++) {
            long container = statusArray[page];
            getDisplayedStatus()[page] = container;
            for (int bit = 1; bit < 65; bit++) {
                int index = page * 64 + bit - 1;
                if (index >= sequencedParticles.size()) break;
                int status = MathDataUtil.getStatusLong(container, bit);
                toggleFromStatus(index, status == 1);
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
        sequencedParticles.clear();
    }

    @Override
    public void scale(double d) {
        if (d < 0.0) {
            LOGGER.error("scale can not be less than zero");
            return;
        }
        setScale(d);
        if (getDisplayed()) {
            toggleScaleDisplayed();
        }
    }

    @Override
    protected final void toggleScaleDisplayed() {
        if (getScale() == 1.0) return;
        for (Pair<SequencedParticleRelativeData, RelativeLocation> pair : sequencedParticles) {
            UUID puuid = pair.first.getUuid();
            Double defLen = getParticlesDefaultScaleLengths().get(puuid);
            if (defLen == null) continue;
            RelativeLocation rl = pair.second;
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * getScale() / currentLen);
            }
        }
    }

    // ---- Rotation overrides (operate on sequencedParticles) ----

    @Override
    public void rotateToPoint(RelativeLocation to) {
        if (!getDisplayed()) return;
        List<RelativeLocation> locs = extractLocations();
        Math3DUtil.rotatePointsToPoint(locs, to, getAxis());
        teleportParticlesToRelative();
        setAxis(to.normalize());
    }

    @Override
    public void rotateAsAxis(double angle) {
        if (!getDisplayed()) return;
        List<RelativeLocation> locs = extractLocations();
        Math3DUtil.rotateAsAxis(locs, getAxis(), angle);
        teleportParticlesToRelative();
    }

    @Override
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        if (!getDisplayed()) return;
        List<RelativeLocation> locs = extractLocations();
        Math3DUtil.rotatePointsToPoint(locs, to, getAxis());
        List<RelativeLocation> locs2 = extractLocations();
        Math3DUtil.rotateAsAxis(locs2, to.normalize(), angle);
        teleportParticlesToRelative();
        setAxis(to.normalize());
    }

    // ---- Internal helpers ----

    private List<RelativeLocation> extractLocations() {
        List<RelativeLocation> result = new ArrayList<>(sequencedParticles.size());
        for (Pair<SequencedParticleRelativeData, RelativeLocation> pair : sequencedParticles) {
            result.add(pair.second);
        }
        return result;
    }

    private void teleportParticlesToRelative() {
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : getParticlesLocations().entrySet()) {
            Controllable<?> c = entry.getKey();
            RelativeLocation rl = entry.getValue();
            c.teleportTo(rl.getX() + getOrigin().x, rl.getY() + getOrigin().y, rl.getZ() + getOrigin().z);
        }
    }

    private boolean createWithIndex(int index) {
        if (index >= sequencedParticles.size() || sequencedParticles.isEmpty()) {
            return false;
        }
        Pair<SequencedParticleRelativeData, RelativeLocation> pair = sequencedParticles.get(index);
        SequencedParticleRelativeData data = pair.first;
        RelativeLocation rl = pair.second;
        UUID puuid = data.getUuid();

        ParticleDisplayer displayer = data.getEffect().apply(puuid);
        if (displayer instanceof ParticleDisplayer.SingleParticleDisplayer) {
            ParticleController controller = ControlParticleManager.INSTANCE.createControl(puuid);
            controller.setInitInvoker(data.getInvoker());
        }

        Vec3 pos = getOrigin();
        Vec3 toPos = new Vec3(pos.x + rl.getX(), pos.y + rl.getY(), pos.z + rl.getZ());
        ClientLevel w = getWorld();
        if (w == null) return false;
        Controllable<?> controllable = displayer.display(toPos, w);
        if (controllable == null) return true;

        if (controllable instanceof ParticleController) {
            data.getControllerAction().accept((ParticleController) controllable);
        }
        getParticles().put(puuid, controllable);
        getParticlesLocations().put(controllable, rl);
        return true;
    }

    private void toggleFromStatus(int index, boolean status) {
        if (index >= sequencedParticles.size()) return;
        if (status) {
            createWithIndex(index);
        } else {
            UUID puuid = sequencedParticles.get(index).first.getUuid();
            Controllable<?> controllable = getParticles().get(puuid);
            if (controllable == null) return;
            controllable.remove();
            getParticles().remove(puuid);
            getParticlesLocations().remove(controllable);
        }
        int page = MathDataUtil.getStoragePageLong(index);
        long container = getDisplayedStatus()[page];
        int bit = MathDataUtil.getStorageWithBitLong(index);
        MathDataUtil.setStatusLong(container, bit, status);
    }

    private void toggleScale(SortedMap<SequencedParticleRelativeData, RelativeLocation> locations) {
        if (getParticlesDefaultScaleLengths().isEmpty()) {
            for (Map.Entry<SequencedParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
                getParticlesDefaultScaleLengths().put(entry.getKey().getUuid(), entry.getValue().length());
            }
        }
        if (getScale() == 1.0) return;
        for (Map.Entry<SequencedParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
            UUID puuid = entry.getKey().getUuid();
            Double defLen = getParticlesDefaultScaleLengths().get(puuid);
            if (defLen == null) continue;
            RelativeLocation rl = entry.getValue();
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * getScale() / currentLen);
            }
        }
    }

    // ---- Inner classes ----

    /**
     * Extension of {@link ParticleRelativeData} with an ordering index for sequenced display.
     */
    public static class SequencedParticleRelativeData
            extends ControllableParticleGroup.ParticleRelativeData
            implements Comparable<SequencedParticleRelativeData> {

        private final int order;

        public SequencedParticleRelativeData(
                Function<UUID, ParticleDisplayer> effect,
                Consumer<ControllableParticle> invoker,
                int order
        ) {
            super(effect, invoker);
            this.order = order;
        }

        public int getOrder() { return order; }

        @Override
        public int compareTo(SequencedParticleRelativeData other) {
            return this.order - other.order;
        }

        @Override
        public boolean equals(Object other) {
            return other == this;
        }

        @Override
        public int hashCode() {
            return order;
        }
    }

    /**
     * Simple generic pair utility (replaces Kotlin's Pair).
     */
    public static class Pair<A, B> {
        public final A first;
        public final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
    }
}
