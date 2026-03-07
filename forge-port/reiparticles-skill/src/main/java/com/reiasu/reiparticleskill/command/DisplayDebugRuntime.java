// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command;

import com.reiasu.reiparticleskill.command.layout.DisplayIndexKind;
import com.reiasu.reiparticleskill.command.layout.DisplayIndexPlan;
import com.reiasu.reiparticleskill.command.layout.DisplaySpawnProfile;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.group.layout.SimpleSwordFormationLayout;
import com.reiasu.reiparticleskill.display.group.layout.SwordFormationLayerPresets;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

import java.util.List;

public final class DisplayDebugRuntime {
    private DisplayDebugRuntime() {
    }

    public static int previewFormation(ServerLevel level, double x, double y, double z) {
        List<SimpleSwordFormationLayout> layouts =
                SwordFormationLayerPresets.createDefaultLayouts(new RelativeLocation(0.0, 0.0, 1.0));

        int emitted = 0;
        for (SimpleSwordFormationLayout layout : layouts) {
            for (RelativeLocation offset : layout.currentOffsets()) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x + offset.getX(),
                        y + 0.2 + offset.getY(),
                        z + offset.getZ(),
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                );
                emitted++;
            }
            layout.tick();
        }
        return emitted;
    }

    public static int previewDisplayProfile(ServerLevel level, double x, double y, double z, DisplaySpawnProfile profile) {
        float scale = profile.targetScale();
        float speed = profile.scaledSpeed();

        int count = Math.max(8, (int) (scale * 8));
        var color = new DustParticleOptions(new Vector3f(0.95f, 0.75f, 0.25f), Math.max(0.8f, scale * 0.15f));
        for (int i = 0; i < count; i++) {
            double dy = i * (0.08 + speed * 0.02);
            level.sendParticles(color, x, y + dy, z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        level.sendParticles(
                ParticleTypes.CRIT,
                x,
                y + 0.1,
                z,
                8,
                0.2,
                0.2,
                0.2,
                0.02
        );
        return count + 8;
    }

    public static int previewByPlan(ServerLevel level, double x, double y, double z, DisplayIndexPlan plan) {
        DisplayIndexKind kind = plan.kind();
        return switch (kind) {
            case EMITTER -> previewEmitter(level, x, y, z);
            case STYLE -> previewStyle(level, x, y, z);
            case ENTITY, DISPLAY -> {
                if (plan.profile().isPresent()) {
                    yield previewDisplayProfile(level, x, y, z, plan.profile().get());
                }
                yield previewDisplayProfile(level, x, y, z, DisplaySpawnProfile.defaultFacingUp());
            }
            case GROUP -> previewFormation(level, x, y, z);
            case COMPOSITION -> previewComposition(level, x, y, z);
        };
    }

    private static int previewEmitter(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.CLOUD, x, y + 0.2, z, 80, 0.8, 0.2, 0.8, 0.02);
        level.sendParticles(ParticleTypes.POOF, x, y + 0.3, z, 24, 0.4, 0.3, 0.4, 0.02);
        return 104;
    }

    private static int previewStyle(ServerLevel level, double x, double y, double z) {
        int emitted = 0;
        for (int i = 0; i < 64; i++) {
            double t = Math.PI * 2.0 * i / 64.0;
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    x + Math.cos(t) * 2.6,
                    y + 0.1,
                    z + Math.sin(t) * 2.6,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
            emitted++;
        }
        return emitted;
    }

    private static int previewComposition(ServerLevel level, double x, double y, double z) {
        int emitted = 0;
        for (int i = 0; i < 80; i++) {
            double t = i * 0.22;
            double radius = 0.1 + i * 0.03;
            level.sendParticles(
                    ParticleTypes.WAX_ON,
                    x + Math.cos(t) * radius,
                    y + 0.05 + i * 0.02,
                    z + Math.sin(t) * radius,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
            emitted++;
        }
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 30, 0.3, 0.5, 0.3, 0.01);
        return emitted + 30;
    }
}

