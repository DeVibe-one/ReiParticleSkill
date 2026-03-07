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
package com.reiasu.reiparticlesapi;

import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReiParticlesAPISchedulerTest {

    @BeforeEach
    @AfterEach
    void clearGlobalScheduler() {
        ReiScheduler.INSTANCE.clear();
    }

    private ReiParticlesAPI.Scheduler createScheduler() {
        return new ReiParticlesAPI.Scheduler();
    }

    @Test
    void shouldExecuteScheduledTask() {
        ReiParticlesAPI.Scheduler scheduler = createScheduler();
        AtomicBoolean executed = new AtomicBoolean(false);

        scheduler.runTask(1, () -> executed.set(true));

        scheduler.tick();
        assertTrue(executed.get());
    }

    @Test
    void shouldExecuteWhenTickIsZero() {
        ReiParticlesAPI.Scheduler scheduler = createScheduler();
        AtomicBoolean executed = new AtomicBoolean(false);

        scheduler.runTask(0, () -> executed.set(true));

        assertFalse(executed.get());
        scheduler.tick();
        assertTrue(executed.get());
    }

    @Test
    void serverTasksShouldNotAdvanceOnClientTicks() {
        AtomicBoolean executed = new AtomicBoolean(false);

        ReiScheduler.INSTANCE.runTask(1, () -> executed.set(true));

        ReiScheduler.INSTANCE.doClientTick();
        assertFalse(executed.get());

        ReiScheduler.INSTANCE.doServerTick();
        assertTrue(executed.get());
    }
}
