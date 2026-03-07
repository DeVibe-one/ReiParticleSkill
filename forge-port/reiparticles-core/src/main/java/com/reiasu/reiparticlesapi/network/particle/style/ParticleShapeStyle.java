// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

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

/**
 * A style that builds its particle layout from {@link PointsBuilder} shapes.
 * Supports scale helpers and display/before-display callbacks.
 */
public class ParticleShapeStyle extends ParticleGroupStyle {

    private ScaleHelper scaleHelper;
    private final List<Consumer<ParticleShapeStyle>> displayInvokes = new ArrayList<>();
    private final List<BiConsumer<ParticleShapeStyle, Map<StyleData, RelativeLocation>>> beforeDisplayInvokes = new ArrayList<>();
    private final LinkedHashMap<PointsBuilder, Function<RelativeLocation, StyleData>> pointBuilders = new LinkedHashMap<>();
    private int spawnAge;
    private boolean scalePreTick;
    private boolean scaleReversed;

    public ParticleShapeStyle(UUID uuid) {
        super(64.0, uuid);
    }

    public ParticleShapeStyle() {
        this(UUID.randomUUID());
    }

    // ---- Getters / Setters ----

    public ScaleHelper getScaleHelper() { return scaleHelper; }
    public void setScaleHelper(ScaleHelper scaleHelper) { this.scaleHelper = scaleHelper; }
    public int getSpawnAge() { return spawnAge; }
    public void setSpawnAge(int spawnAge) { this.spawnAge = spawnAge; }
    public boolean getScalePreTick() { return scalePreTick; }
    public boolean getScaleReversed() { return scaleReversed; }

    // ---- Builder methods ----

    public ParticleShapeStyle appendBuilder(PointsBuilder pointsBuilder,
                                            Function<RelativeLocation, StyleData> dataBuilder) {
        this.pointBuilders.put(pointsBuilder, dataBuilder);
        return this;
    }

    public ParticleShapeStyle appendPoint(RelativeLocation point,
                                          Function<RelativeLocation, StyleData> dataBuilder) {
        PointsBuilder pb = new PointsBuilder();
        pb.addPoint(point);
        this.pointBuilders.put(pb, dataBuilder);
        return this;
    }

    public ParticleShapeStyle scaleReversed(boolean max) {
        if (scaleHelper == null) return this;
        this.scaleReversed = true;
        if (max) {
            scaleHelper.toggleScale(scaleHelper.getMaxScale());
        }
        return this;
    }

    public ParticleShapeStyle loadScaleHelper(double minScale, double maxScale, int scaleTick) {
        this.scaleHelper = new StyleScaleHelper(minScale, maxScale, scaleTick);
        this.scalePreTick = true;
        scaleHelper.loadController(this);
        return this;
    }

    public ParticleShapeStyle loadScaleHelperBezierValue(double minScale, double maxScale,
                                                          int scaleTick,
                                                          RelativeLocation c1, RelativeLocation c2) {
        this.scaleHelper = new StyleBezierValueScaleHelper(scaleTick, minScale, maxScale, c1, c2);
        this.scalePreTick = true;
        scaleHelper.loadController(this);
        return this;
    }

    public ParticleShapeStyle toggleOnDisplay(Consumer<ParticleShapeStyle> toggleMethod) {
        this.displayInvokes.add(toggleMethod);
        return this;
    }

    public ParticleShapeStyle toggleBeforeDisplay(
            BiConsumer<ParticleShapeStyle, Map<StyleData, RelativeLocation>> toggleMethod) {
        this.beforeDisplayInvokes.add(toggleMethod);
        return this;
    }

    // ---- Overrides ----

    @Override
    public void beforeDisplay(Map<StyleData, RelativeLocation> styles) {
        for (BiConsumer<ParticleShapeStyle, Map<StyleData, RelativeLocation>> invoke : beforeDisplayInvokes) {
            invoke.accept(this, styles);
        }
    }

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        Map<StyleData, RelativeLocation> result = new LinkedHashMap<>();
        for (Map.Entry<PointsBuilder, Function<RelativeLocation, StyleData>> entry : pointBuilders.entrySet()) {
            List<RelativeLocation> points = entry.getKey().create();
            Function<RelativeLocation, StyleData> dataBuilder = entry.getValue();
            for (RelativeLocation point : points) {
                StyleData data = dataBuilder.apply(point);
                result.put(data, point);
            }
        }
        return result;
    }

    @Override
    public void onDisplay() {
        for (Consumer<ParticleShapeStyle> invoke : displayInvokes) {
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
        });
    }

    // ---- Fast style data factory methods ----

    public StyleData fastStyleData(Vec3 color, Function<UUID, ParticleDisplayer> displayer) {
        return new StyleData(displayer).withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
        });
    }

    public StyleData fastStyleData(Vec3 color, Object sheet, Function<UUID, ParticleDisplayer> displayer) {
        return new StyleData(displayer).withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
        });
    }

    public StyleData fastStyleData(Vec3 color, Object sheet, float size,
                                    Function<UUID, ParticleDisplayer> displayer) {
        return new StyleData(displayer).withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
            p.setSize(size);
        });
    }

    public StyleData fastStyleData(Vec3 color, Object sheet, float size, float alpha,
                                    Function<UUID, ParticleDisplayer> displayer) {
        return new StyleData(displayer).withParticleHandler(p -> {
            p.colorOfRGB((int) color.x, (int) color.y, (int) color.z);
            p.setSize(size);
            p.setParticleAlpha(alpha);
        });
    }

    public StyleData fastStyleData(Object sheet, Function<UUID, ParticleDisplayer> displayer) {
        return new StyleData(displayer);
    }
}
