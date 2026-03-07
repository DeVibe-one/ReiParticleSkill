// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticlesapi.utils.helper.ScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleBezierValueScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleStatusHelper;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A sequenced style that builds its particle layout from {@link PointsBuilder} shapes.
 * Combines the shape-building approach of {@link ParticleShapeStyle} with the
 * sequential display approach of {@link SequencedParticleStyle}.
 */
public final class SequencedParticleShapeStyle extends SequencedParticleStyle {

    private ScaleHelper scaleHelper;
    private final List<Consumer<SequencedParticleShapeStyle>> displayInvokes = new ArrayList<>();
    private final List<BiConsumer<SequencedParticleShapeStyle, SortedMap<SortedStyleData, RelativeLocation>>> beforeDisplayInvokes = new ArrayList<>();
    private final LinkedHashMap<PointsBuilder, Function<RelativeLocation, SortedStyleData>> pointBuilders = new LinkedHashMap<>();
    private final ArrayList<Map.Entry<Predicate<SequencedParticleShapeStyle>, Integer>> animationConditions = new ArrayList<>();
    private int animationIndex;
    private int spawnAge;
    private boolean scalePreTick;
    private boolean scaleReversed;
    private int count;

    public SequencedParticleShapeStyle(UUID uuid) {
        super(64.0, uuid);
    }

    public SequencedParticleShapeStyle() {
        this(UUID.randomUUID());
    }

    // ---- Getters / Setters ----

    public ScaleHelper getScaleHelper() { return scaleHelper; }
    public void setScaleHelper(ScaleHelper scaleHelper) { this.scaleHelper = scaleHelper; }
    public int getAnimationIndex() { return animationIndex; }
    public int getSpawnAge() { return spawnAge; }
    public void setSpawnAge(int spawnAge) { this.spawnAge = spawnAge; }
    public boolean getScalePreTick() { return scalePreTick; }
    public boolean getScaleReversed() { return scaleReversed; }

    // ---- Builder methods ----

    public SequencedParticleShapeStyle appendBuilder(PointsBuilder pointsBuilder,
                                                      Function<RelativeLocation, SortedStyleData> dataBuilder) {
        this.pointBuilders.put(pointsBuilder, dataBuilder);
        return this;
    }

    public SequencedParticleShapeStyle appendPoint(RelativeLocation point,
                                                    Function<RelativeLocation, SortedStyleData> dataBuilder) {
        PointsBuilder pb = new PointsBuilder();
        pb.addPoint(point);
        this.pointBuilders.put(pb, dataBuilder);
        return this;
    }

    public SequencedParticleShapeStyle appendAnimateCondition(Predicate<SequencedParticleShapeStyle> predicate, int add) {
        animationConditions.add(new AbstractMap.SimpleEntry<>(predicate, add));
        return this;
    }

    public SequencedParticleShapeStyle scaleReversed(boolean max) {
        if (scaleHelper == null) return this;
        this.scaleReversed = true;
        if (max) {
            scaleHelper.toggleScale(scaleHelper.getMaxScale());
        }
        return this;
    }

    public SequencedParticleShapeStyle loadScaleHelper(double minScale, double maxScale, int scaleTick) {
        this.scaleHelper = new StyleScaleHelper(minScale, maxScale, scaleTick);
        this.scalePreTick = true;
        scaleHelper.loadController(this);
        return this;
    }

    public SequencedParticleShapeStyle loadScaleHelperBezierValue(double minScale, double maxScale,
                                                                    int scaleTick,
                                                                    RelativeLocation c1, RelativeLocation c2) {
        this.scaleHelper = new StyleBezierValueScaleHelper(scaleTick, minScale, maxScale, c1, c2);
        this.scalePreTick = true;
        scaleHelper.loadController(this);
        return this;
    }

    public SequencedParticleShapeStyle toggleOnDisplay(Consumer<SequencedParticleShapeStyle> toggleMethod) {
        this.displayInvokes.add(toggleMethod);
        return this;
    }

    public SequencedParticleShapeStyle toggleBeforeDisplay(
            BiConsumer<SequencedParticleShapeStyle, SortedMap<SortedStyleData, RelativeLocation>> toggleMethod) {
        this.beforeDisplayInvokes.add(toggleMethod);
        return this;
    }

    // ---- Overrides ----

    public void beforeDisplay(SortedMap<SortedStyleData, RelativeLocation> styles) {
        for (BiConsumer<SequencedParticleShapeStyle, SortedMap<SortedStyleData, RelativeLocation>> invoke : beforeDisplayInvokes) {
            invoke.accept(this, styles);
        }
    }

    @Override
    public void onDisplay() {
        for (Consumer<SequencedParticleShapeStyle> invoke : displayInvokes) {
            invoke.accept(this);
        }
        addPreTickAction(style -> {
            spawnAge++;
            if (scalePreTick && scaleHelper != null) {
                if (scaleReversed) {
                    scaleHelper.doScaleReversed();
                } else {
                    scaleHelper.doScale();
                }
            }
            // Process animation conditions
            for (Map.Entry<Predicate<SequencedParticleShapeStyle>, Integer> entry : animationConditions) {
                if (entry.getKey().test(this)) {
                    int add = entry.getValue();
                    if (add > 0) {
                        addMultiple(add);
                    } else if (add < 0) {
                        removeMultiple(-add);
                    }
                    break;
                }
            }
        });
    }

    @Override
    public int getParticlesCount() {
        if (count <= 0) {
            count = 0;
            for (Map.Entry<PointsBuilder, Function<RelativeLocation, SortedStyleData>> entry : pointBuilders.entrySet()) {
                count += entry.getKey().create().size();
            }
        }
        return count;
    }

    @Override
    public SortedMap<SortedStyleData, RelativeLocation> getCurrentFramesSequenced() {
        TreeMap<SortedStyleData, RelativeLocation> result = new TreeMap<>();
        int order = 0;
        for (Map.Entry<PointsBuilder, Function<RelativeLocation, SortedStyleData>> entry : pointBuilders.entrySet()) {
            List<RelativeLocation> points = entry.getKey().create();
            Function<RelativeLocation, SortedStyleData> dataBuilder = entry.getValue();
            for (RelativeLocation point : points) {
                SortedStyleData data = dataBuilder.apply(point);
                result.put(data, point);
                order++;
            }
        }
        return result;
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgsSequenced() {
        return new HashMap<>();
    }

    @Override
    public void readPacketArgsSequenced(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        // No additional args by default
    }

    // ---- Fast style data factory methods ----

    public SortedStyleData fastStyleData(int order, Vec3 color, Function<UUID, ParticleDisplayer> displayer) {
        SortedStyleData data = new SortedStyleData(displayer, order);
        data.withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
        });
        return data;
    }

    public SortedStyleData fastStyleData(int order, Vec3 color, Object sheet,
                                          Function<UUID, ParticleDisplayer> displayer) {
        SortedStyleData data = new SortedStyleData(displayer, order);
        data.withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
        });
        return data;
    }

    public SortedStyleData fastStyleData(int order, Vec3 color, Object sheet, float size,
                                          Function<UUID, ParticleDisplayer> displayer) {
        SortedStyleData data = new SortedStyleData(displayer, order);
        data.withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
            p.setSize(size);
        });
        return data;
    }

    public SortedStyleData fastStyleData(int order, Vec3 color, Object sheet, float size, float alpha,
                                          Function<UUID, ParticleDisplayer> displayer) {
        SortedStyleData data = new SortedStyleData(displayer, order);
        data.withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
            p.setSize(size);
            p.setParticleAlpha(alpha);
        });
        return data;
    }

    public SortedStyleData fastStyleData(int order, Object sheet, Function<UUID, ParticleDisplayer> displayer) {
        return new SortedStyleData(displayer, order);
    }
}
