/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticleSkill.
 *
 * ReiParticleSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticleSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticleSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimatsActionTest {

    @BeforeEach
    void setUp() {
        ParticleEmittersManager.clear();
        DisplayEntityManager.INSTANCE.clear();
    }

    @AfterEach
    void tearDown() {
        ParticleEmittersManager.clear();
        DisplayEntityManager.INSTANCE.clear();
    }

    @Test
    void tickingActionStopsAtMaxTick() {
        AtomicInteger calls = new AtomicInteger();
        TickingAction action = new TickingAction(3, it -> calls.incrementAndGet());
        action.onStart();

        for (int i = 0; i < 8; i++) {
            if (action.check()) {
                break;
            }
            action.doTick();
            if (action.check()) {
                action.onDone();
                break;
            }
        }

        assertTrue(action.getCanceled());
        assertTrue(action.getDone());
        assertEquals(4, calls.get());
        assertTrue(action.getFirstTick());
    }

    @Test
    void emitterCrafterActionSpawnsAtInterval() {
        AtomicInteger built = new AtomicInteger();
        EmitterCrafterAction action = new EmitterCrafterAction(
                () -> {
                    built.incrementAndGet();
                    return null;
                },
                3,
                it -> it.getCount() >= 8
        );
        action.onStart();

        while (!action.checkDone()) {
            action.tick();
        }

        assertEquals(8, action.getCount());
        assertEquals(3, built.get());
        assertEquals(0, ParticleEmittersManager.activeCount());
    }

    @Test
    void displayEntityActionSpawnsOnlyOnce() {
        DummyDisplay display = new DummyDisplay();
        DisplayEntityAction action = new DisplayEntityAction(display, it -> {
        });
        action.onStart();

        action.tick();
        action.tick();

        assertEquals(1, DisplayEntityManager.INSTANCE.activeCount());
        assertFalse(action.checkDone());
        action.onDone();
        assertTrue(action.checkDone());
    }

    private static final class DummyDisplay extends DisplayEntity {
        @Override
        public void tick() {
            // no-op for unit test
        }
    }
}
