// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.preview.display;

import com.reiasu.reiparticlesapi.network.particle.composition.AutoParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.FourierSeriesBuilder;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChangingComposition extends AutoParticleComposition {
    private static final DustParticleOptions COLOR_A =
            new DustParticleOptions(new Vector3f(0.95f, 0.82f, 0.35f), 0.95f);
    private static final DustParticleOptions COLOR_B =
            new DustParticleOptions(new Vector3f(0.62f, 0.9f, 1.0f), 0.85f);
    private static final List<List<RelativeLocation>> SHAPES = createShapes();

    private int index;
    private int lifeTick;

    public ChangingComposition(Vec3 position, Level world) {
        setPosition(position == null ? Vec3.ZERO : position);
        setWorld(world);
        setVisibleRange(196.0);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.floorMod(index, SHAPES.size());
    }

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        Map<CompositionData, RelativeLocation> result = new LinkedHashMap<>();
        result.put(new CompositionData(), new RelativeLocation());
        return result;
    }

    @Override
    public void onDisplay() {
        addPreTickAction(composition -> composition.rotateAsAxis(0.04908738521234052));
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }

        lifeTick++;
        if (lifeTick % 12 == 0) {
            index = (index + 1) % SHAPES.size();
        }
        if (lifeTick > 130) {
            remove();
            return;
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getPosition();
        List<RelativeLocation> shape = SHAPES.get(index);
        double angle = getRoll();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double pulsate = 1.0 + Math.sin(lifeTick * 0.15) * 0.12;
        int step = lifeTick < 40 ? 2 : 3;

        for (int i = 0; i < shape.size(); i += step) {
            RelativeLocation point = shape.get(i);
            double px = point.getX() * pulsate;
            double pz = point.getZ() * pulsate;
            double rx = px * cos - pz * sin;
            double rz = px * sin + pz * cos;
            double x = center.x + rx;
            double y = center.y + point.getY() * 0.15 + 0.08;
            double z = center.z + rz;

            ParticleOptions particle = (i & 1) == 0 ? COLOR_A : COLOR_B;
            level.sendParticles(particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        if ((lifeTick & 5) == 0) {
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.12, center.z, 8, 0.3, 0.08, 0.3, 0.0);
        }
        if ((lifeTick & 7) == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.09, center.z, 12, 0.35, 0.05, 0.35, 0.0);
        }
    }

    private static List<List<RelativeLocation>> createShapes() {
        List<List<RelativeLocation>> shapes = new ArrayList<>();

        shapes.add(new PointsBuilder()
                .addCircle(2.6, 180)
                .addPolygonInCircle(6, 26, 2.0)
                .create());

        shapes.add(new PointsBuilder()
                .addFourierSeries(
                        new FourierSeriesBuilder()
                                .count(300)
                                .scale(0.9)
                                .addFourier(2.0, 1.0, 0.0)
                                .addFourier(0.45, 4.0, 0.0)
                )
                .addPolygonInCircle(4, 40, 2.2)
                .rotateAsAxis(Math.PI / 4.0)
                .create());

        shapes.add(new PointsBuilder()
                .addLine(new RelativeLocation(-2.2, 0.0, 0.0), new RelativeLocation(2.2, 0.0, 0.0), 96)
                .addLine(new RelativeLocation(0.0, 0.0, -2.2), new RelativeLocation(0.0, 0.0, 2.2), 96)
                .addCircle(1.6, 120)
                .create());

        shapes.add(new PointsBuilder()
                .addPolygonInCircle(3, 58, 2.7)
                .withBuilder(new PointsBuilder().addPolygonInCircle(3, 58, 2.7).rotateAsAxis(Math.PI / 3.0))
                .addCircle(0.9, 90)
                .create());

        shapes.add(new PointsBuilder()
                .addFourierSeries(
                        new FourierSeriesBuilder()
                                .count(320)
                                .scale(0.8)
                                .addFourier(1.6, 1.0, 0.0)
                                .addFourier(0.8, 2.0, 0.0)
                                .addFourier(0.25, 6.0, 0.0)
                )
                .addCircle(2.9, 180)
                .create());

        return shapes;
    }
}
