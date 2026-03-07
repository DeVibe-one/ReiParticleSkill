// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation.effects;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

final class FormationParticleHelper {
    private FormationParticleHelper() {
    }

    static Basis basis(Vec3 direction) {
        Vec3 axis = safeDirection(direction);
        Vec3 ref = Math.abs(axis.y) < 0.98 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 u = axis.cross(ref);
        if (u.lengthSqr() < 1.0E-8) {
            ref = new Vec3(0.0, 0.0, 1.0);
            u = axis.cross(ref);
        }
        u = u.normalize();
        Vec3 v = axis.cross(u).normalize();
        return new Basis(axis, u, v);
    }

    static Vec3 safeDirection(Vec3 direction) {
        if (direction == null || direction.lengthSqr() < 1.0E-8) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return direction.normalize();
    }

    static Vec3 onPlane(Vec3 center, Basis basis, double radius, double angle, double axialOffset) {
        Vec3 radial = basis.u.scale(Math.cos(angle) * radius).add(basis.v.scale(Math.sin(angle) * radius));
        return center.add(radial).add(basis.axis.scale(axialOffset));
    }

    static float randomFloat(float min, float max) {
        if (max <= min) {
            return min;
        }
        return (float) ThreadLocalRandom.current().nextDouble(min, max);
    }

    static double randomDouble(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    static Vector3f lerpColor(Vector3f from, Vector3f to, float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        return new Vector3f(
                from.x + (to.x - from.x) * clamped,
                from.y + (to.y - from.y) * clamped,
                from.z + (to.z - from.z) * clamped
        );
    }

    static void spawnDust(ServerLevel level, Vec3 pos, Vector3f color, float size) {
        if (level == null || pos == null || color == null) {
            return;
        }
        level.sendParticles(new DustParticleOptions(color, Math.max(0.01F, size)),
                pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    static void spawnMovingDust(ServerLevel level, Vec3 pos, Vec3 velocity, Vector3f color, float size) {
        if (level == null || pos == null || velocity == null || color == null) {
            return;
        }
        level.sendParticles(new DustParticleOptions(color, Math.max(0.01F, size)),
                pos.x, pos.y, pos.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
    }

    static void spawn(ServerLevel level, ParticleOptions options, Vec3 pos, int count, double spread, double speed) {
        if (level == null || pos == null || options == null) {
            return;
        }
        level.sendParticles(options, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
    }

    static Vec3 randomUnit() {
        while (true) {
            double x = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double y = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double z = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double l2 = x * x + y * y + z * z;
            if (l2 < 1.0E-6 || l2 > 1.0) {
                continue;
            }
            return new Vec3(x, y, z).normalize();
        }
    }

    record Basis(Vec3 axis, Vec3 u, Vec3 v) {
    }
}
