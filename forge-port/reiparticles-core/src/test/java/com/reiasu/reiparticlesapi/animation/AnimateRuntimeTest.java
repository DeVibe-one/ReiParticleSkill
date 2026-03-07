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
package com.reiasu.reiparticlesapi.animation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimateRuntimeTest {
    @AfterEach
    void cleanup() {
        AnimateManager.INSTANCE.clear();
    }

    @Test
    void shouldDriveAnimateToCompletionByServerTicks() {
        CountingAction action = new CountingAction(3);
        Animate animate = new Animate().addNode(new AnimateNode().addAction(action));

        AnimateManager.INSTANCE.displayAnimateServer(animate);
        assertEquals(1, AnimateManager.INSTANCE.activeCount());

        AnimateManager.INSTANCE.tickServer();
        AnimateManager.INSTANCE.tickServer();
        assertEquals(1, AnimateManager.INSTANCE.activeCount());

        AnimateManager.INSTANCE.tickServer();
        assertEquals(1, AnimateManager.INSTANCE.activeCount());
        AnimateManager.INSTANCE.tickServer();
        assertEquals(0, AnimateManager.INSTANCE.activeCount());
        assertTrue(animate.getDone());
        assertEquals(3, action.ticks);
    }

    @Test
    void shouldCancelWhenPredicateIsSatisfied() {
        CountingAction action = new CountingAction(20);
        Animate animate = new Animate()
                .addNode(new AnimateNode().addAction(action))
                .addCancelPredicate(a -> action.ticks >= 2);

        AnimateManager.INSTANCE.displayAnimateServer(animate);
        AnimateManager.INSTANCE.tickServer();
        AnimateManager.INSTANCE.tickServer();
        AnimateManager.INSTANCE.tickServer();

        assertTrue(animate.getDone());
        assertEquals(0, AnimateManager.INSTANCE.activeCount());
        assertTrue(action.ticks >= 2);
    }

    private static final class CountingAction extends AnimateAction {
        private final int maxTicks;
        private int ticks;

        private CountingAction(int maxTicks) {
            this.maxTicks = maxTicks;
        }

        @Override
        public boolean checkDone() {
            return ticks >= maxTicks;
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public void onStart() {
            ticks = 0;
        }

        @Override
        public void onDone() {
        }
    }
}
