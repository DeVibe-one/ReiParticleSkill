// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class EndRespawnEffectPreview {
    private static final DustParticleOptions MAIN_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 1.2f);

    private EndRespawnEffectPreview() {
    }

    public static int emit(ServerLevel level, Vec3 center, EndRespawnPhase phase, long phaseTick) {
        return switch (phase) {
            case START -> emitStart(level, center, phaseTick);
            case SUMMON_PILLARS -> emitSummonPillars(level, center, phaseTick);
            case SUMMONING_DRAGON -> emitSummoningDragon(level, center, phaseTick);
            case BEFORE_END_WAITING -> emitBeforeEndWaiting(level, center, phaseTick);
            case END -> emitEnd(level, center, phaseTick);
        };
    }

    private static int emitStart(ServerLevel level, Vec3 c, long tick) {
        int emitted = 0;
        for (int i = 0; i < 48; i++) {
            double t = (Math.PI * 2.0 * i) / 48.0 + tick * 0.03;
            level.sendParticles(MAIN_COLOR, c.x + Math.cos(t) * 6.0, c.y + 1.0, c.z + Math.sin(t) * 6.0, 1, 0, 0, 0, 0);
            emitted++;
        }
        level.sendParticles(ParticleTypes.PORTAL, c.x, c.y + 2.0, c.z, 32, 0.8, 0.6, 0.8, 0.01);
        return emitted + 32;
    }

    private static int emitSummonPillars(ServerLevel level, Vec3 c, long tick) {
        int emitted = 0;
        for (int ring = 0; ring < 3; ring++) {
            double radius = 10.0 + ring * 6.0;
            int count = 56 + ring * 28;
            for (int i = 0; i < count; i++) {
                double t = (Math.PI * 2.0 * i) / count + tick * (0.01 + ring * 0.004);
                level.sendParticles(MAIN_COLOR, c.x + Math.cos(t) * radius, c.y + 2.0 + ring * 6.0, c.z + Math.sin(t) * radius, 1, 0, 0, 0, 0);
                emitted++;
            }
        }
        return emitted;
    }

    private static int emitSummoningDragon(ServerLevel level, Vec3 c, long tick) {
        int emitted = 0;
        for (int i = 0; i < 96; i++) {
            double t = tick * 0.1 + i * 0.26;
            double r = 2.0 + i * 0.03;
            level.sendParticles(MAIN_COLOR, c.x + Math.cos(t) * r, c.y + 0.8 + i * 0.04, c.z + Math.sin(t) * r, 1, 0, 0, 0, 0);
            emitted++;
        }
        level.sendParticles(ParticleTypes.DRAGON_BREATH, c.x, c.y + 8.0, c.z, 80, 0.6, 4.0, 0.6, 0.01);
        return emitted + 80;
    }

    private static int emitBeforeEndWaiting(ServerLevel level, Vec3 c, long tick) {
        int emitted = 0;
        for (int i = 0; i < 72; i++) {
            double t = (Math.PI * 2.0 * i) / 72.0;
            double y = c.y + 1.2 + Math.sin(tick * 0.08 + i * 0.15) * 0.8;
            level.sendParticles(MAIN_COLOR, c.x + Math.cos(t) * 5.0, y, c.z + Math.sin(t) * 5.0, 1, 0, 0, 0, 0);
            emitted++;
        }
        return emitted;
    }

    private static int emitEnd(ServerLevel level, Vec3 c, long tick) {
        int emitted = 0;
        if (tick == 0) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1.0, c.z, 1, 0, 0, 0, 0);
            emitted++;
        }
        level.sendParticles(MAIN_COLOR, c.x, c.y + 2.5, c.z, 120, 2.6, 2.6, 2.6, 0.04);
        emitted += 120;
        return emitted;
    }
}

