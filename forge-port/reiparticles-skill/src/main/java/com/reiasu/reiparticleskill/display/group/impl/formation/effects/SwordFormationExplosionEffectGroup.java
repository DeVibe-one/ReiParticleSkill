// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation.effects;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public final class SwordFormationExplosionEffectGroup extends ServerOnlyDisplayGroup {
    private static final Vector3f SPHERE_COLOR = new Vector3f(0.988235F, 0.823529F, 0.321569F);
    private static final Vector3f WAVE_COLOR = new Vector3f(0.466667F, 0.992157F, 0.792157F);

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);
    private boolean fired;

    public SwordFormationExplosionEffectGroup(Vec3 pos, Level world) {
        super(pos, world);
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        return Collections.emptyMap();
    }

    public Vec3 getDirection() {
        return direction;
    }

    public void setDirection(Vec3 direction) {
        this.direction = FormationParticleHelper.safeDirection(direction);
    }

    @Override
    public void tick() {
        if (fired) {
            remove();
            return;
        }
        fired = true;
        if (!(getWorld() instanceof ServerLevel level)) {
            remove();
            return;
        }

        Vec3 center = getPos();
        FormationParticleHelper.Basis basis = FormationParticleHelper.basis(direction);

        // Sphere burst.
        for (int i = 0; i < 1600; i++) {
            Vec3 unit = FormationParticleHelper.randomUnit();
            Vec3 velocity = unit.scale(FormationParticleHelper.randomDouble(0.8, 1.4));
            FormationParticleHelper.spawnMovingDust(level, center, velocity, SPHERE_COLOR, 0.52F);
            if (i % 4 == 0) {
                FormationParticleHelper.spawnMovingDust(level, center, velocity.scale(1.15), WAVE_COLOR, 0.62F);
            }
        }

        // Disk wave in current plane.
        int ringSamples = 420;
        for (int i = 0; i < ringSamples; i++) {
            double angle = Math.PI * 2.0 * i / ringSamples;
            Vec3 radial = basis.u().scale(Math.cos(angle)).add(basis.v().scale(Math.sin(angle))).normalize();
            Vec3 start = center.add(radial.scale(0.6));
            Vec3 velocity = radial.scale(FormationParticleHelper.randomDouble(1.4, 2.6))
                    .add(basis.axis().scale(FormationParticleHelper.randomDouble(-0.22, 0.35)));
            FormationParticleHelper.spawnMovingDust(level, start, velocity, WAVE_COLOR, 0.7F);
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, start.x, start.y, start.z, 0,
                        velocity.x * 0.65, velocity.y * 0.65, velocity.z * 0.65, 1.0);
            }
        }

        FormationParticleHelper.spawn(level, ParticleTypes.FLASH, center, 2, 0.0, 0.0);
        FormationParticleHelper.spawn(level, ParticleTypes.END_ROD, center, 180, 6.0, 0.01);
        FormationParticleHelper.spawn(level, ParticleTypes.ELECTRIC_SPARK, center, 120, 4.0, 0.65);
    }

    @Override
    public void onDisplay() {
        fired = false;
    }
}
