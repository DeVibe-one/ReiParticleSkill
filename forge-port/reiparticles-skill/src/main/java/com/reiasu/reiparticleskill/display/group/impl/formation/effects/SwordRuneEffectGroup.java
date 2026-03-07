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

public final class SwordRuneEffectGroup extends ServerOnlyDisplayGroup {
    private static final Vector3f RING_COLOR = new Vector3f(0.93F, 0.94F, 0.68F);
    private static final Vector3f RING_COLOR_ALT = new Vector3f(0.43F, 0.87F, 0.72F);
    private static final Vector3f RUNE_COLOR = new Vector3f(0.95F, 0.80F, 0.33F);

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);
    private int age;
    private int maxAge = -1;
    private double runeScale = 1.0;
    private double outerRadius = 8.0;
    private int ringSamples = 96;
    private boolean fading;
    private int fadeTick;
    private int fadeMaxTick = 20;

    public SwordRuneEffectGroup(Vec3 pos, Level world) {
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

    public double getRuneScale() {
        return runeScale;
    }

    public void setRuneScale(double runeScale) {
        this.runeScale = Math.max(0.05, runeScale);
    }

    public double getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(double outerRadius) {
        this.outerRadius = Math.max(0.5, outerRadius);
    }

    public int getRingSamples() {
        return ringSamples;
    }

    public void setRingSamples(int ringSamples) {
        this.ringSamples = Math.max(24, ringSamples);
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
        double spinFast = age * 0.04908738521234052;
        double spinSlow = -age * 0.02454369260617026;
        double progress = Math.min(1.0, age / 20.0);
        double fade = fading ? Math.max(0.0, 1.0 - fadeTick / (double) fadeMaxTick) : 1.0;
        double scale = (0.15 + progress * 0.85) * runeScale * fade;

        int samples = Math.max(24, ringSamples);
        for (int i = 0; i < samples; i++) {
            double a1 = Math.PI * 2.0 * i / samples + spinFast;
            double a2 = -Math.PI * 2.0 * i / samples + spinSlow;
            Vec3 ringOuter = FormationParticleHelper.onPlane(center, basis, outerRadius * scale, a1, 0.0);
            Vec3 ringInner = FormationParticleHelper.onPlane(center, basis, outerRadius * 0.5 * scale, a2, 0.0);
            FormationParticleHelper.spawnDust(level, ringOuter, RING_COLOR, (float) (0.56 * fade));
            if (i % 2 == 0) {
                FormationParticleHelper.spawnDust(level, ringInner, RING_COLOR_ALT, (float) (0.48 * fade));
            }
            if (i % 7 == 0) {
                FormationParticleHelper.spawn(level, ParticleTypes.ENCHANT, ringOuter, 1, 0.0, 0.0);
            }
        }

        // Sword glyph in runic plane.
        drawLine(level, center, basis, 0.0, -2.0, 0.0, 3.6, 36, spinFast, scale, RUNE_COLOR, (float) (0.58 * fade));   // spine
        drawLine(level, center, basis, -1.45, 0.0, 1.45, 0.0, 20, spinFast, scale, RUNE_COLOR, (float) (0.54 * fade)); // crossguard
        drawLine(level, center, basis, -0.55, -1.2, 0.55, -1.2, 12, spinFast, scale, RUNE_COLOR, (float) (0.52 * fade));
        drawLine(level, center, basis, -0.35, 3.6, 0.0, 4.2, 12, spinFast, scale, RUNE_COLOR, (float) (0.56 * fade));  // tip left
        drawLine(level, center, basis, 0.35, 3.6, 0.0, 4.2, 12, spinFast, scale, RUNE_COLOR, (float) (0.56 * fade));   // tip right
        drawLine(level, center, basis, -0.55, 2.0, -1.1, 2.7, 14, spinFast, scale, RUNE_COLOR, (float) (0.5 * fade));
        drawLine(level, center, basis, 0.55, 2.0, 1.1, 2.7, 14, spinFast, scale, RUNE_COLOR, (float) (0.5 * fade));

        if (age % 8 == 0) {
            FormationParticleHelper.spawn(level, ParticleTypes.FLASH, center, 1, 0.0, 0.0);
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

    private static void drawLine(
            ServerLevel level,
            Vec3 center,
            FormationParticleHelper.Basis basis,
            double x1,
            double z1,
            double x2,
            double z2,
            int steps,
            double spin,
            double scale,
            Vector3f color,
            float size
    ) {
        int n = Math.max(1, steps);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double lx = x1 + (x2 - x1) * t;
            double lz = z1 + (z2 - z1) * t;
            double rx = lx * Math.cos(spin) - lz * Math.sin(spin);
            double rz = lx * Math.sin(spin) + lz * Math.cos(spin);
            Vec3 p = center.add(basis.u().scale(rx * scale)).add(basis.v().scale(rz * scale));
            FormationParticleHelper.spawnDust(level, p, color, size);
        }
    }
}
