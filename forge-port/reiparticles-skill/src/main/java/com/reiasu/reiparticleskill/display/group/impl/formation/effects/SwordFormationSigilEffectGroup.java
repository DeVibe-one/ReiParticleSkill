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

public final class SwordFormationSigilEffectGroup extends ServerOnlyDisplayGroup {
    private static final Vector3f BASE_COLOR = new Vector3f(0.98F, 0.89F, 0.48F);
    private static final Vector3f ACCENT_COLOR = new Vector3f(0.46F, 0.99F, 0.79F);

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);
    private int age;
    private int maxAge = -1;
    private double scale = 1.0;
    private double fastSpinSpeed = 0.04908738521234052;
    private double slowSpinSpeed = 0.02454369260617026;
    private int outerRingSamples = 240;
    private boolean fading;
    private int fadeTick;
    private int fadeMaxTick = 20;

    public SwordFormationSigilEffectGroup(Vec3 pos, Level world) {
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

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = Math.max(0.05, scale);
    }

    public double getFastSpinSpeed() {
        return fastSpinSpeed;
    }

    public void setFastSpinSpeed(double fastSpinSpeed) {
        this.fastSpinSpeed = fastSpinSpeed;
    }

    public double getSlowSpinSpeed() {
        return slowSpinSpeed;
    }

    public void setSlowSpinSpeed(double slowSpinSpeed) {
        this.slowSpinSpeed = slowSpinSpeed;
    }

    public int getOuterRingSamples() {
        return outerRingSamples;
    }

    public void setOuterRingSamples(int outerRingSamples) {
        this.outerRingSamples = Math.max(24, outerRingSamples);
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
        double grow = Math.min(1.0, age / 18.0);
        double fadeFactor = fading ? Math.max(0.0, 1.0 - fadeTick / (double) fadeMaxTick) : 1.0;
        double currentScale = scale * (0.20 + 0.80 * grow) * fadeFactor;
        double fastSpin = age * fastSpinSpeed;
        double slowSpin = -age * slowSpinSpeed;

        // Main circle + cross rays.
        drawRing(level, center, basis, 4.0 * currentScale, (int) (outerRingSamples * fadeFactor), fastSpin, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, 3.410561, 2.089994, 0.0, 8.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, 3.410561, -2.089994, 0.0, -8.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, -3.410561, 2.089994, 0.0, 8.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, -3.410561, -2.089994, 0.0, -8.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, 3.18319, 2.483995, 8.0, 0.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, 3.18319, -2.483995, 8.0, 0.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, -3.18319, 2.483995, -8.0, 0.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));
        drawLine(level, center, basis, -3.18319, -2.483995, -8.0, 0.0, 70, fastSpin, currentScale, BASE_COLOR, (float) (0.42 * fadeFactor));

        // Rotated square (radius 6, 45 degrees).
        drawSquare(level, center, basis, 6.0 * currentScale, Math.PI * 0.25 + slowSpin, ACCENT_COLOR, (float) (0.40 * fadeFactor));

        // Four small triangular runes near outer ring.
        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI * 0.5) + slowSpin;
            drawTriangle(level, center, basis, 7.8 * currentScale, angle, currentScale, ACCENT_COLOR);
            drawMiniSword(level, center, basis, 5.25 * currentScale, angle + Math.PI * 0.25, currentScale * 0.9, fastSpin, fadeFactor);
        }

        if (age % 6 == 0) {
            FormationParticleHelper.spawn(level, ParticleTypes.ENCHANT, center, Math.max(1, (int) (8 * fadeFactor)), 1.8 * currentScale, 0.01);
        }
        if (age % 11 == 0) {
            FormationParticleHelper.spawn(level, ParticleTypes.END_ROD, center, Math.max(1, (int) (4 * fadeFactor)), 2.1 * currentScale, 0.0);
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

    private static void drawRing(
            ServerLevel level,
            Vec3 center,
            FormationParticleHelper.Basis basis,
            double radius,
            int samples,
            double spin,
            Vector3f color,
            float size
    ) {
        int n = Math.max(8, samples);
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2.0 * i / n + spin;
            Vec3 p = toWorld(center, basis, Math.cos(angle) * radius, Math.sin(angle) * radius);
            FormationParticleHelper.spawnDust(level, p, color, size);
        }
    }

    private static void drawSquare(
            ServerLevel level,
            Vec3 center,
            FormationParticleHelper.Basis basis,
            double radius,
            double rotation,
            Vector3f color,
            float size
    ) {
        double[] xs = {radius, 0.0, -radius, 0.0};
        double[] zs = {0.0, radius, 0.0, -radius};
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double x1 = xs[i] * Math.cos(rotation) - zs[i] * Math.sin(rotation);
            double z1 = xs[i] * Math.sin(rotation) + zs[i] * Math.cos(rotation);
            double x2 = xs[next] * Math.cos(rotation) - zs[next] * Math.sin(rotation);
            double z2 = xs[next] * Math.sin(rotation) + zs[next] * Math.cos(rotation);
            drawLine(level, center, basis, x1, z1, x2, z2, 40, 0.0, 1.0, color, size);
        }
    }

    private static void drawTriangle(
            ServerLevel level,
            Vec3 center,
            FormationParticleHelper.Basis basis,
            double orbitRadius,
            double orbitAngle,
            double scale,
            Vector3f color
    ) {
        double cx = Math.cos(orbitAngle) * orbitRadius;
        double cz = Math.sin(orbitAngle) * orbitRadius;
        double localRotation = orbitAngle + Math.PI;

        Vec3 a = toWorld(center, basis, cx + rotateX(0.0 * scale, 2.0 * scale, localRotation), cz + rotateZ(0.0 * scale, 2.0 * scale, localRotation));
        Vec3 b = toWorld(center, basis, cx + rotateX(-1.0 * scale, 1.0 * scale, localRotation), cz + rotateZ(-1.0 * scale, 1.0 * scale, localRotation));
        Vec3 c = toWorld(center, basis, cx + rotateX(1.0 * scale, 1.0 * scale, localRotation), cz + rotateZ(1.0 * scale, 1.0 * scale, localRotation));

        drawLineWorld(level, a, b, 20, color, 0.34F);
        drawLineWorld(level, a, c, 20, color, 0.34F);
        drawLineWorld(level, b, c, 20, color, 0.34F);
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
        double sx1 = rotateX(x1 * scale, z1 * scale, spin);
        double sz1 = rotateZ(x1 * scale, z1 * scale, spin);
        double sx2 = rotateX(x2 * scale, z2 * scale, spin);
        double sz2 = rotateZ(x2 * scale, z2 * scale, spin);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double x = sx1 + (sx2 - sx1) * t;
            double z = sz1 + (sz2 - sz1) * t;
            Vec3 p = toWorld(center, basis, x, z);
            FormationParticleHelper.spawnDust(level, p, color, size);
        }
    }

    private static void drawLineWorld(ServerLevel level, Vec3 from, Vec3 to, int steps, Vector3f color, float size) {
        int n = Math.max(1, steps);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            Vec3 p = from.add(to.subtract(from).scale(t));
            FormationParticleHelper.spawnDust(level, p, color, size);
        }
    }

    private static Vec3 toWorld(Vec3 center, FormationParticleHelper.Basis basis, double x, double z) {
        return center.add(basis.u().scale(x)).add(basis.v().scale(z));
    }

    private static double rotateX(double x, double z, double angle) {
        return x * Math.cos(angle) - z * Math.sin(angle);
    }

    private static double rotateZ(double x, double z, double angle) {
        return x * Math.sin(angle) + z * Math.cos(angle);
    }

    private static void drawMiniSword(
            ServerLevel level,
            Vec3 center,
            FormationParticleHelper.Basis basis,
            double orbitRadius,
            double orbitAngle,
            double scale,
            double spin,
            double alpha
    ) {
        double cx = Math.cos(orbitAngle) * orbitRadius;
        double cz = Math.sin(orbitAngle) * orbitRadius;
        double localRot = orbitAngle + spin + Math.PI;
        float size = (float) (0.30 * Math.max(0.2, alpha));

        // Pommel + guard.
        drawLine(level, center, basis, cx - 0.45 * scale, cz - 1.5 * scale, cx + 0.45 * scale, cz - 1.5 * scale, 14, localRot, 1.0, ACCENT_COLOR, size);
        drawLine(level, center, basis, cx - 1.2 * scale, cz - 0.2 * scale, cx + 1.2 * scale, cz - 0.2 * scale, 14, localRot, 1.0, ACCENT_COLOR, size);
        // Blade.
        drawLine(level, center, basis, cx, cz - 1.5 * scale, cx, cz + 2.2 * scale, 24, localRot, 1.0, BASE_COLOR, (float) (size + 0.03));
        drawLine(level, center, basis, cx - 0.32 * scale, cz + 1.7 * scale, cx, cz + 2.2 * scale, 10, localRot, 1.0, BASE_COLOR, size);
        drawLine(level, center, basis, cx + 0.32 * scale, cz + 1.7 * scale, cx, cz + 2.2 * scale, 10, localRot, 1.0, BASE_COLOR, size);
    }
}
