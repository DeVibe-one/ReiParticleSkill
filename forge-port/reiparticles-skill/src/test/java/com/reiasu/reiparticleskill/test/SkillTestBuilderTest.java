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
package com.reiasu.reiparticleskill.test;

import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.test.TestManager;
import com.reiasu.reiparticlesapi.test.api.TestOption;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTestBuilderTest {
    @AfterEach
    void cleanup() {
        ParticleEmittersManager.clear();
        TestManager.INSTANCE.registry().remove(SkillTestBuilder.ID);
    }

    @Test
    void shouldRegisterSkillTestBuilder() {
        SkillTestBuilder.register();

        assertTrue(TestManager.INSTANCE.registry().containsKey(SkillTestBuilder.ID));
    }

    @Test
    void shouldSpawnAndCancelSwordFormationEmitterThroughTestOption() {
        ServerLevel level = UnsafeAllocator.allocate(ServerLevel.class);
        Vec3 eyePos = new Vec3(2.0, 80.0, -4.0);
        TestOption option = SkillTestBuilder.createSwordFormationTest(level, eyePos);

        option.start();

        assertEquals(1, ParticleEmittersManager.activeCount());
        ParticleEmitters emitter = ParticleEmittersManager.getEmitters().get(0);
        assertSame(level, emitter.level());
        assertEquals(eyePos, emitter.position());

        for (int i = 0; i < 20; i++) {
            option.doTick();
        }

        assertFalse(option.isValid());
        assertTrue(emitter.getCanceled());
    }
}

