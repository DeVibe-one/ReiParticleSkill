/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParticleEmittersManagerTest {
    @AfterEach
    void cleanup() {
        ParticleEmittersManager.clear();
    }

    @Test
    void shouldTickAndPruneEmittersAtMaxTick() {
        CountingEmitter emitter = new CountingEmitter();
        emitter.setMaxTick(3);
        ParticleEmittersManager.spawnEmitters(emitter);

        assertEquals(1, ParticleEmittersManager.activeCount());

        ParticleEmittersManager.tickAll();
        ParticleEmittersManager.tickAll();
        assertEquals(1, ParticleEmittersManager.activeCount());

        ParticleEmittersManager.tickAll();
        assertEquals(0, ParticleEmittersManager.activeCount());
        assertEquals(3, emitter.emittedTicks);
    }

    @Test
    void shouldIgnoreNonEmitterObjects() {
        ParticleEmittersManager.spawnEmitters("not-an-emitter");
        assertEquals(0, ParticleEmittersManager.activeCount());
    }

    @Test
    void shouldBindWorldAndPositionWhenSpawningInWorld() {
        ServerLevel level = UnsafeAllocator.allocate(ServerLevel.class);
        Vec3 pos = new Vec3(7.0, 80.0, -2.0);
        CountingEmitter emitter = new CountingEmitter();

        emitter.spawnInWorld(level, pos);

        assertEquals(1, ParticleEmittersManager.activeCount());
        assertSame(level, emitter.level());
        assertEquals(pos, emitter.position());
    }

    private static final class CountingEmitter extends ParticleEmitters {
        private int emittedTicks;

        @Override
        protected void emitTick() {
            emittedTicks++;
        }
    }
}
