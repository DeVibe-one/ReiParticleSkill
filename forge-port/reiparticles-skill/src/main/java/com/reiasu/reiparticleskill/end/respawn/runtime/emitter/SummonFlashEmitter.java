// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import com.reiasu.reiparticlesapi.network.particle.util.ServerParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SummonFlashEmitter extends TimedRespawnEmitter {
    private static final Vector3f FLASH_COLOR = new Vector3f(220f / 255f, 120f / 255f, 1.0f);
    private static final int BURST_POINTS = 96;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private final RandomSource random = RandomSource.create();
    private Vec3 anchorOffset = Vec3.ZERO;

    public SummonFlashEmitter(int maxTicks) {
        super(maxTicks);
    }

    public SummonFlashEmitter setAnchorOffset(Vec3 anchorOffset) {
        this.anchorOffset = anchorOffset == null ? Vec3.ZERO : anchorOffset;
        return this;
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        if (tick > 0) {
            return 0;
        }

        Vec3 origin = center.add(anchorOffset);
        int emitted = 0;

        ServerParticleUtil.sendForce(level, ParticleTypes.FLASH,
                origin.x, origin.y, origin.z,
                0, 0.0, 0.0, 0.0, 1.0);
        emitted++;

        ServerParticleUtil.sendForce(level, ParticleTypes.EXPLOSION_EMITTER,
                origin.x, origin.y, origin.z,
                1, 0.0, 0.0, 0.0, 0.0);
        emitted++;

        DustParticleOptions dust = new DustParticleOptions(FLASH_COLOR, 2.2f);
        for (int i = 0; i < BURST_POINTS; i++) {
            double y = 1.0 - (2.0 * i + 1.0) / BURST_POINTS;
            double lateral = Math.sqrt(1.0 - y * y);
            double angle = GOLDEN_ANGLE * i + random.nextDouble() * 0.08;
            Vec3 dir = new Vec3(lateral * Math.cos(angle), y, lateral * Math.sin(angle)).normalize();
            double radius = 1.5 + random.nextDouble() * 4.0;
            Vec3 pos = origin.add(dir.scale(radius));
            Vec3 velocity = dir.scale(0.25 + random.nextDouble() * 0.2);

            ServerParticleUtil.sendForce(level, ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0);
            emitted++;

            if ((i & 3) == 0) {
                ServerParticleUtil.sendForce(level, dust,
                        pos.x, pos.y, pos.z,
                        1, 0.1, 0.1, 0.1, 0.0);
                emitted++;
            }
        }

        return emitted;
    }
}
