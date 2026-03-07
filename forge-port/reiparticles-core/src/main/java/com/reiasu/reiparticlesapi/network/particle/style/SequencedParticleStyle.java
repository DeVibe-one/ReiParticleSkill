// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.particles.impl.ControllableEndRodEffect;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A particle style where particles are displayed sequentially (one-by-one)
 * and can be toggled on/off by index. Supports ordered particle display.
 */
public abstract class SequencedParticleStyle extends ParticleGroupStyle {

    public static final int CREATE_PARTICLE = 1;
    public static final int DELETE_PARTICLE = 0;

    private long[] displayedStatus;
    private ArrayList<Map.Entry<SortedStyleData, RelativeLocation>> sequencedParticles = new ArrayList<>();
    private int displayedParticleCount;
    private int particleLinkageDisplayCurrentIndex;

    public SequencedParticleStyle(double visibleRange, UUID uuid) {
        super(visibleRange, uuid);
    }

    public SequencedParticleStyle() {
        this(32.0, UUID.randomUUID());
    }

    // ---- Abstract methods ----

    public abstract int getParticlesCount();
    public abstract SortedMap<SortedStyleData, RelativeLocation> getCurrentFramesSequenced();
    public abstract Map<String, ParticleControllerDataBuffer<?>> writePacketArgsSequenced();
    public abstract void readPacketArgsSequenced(Map<String, ? extends ParticleControllerDataBuffer<?>> args);

    // ---- Getters / Setters ----

    public long[] getDisplayedStatus() {
        if (displayedStatus == null) {
            displayedStatus = new long[(getParticlesCount() + 63) / 64];
        }
        return displayedStatus;
    }

    protected ArrayList<Map.Entry<SortedStyleData, RelativeLocation>> getSequencedParticles() {
        return sequencedParticles;
    }

    public int getDisplayedParticleCount() { return displayedParticleCount; }

    public void setDisplayedParticleCount(int count) {
        this.displayedParticleCount = Math.max(count, 0);
    }

    public int getParticleLinkageDisplayCurrentIndex() { return particleLinkageDisplayCurrentIndex; }

    // ---- Status management ----

    public boolean getStatus(int index) {
        if (index < 0 || index >= getParticlesCount()) return false;
        int word = index / 64;
        int bit = index % 64;
        return (getDisplayedStatus()[word] & (1L << bit)) != 0;
    }

    public void setStatus(int index, boolean status) {
        if (index < 0 || index >= getParticlesCount()) return;
        int word = index / 64;
        int bit = index % 64;
        if (status) {
            getDisplayedStatus()[word] |= (1L << bit);
        } else {
            getDisplayedStatus()[word] &= ~(1L << bit);
        }
    }

    public void clearStatus() {
        Arrays.fill(getDisplayedStatus(), 0L);
        displayedParticleCount = 0;
        particleLinkageDisplayCurrentIndex = 0;
    }

    // ---- Sequenced add/remove ----

    public void addSingle() {
        if (getClient()) {
            if (particleLinkageDisplayCurrentIndex >= getParticlesCount()) return;
            int idx = particleLinkageDisplayCurrentIndex++;
            toggleFromStatus(idx, true);
            return;
        }
        displayedParticleCount++;
        if (getStatus(particleLinkageDisplayCurrentIndex)) return;
        setStatus(particleLinkageDisplayCurrentIndex, true);
        int idx = particleLinkageDisplayCurrentIndex++;

        Map<String, ParticleControllerDataBuffer<?>> args = buildChangeSingleStatusArgs(idx, true);
        change(g -> {}, args);
    }

    public void addMultiple(int count) {
        if (getClient()) {
            for (int i = 0; i < count; i++) addSingle();
            return;
        }
        if (count <= 0) return;
        if (count == 1) { addSingle(); return; }

        displayedParticleCount += count;
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            int idx = particleLinkageDisplayCurrentIndex + i;
            setStatus(idx, true);
            indices[i] = idx;
        }
        Map<String, ParticleControllerDataBuffer<?>> args = buildChangeMultipleStatusArgs(indices, true);
        int startIdx = particleLinkageDisplayCurrentIndex;
        particleLinkageDisplayCurrentIndex += count;
        change(g -> {}, args);
    }

    public void removeSingle() {
        if (getClient()) {
            if (particleLinkageDisplayCurrentIndex <= 0) return;
            int idx = --particleLinkageDisplayCurrentIndex;
            toggleFromStatus(idx, false);
            return;
        }
        displayedParticleCount = Math.max(displayedParticleCount - 1, 0);
        if (particleLinkageDisplayCurrentIndex <= 0) return;
        int idx = --particleLinkageDisplayCurrentIndex;
        setStatus(idx, false);

        Map<String, ParticleControllerDataBuffer<?>> args = buildChangeSingleStatusArgs(idx, false);
        change(g -> {}, args);
    }

    public void removeMultiple(int count) {
        if (getClient()) {
            for (int i = 0; i < count; i++) removeSingle();
            return;
        }
        if (count <= 0) return;
        if (count == 1) { removeSingle(); return; }

        displayedParticleCount = Math.max(displayedParticleCount - count, 0);
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            if (particleLinkageDisplayCurrentIndex <= 0) break;
            int idx = --particleLinkageDisplayCurrentIndex;
            setStatus(idx, false);
            indices[i] = idx;
        }
        Map<String, ParticleControllerDataBuffer<?>> args = buildChangeMultipleStatusArgs(indices, false);
        change(g -> {}, args);
    }

    // ---- Toggle from status ----

    protected void toggleFromStatus(int index, boolean status) {
        if (index < 0 || index >= sequencedParticles.size()) return;
        Map.Entry<SortedStyleData, RelativeLocation> entry = sequencedParticles.get(index);
        SortedStyleData data = entry.getKey();
        RelativeLocation rl = entry.getValue();

        if (status) {
            // Create particle
            createWithIndex(data, rl);
        } else {
            // Remove particle
            Controllable<?> particle = getParticles().get(data.getUuid());
            if (particle != null) {
                particle.remove();
                getParticles().remove(data.getUuid());
                getParticleLocations().remove(particle);
            }
        }
        setStatus(index, status);
    }

    protected void toggleDataStatus(int[] indices, boolean status) {
        for (int index : indices) {
            toggleFromStatus(index, status);
        }
    }

    private void createWithIndex(SortedStyleData data, RelativeLocation rl) {
        UUID particleUUID = data.getUuid();
        ParticleDisplayer displayer = data.getDisplayerBuilder().apply(particleUUID);

        if (displayer instanceof ParticleDisplayer.SingleParticleDisplayer) {
            ParticleController controller = ControlParticleManager.INSTANCE.createControl(particleUUID);
            controller.setInitInvoker(data.getParticleHandler());
        }

        Vec3 toPos = new Vec3(getPos().x + rl.getX(), getPos().y + rl.getY(), getPos().z + rl.getZ());
        Controllable<?> controllable = displayer.display(toPos, (ClientLevel) getWorld());
        if (controllable == null) return;

        if (controllable instanceof ParticleController) {
            data.getParticleControllerHandler().accept((ParticleController) controllable);
        }
        getParticles().put(particleUUID, controllable);
        getParticleLocations().put(controllable, rl);
    }

    // ---- Packet args helpers ----

    private Map<String, ParticleControllerDataBuffer<?>> buildChangeSingleStatusArgs(int index, boolean status) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("seq_index", ParticleControllerDataBuffers.INSTANCE.intValue(index));
        args.put("seq_status", ParticleControllerDataBuffers.INSTANCE.bool(status));
        args.put("seq_count", ParticleControllerDataBuffers.INSTANCE.intValue(displayedParticleCount));
        return args;
    }

    private Map<String, ParticleControllerDataBuffer<?>> buildChangeMultipleStatusArgs(int[] indices, boolean status) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("seq_indices", ParticleControllerDataBuffers.INSTANCE.intArray(indices));
        args.put("seq_status", ParticleControllerDataBuffers.INSTANCE.bool(status));
        args.put("seq_count", ParticleControllerDataBuffers.INSTANCE.intValue(displayedParticleCount));
        return args;
    }

    // ---- Overrides ----

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        return Collections.emptyMap();
    }

    @Override
    protected void displayParticles() {
        SortedMap<SortedStyleData, RelativeLocation> locations = getCurrentFramesSequenced();
        sequencedParticles.clear();
        List<RelativeLocation> locs = new ArrayList<>(locations.values());
        Math3DUtil.rotateAsAxis(locs, getAxis(), getRotate());
        toggleScale(locations);

        for (Map.Entry<SortedStyleData, RelativeLocation> entry : locations.entrySet()) {
            sequencedParticles.add(entry);
        }
    }

    @Override
    public void display(Vec3 pos, Level world) {
        super.display(pos, world);
    }

    @Override
    public void flush() {
        if (!getParticles().isEmpty()) {
            clear(true);
        }
        displayParticles();
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgs() {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("seq_displayed_status", ParticleControllerDataBuffers.INSTANCE.longArray(getDisplayedStatus().clone()));
        args.put("seq_displayed_count", ParticleControllerDataBuffers.INSTANCE.intValue(displayedParticleCount));
        args.put("seq_current_index", ParticleControllerDataBuffers.INSTANCE.intValue(particleLinkageDisplayCurrentIndex));
        args.putAll(writePacketArgsSequenced());
        return args;
    }

    @Override
    public void readPacketArgs(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        ParticleControllerDataBuffer<?> statusBuf = args.get("seq_displayed_status");
        if (statusBuf != null) {
            displayedStatus = (long[]) statusBuf.getLoadedValue();
        }
        ParticleControllerDataBuffer<?> countBuf = args.get("seq_displayed_count");
        if (countBuf != null) {
            displayedParticleCount = (int) countBuf.getLoadedValue();
        }
        ParticleControllerDataBuffer<?> indexBuf = args.get("seq_current_index");
        if (indexBuf != null) {
            particleLinkageDisplayCurrentIndex = (int) indexBuf.getLoadedValue();
        }
        readPacketArgsSequenced(args);
    }

    // ---- Inner classes ----

    /**
     * Style data with an order index for sequenced display.
     */
    public static class SortedStyleData extends StyleData implements Comparable<SortedStyleData> {
        private final int order;

        public SortedStyleData(Function<UUID, ParticleDisplayer> displayerBuilder, int order) {
            super(displayerBuilder);
            this.order = order;
        }

        public int getOrder() { return order; }

        @Override
        public int compareTo(SortedStyleData other) {
            return this.order - other.order;
        }
    }

    /**
     * Builder for creating {@link SortedStyleData} instances with chained configuration.
     */
    public static final class SortedStyleDataBuilder {
        private Function<UUID, ParticleDisplayer> displayerBuilder =
                uuid -> ParticleDisplayer.Companion.withSingle(new ControllableEndRodEffect(uuid, false));
        private final List<Consumer<ControllableParticle>> particleHandlers = new ArrayList<>();
        private final List<Consumer<ParticleController>> particleControllerHandlers = new ArrayList<>();

        public SortedStyleDataBuilder addParticleHandler(Consumer<ControllableParticle> builder) {
            particleHandlers.add(builder);
            return this;
        }

        public SortedStyleDataBuilder addParticleControllerHandler(Consumer<ParticleController> builder) {
            particleControllerHandlers.add(builder);
            return this;
        }

        public SortedStyleDataBuilder clearParticleHandlers() {
            particleHandlers.clear();
            return this;
        }

        public SortedStyleDataBuilder clearParticleControllers() {
            particleControllerHandlers.clear();
            return this;
        }

        public SortedStyleDataBuilder removeHandler(int index) {
            if (index >= 0 && index < particleHandlers.size()) {
                particleHandlers.remove(index);
            }
            return this;
        }

        public SortedStyleDataBuilder removeParticleController(int index) {
            if (index >= 0 && index < particleControllerHandlers.size()) {
                particleControllerHandlers.remove(index);
            }
            return this;
        }

        public SortedStyleDataBuilder displayer(Function<UUID, ParticleDisplayer> builder) {
            this.displayerBuilder = builder;
            return this;
        }

        public SortedStyleData build(int order) {
            SortedStyleData data = new SortedStyleData(displayerBuilder, order);
            data.withParticleHandler(p -> {
                for (Consumer<ControllableParticle> handler : particleHandlers) {
                    handler.accept(p);
                }
            });
            data.withParticleControllerHandler(c -> {
                for (Consumer<ParticleController> handler : particleControllerHandlers) {
                    handler.accept(c);
                }
            });
            return data;
        }
    }
}
