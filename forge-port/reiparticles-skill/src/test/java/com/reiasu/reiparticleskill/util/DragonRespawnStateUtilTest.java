/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesSkill.
 *
 * ReiParticlesSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.util;

import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DragonRespawnStateUtilTest {
    @AfterEach
    void cleanup() {
        ParticleEmittersManager.clear();
    }

    @Test
    void shouldRegisterEmitterBeforeRunningInitialTick() {
        CountingEmitter emitter = new CountingEmitter();
        emitter.setMaxTick(3);

        CountingEmitter returned = DragonRespawnStateUtil.spawnManagedEmitter(emitter);

        assertSame(emitter, returned);
        assertEquals(1, emitter.emittedTicks);
        assertEquals(1, emitter.getTick());
        assertEquals(1, ParticleEmittersManager.activeCount());

        ParticleEmittersManager.tickAll();

        assertEquals(2, emitter.emittedTicks);
        assertEquals(2, emitter.getTick());
        assertEquals(1, ParticleEmittersManager.activeCount());
    }

    private static final class CountingEmitter extends ParticleEmitters {
        private int emittedTicks;

        @Override
        protected void emitTick() {
            emittedTicks++;
        }
    }
}
