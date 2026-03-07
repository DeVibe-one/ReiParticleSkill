// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client.ClientCenterEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client.ClientCloudRingEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client.ClientDustEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client.ClientEnchantRingEmitter;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and manages client-rendered particle emitters for the
 * dragon respawn animation phases. Emitter parameters are defined
 * as named presets instead of inline magic numbers.
 */
final class ClientEmitterFactory {

    // --- Rotation speed presets ---
    static final double ROT_VERY_SLOW  = Math.PI / 1024.0;
    static final double ROT_SLOW       = Math.PI / 512.0;
    static final double ROT_MEDIUM     = Math.PI / 256.0;
    static final double ROT_FAST       = Math.PI / 128.0;
    static final double ROT_DEG        = Math.PI / 180.0;

    private final List<ParticleEmitters> active = new ArrayList<>();

    void spawnForStart(ServerLevel level, Vec3 center) {
        // Center geometric pattern
        spawn(new ClientCenterEmitter(center, level, Integer.MAX_VALUE, 0.0), level, center);

        // Dense ambient dust
        spawn(new ClientDustEmitter(center, level, Integer.MAX_VALUE,
                100, 120.0, ROT_SLOW, 0.8, 2.5, 0.0), level, center);

        // Cloud ring A at y=20
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                28.0, 45, 65, 3.0, ROT_DEG, 2.0, 3.5, 20.0), level, center);

        // Cloud ring B at y=40
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                20.0, 30, 45, 2.0, -ROT_DEG, 2.5, 4.0, 40.0), level, center);

        // Enchant ring at y=55
        spawn(new ClientEnchantRingEmitter(center, level, Integer.MAX_VALUE,
                50.0, 55, 80, 8.0, -ROT_MEDIUM, 2.5, 4.0, 55.0), level, center);
    }

    void spawnForSummon(ServerLevel level, Vec3 center) {
        // Center style
        spawn(new ClientCenterEmitter(center, level, Integer.MAX_VALUE, 0.0), level, center);

        // Dust layer A: ambient fill
        spawn(new ClientDustEmitter(center, level, Integer.MAX_VALUE,
                50, 156.0, ROT_SLOW, 0.6, 2.0, 0.0), level, center);

        // Dust layer B: inner fill
        spawn(new ClientDustEmitter(center, level, Integer.MAX_VALUE,
                35, 128.0, ROT_VERY_SLOW, 0.8, 2.5, 0.0), level, center);

        // Cloud ring A (y=30)
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                32.0, 40, 55, 3.0, ROT_DEG, 2.0, 3.5, 30.0), level, center);

        // Cloud ring B (y=50)
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                16.0, 30, 45, 2.0, -ROT_MEDIUM, 2.5, 4.0, 50.0), level, center);

        // Cloud ring C (y=16)
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                24.0, 30, 45, 2.0, -ROT_DEG, 2.0, 3.5, 16.0), level, center);

        // Enchant ring (y=65, large radius)
        spawn(new ClientEnchantRingEmitter(center, level, Integer.MAX_VALUE,
                108.0, 90, 130, 18.0, ROT_SLOW, 3.0, 4.0, 65.0), level, center);

        // Cloud ring D (y=120, top cap)
        spawn(new ClientCloudRingEmitter(center, level, Integer.MAX_VALUE,
                48.0, 50, 75, 8.0, ROT_SLOW, 3.0, 4.0, 120.0), level, center);
    }

    void cancelAll() {
        for (ParticleEmitters emitter : active) {
            emitter.cancel();
        }
        active.clear();
    }

    int activeCount() {
        return active.size();
    }

    private void spawn(ParticleEmitters emitter, ServerLevel level, Vec3 center) {
        ParticleEmittersManager.spawnEmitters(emitter, level, center.x, center.y, center.z);
        active.add(emitter);
    }
}
