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
package com.reiasu.reiparticleskill.display.group;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerDisplayGroupManagerTest {
    @AfterEach
    void tearDown() {
        ServerDisplayGroupManager.INSTANCE.clear();
    }

    @Test
    void managerRemovesCanceledGroup() {
        DummyGroup group = new DummyGroup();
        ServerDisplayGroupManager.INSTANCE.spawn(group);

        assertEquals(1, ServerDisplayGroupManager.INSTANCE.getGroups().size());
        ServerDisplayGroupManager.INSTANCE.doTick();
        assertFalse(group.getCanceled());
        ServerDisplayGroupManager.INSTANCE.doTick();

        assertTrue(group.getCanceled());
        assertEquals(0, ServerDisplayGroupManager.INSTANCE.getGroups().size());
    }

    @Test
    void shouldContinueTickingAfterGroupFailure() {
        FailingTickGroup failing = new FailingTickGroup();
        DummyGroup healthy = new DummyGroup(4);
        ServerDisplayGroupManager.INSTANCE.spawn(failing);
        ServerDisplayGroupManager.INSTANCE.spawn(healthy);

        ServerDisplayGroupManager.INSTANCE.doTick();

        assertEquals(1, healthy.getTickCount());
        assertEquals(1, ServerDisplayGroupManager.INSTANCE.getGroups().size());
        assertSame(healthy, ServerDisplayGroupManager.INSTANCE.getGroups().iterator().next());
    }

    @Test
    void clearShouldIgnoreRemoveFailures() {
        ServerDisplayGroupManager.INSTANCE.spawn(new FailingRemoveGroup());
        ServerDisplayGroupManager.INSTANCE.spawn(new DummyGroup());

        assertDoesNotThrow(() -> ServerDisplayGroupManager.INSTANCE.clear());
        assertTrue(ServerDisplayGroupManager.INSTANCE.getGroups().isEmpty());
    }

    private static class DummyGroup extends ServerOnlyDisplayGroup {
        private final int removeAfterTicks;
        private int ticks;

        private DummyGroup() {
            this(2);
        }

        private DummyGroup(int removeAfterTicks) {
            super(Vec3.ZERO, null);
            this.removeAfterTicks = removeAfterTicks;
        }

        int getTickCount() {
            return ticks;
        }

        @Override
        public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
            return Map.of();
        }

        @Override
        public void tick() {
            ticks++;
            if (ticks >= removeAfterTicks) {
                remove();
            }
        }

        @Override
        public void onDisplay() {
        }
    }

    private static final class FailingTickGroup extends DummyGroup {
        @Override
        public void tick() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class FailingRemoveGroup extends DummyGroup {
        @Override
        public void remove() {
            throw new IllegalStateException("boom");
        }
    }
}
