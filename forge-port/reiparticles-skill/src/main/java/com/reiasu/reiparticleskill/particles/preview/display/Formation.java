// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.preview.display;

import com.reiasu.reiparticlesapi.network.particle.composition.AutoParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static sword-shaped formation composition.
 * Renders the sword outline as defined in the Fabric original's Formation class.
 */
public final class Formation extends AutoParticleComposition {
    private static final DustParticleOptions COLOR =
            new DustParticleOptions(new Vector3f(0.95f, 0.82f, 0.35f), 0.7f);

    private static final List<RelativeLocation> SWORD_SHAPE = buildSwordShape();

    private int lifeTick;

    public Formation(Vec3 position, Level world) {
        setPosition(position == null ? Vec3.ZERO : position);
        setWorld(world);
        setVisibleRange(196.0);
    }

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        Map<CompositionData, RelativeLocation> result = new LinkedHashMap<>();
        for (RelativeLocation point : SWORD_SHAPE) {
            result.put(new CompositionData(), point.copy());
        }
        return result;
    }

    @Override
    public void onDisplay() {
        lifeTick = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }

        lifeTick++;
        if (lifeTick > 200) {
            remove();
            return;
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getPosition();
        int step = lifeTick < 30 ? 2 : 3;
        for (int i = 0; i < SWORD_SHAPE.size(); i += step) {
            RelativeLocation p = SWORD_SHAPE.get(i);
            level.sendParticles(COLOR,
                    center.x + p.getX(),
                    center.y + p.getY() + 0.05,
                    center.z + p.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        if (lifeTick % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.x, center.y + 0.08, center.z,
                    6, 0.2, 0.05, 0.2, 0.0);
        }
    }

    private static List<RelativeLocation> buildSwordShape() {
        return new PointsBuilder()
                // Blade tip
                .addLine(new RelativeLocation(0.0, 0.0, 3.0), new RelativeLocation(1.0, 0.0, 2.0), 30)
                .addLine(new RelativeLocation(0.0, 0.0, 3.0), new RelativeLocation(-1.0, 0.0, 2.0), 30)
                // Blade edges
                .addLine(new RelativeLocation(1.0, 0.0, 2.0), new RelativeLocation(1.0, 0.0, -2.0), 30)
                .addLine(new RelativeLocation(-1.0, 0.0, 2.0), new RelativeLocation(-1.0, 0.0, -2.0), 30)
                // Cross-guard
                .addLine(new RelativeLocation(3.0, 0.0, -2.0), new RelativeLocation(-3.0, 0.0, -2.0), 30)
                .addLine(new RelativeLocation(3.0, 0.0, -2.0), new RelativeLocation(2.0, 0.0, -3.0), 30)
                .addLine(new RelativeLocation(2.0, 0.0, -3.0), new RelativeLocation(-2.0, 0.0, -3.0), 30)
                // Grip
                .addLine(new RelativeLocation(-0.5, 0.0, -3.0), new RelativeLocation(-0.5, 0.0, -5.0), 30)
                .addLine(new RelativeLocation(0.5, 0.0, -3.0), new RelativeLocation(0.5, 0.0, -5.0), 30)
                // Guard bottom
                .addLine(new RelativeLocation(-3.0, 0.0, -2.0), new RelativeLocation(-2.0, 0.0, -3.0), 30)
                // Pommel
                .addLine(new RelativeLocation(0.5, 0.0, -5.0), new RelativeLocation(-0.5, 0.0, -5.0), 30)
                .create();
    }
}
