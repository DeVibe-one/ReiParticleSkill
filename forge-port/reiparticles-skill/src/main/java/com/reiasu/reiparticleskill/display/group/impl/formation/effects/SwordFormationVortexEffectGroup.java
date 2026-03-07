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

public final class SwordFormationVortexEffectGroup extends ServerOnlyDisplayGroup {
    private static final Vector3f VORTEX_COLOR = new Vector3f(24.0F / 255.0F, 129.0F / 255.0F, 108.0F / 255.0F);
    private static final Vector3f OUTER_COLOR = new Vector3f(130.0F / 255.0F, 130.0F / 255.0F, 70.0F / 255.0F);

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);
    private int age;
    private int maxAge = -1;
    private int ringSamples = 72;
    private double innerRadius = 45.0;
    private double outerRadius = 47.0;
    private boolean fading;
    private int fadeTick;
    private int fadeMaxTick = 20;

    public SwordFormationVortexEffectGroup(Vec3 pos, Level world) {
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

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getRingSamples() {
        return ringSamples;
    }

    public void setRingSamples(int ringSamples) {
        this.ringSamples = Math.max(12, ringSamples);
    }

    public double getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(double innerRadius) {
        this.innerRadius = Math.max(0.5, innerRadius);
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(double outerRadius) {
        this.outerRadius = Math.max(0.5, outerRadius);
    }

    public int getFadeMaxTick() {
        return fadeMaxTick;
    }

    public void setFadeMaxTick(int fadeMaxTick) {
        this.fadeMaxTick = Math.max(1, fadeMaxTick);
    }

    @Override
    public void tick() {
        age++;
        if (maxAge > 0 && age > maxAge) {
            remove();
        }
        if (!(getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (fading) {
            fadeTick++;
            if (fadeTick >= fadeMaxTick) {
                super.remove();
                return;
            }
        }

        Vec3 center = getPos();
        FormationParticleHelper.Basis basis = FormationParticleHelper.basis(direction);
        double spin = age * 0.09;
        double fade = fading ? Math.max(0.0, 1.0 - fadeTick / (double) fadeMaxTick) : 1.0;
        int samples = Math.max(12, (int) (ringSamples * fade));

        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2.0 * i / samples + spin;
            double wobble = Math.sin(age * 0.08 + i * 0.19) * 0.36;
            Vec3 p1 = FormationParticleHelper.onPlane(center, basis, innerRadius + wobble, angle, Math.sin(age * 0.04 + i * 0.16) * 0.35);
            FormationParticleHelper.spawnDust(level, p1, VORTEX_COLOR, (float) (0.65 * fade));

            if (i % 2 == 0) {
                Vec3 p2 = FormationParticleHelper.onPlane(center, basis, outerRadius + wobble * 0.6, -angle * 0.75, Math.cos(age * 0.05 + i * 0.21) * 0.42);
                FormationParticleHelper.spawnDust(level, p2, OUTER_COLOR, (float) (0.72 * fade));
            }
            if (i % 5 == 0) {
                Vec3 p3 = FormationParticleHelper.onPlane(center, basis, innerRadius * 0.74, angle * 1.4, Math.sin(age * 0.03 + i * 0.11) * 0.52);
                FormationParticleHelper.spawnDust(level, p3, OUTER_COLOR, (float) (0.36 * fade));
            }
            if (i % 6 == 0) {
                FormationParticleHelper.spawn(level, ParticleTypes.ENCHANT, p1, 1, 0.0, 0.0);
            }
        }

        // Core turbulent stream.
        for (int i = 0; i < Math.max(2, (int) (10 * fade)); i++) {
            double t = FormationParticleHelper.randomDouble(0.0, Math.PI * 2.0);
            double r = FormationParticleHelper.randomDouble(0.2, 1.7);
            Vec3 p = FormationParticleHelper.onPlane(center, basis, r, t, FormationParticleHelper.randomDouble(-1.5, 1.5));
            FormationParticleHelper.spawn(level, ParticleTypes.END_ROD, p, 1, 0.0, 0.0);
            if (i % 3 == 0) {
                FormationParticleHelper.spawn(level, ParticleTypes.ELECTRIC_SPARK, p, 1, 0.0, 0.0);
            }
        }
    }

    @Override
    public void onDisplay() {
        age = 0;
        fading = false;
        fadeTick = 0;
    }

    @Override
    public void remove() {
        if (!fading) {
            fading = true;
            fadeTick = 0;
            return;
        }
        super.remove();
    }
}
