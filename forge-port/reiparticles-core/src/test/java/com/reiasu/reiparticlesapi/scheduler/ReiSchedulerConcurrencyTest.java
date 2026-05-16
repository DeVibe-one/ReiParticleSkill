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
package com.reiasu.reiparticlesapi.scheduler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReiSchedulerConcurrencyTest {
    @Test
    void cancelShouldInvokeFinishCallbackOnlyOnceAcrossConcurrentCallers() throws Exception {
        int concurrentCallers = 16;
        int attempts = 200;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentCallers);
        try {
            for (int attempt = 0; attempt < attempts; attempt++) {
                ReiScheduler.TickRunnable task = new ReiScheduler.TickRunnable(1, () -> {
                }, false, 0);
                AtomicInteger callbackCalls = new AtomicInteger();
                CountDownLatch ready = new CountDownLatch(concurrentCallers);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<>();
                task.setFinishCallback(callbackCalls::incrementAndGet);
                for (int i = 0; i < concurrentCallers; i++) {
                    futures.add(executor.submit(() -> {
                        try {
                            ready.countDown();
                            if (!start.await(5, TimeUnit.SECONDS)) {
                                throw new AssertionError("Timed out waiting to start concurrent cancellation");
                            }
                            task.cancel();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError("Interrupted while waiting to cancel task", e);
                        }
                    }));
                }
                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                for (Future<?> future : futures) {
                    future.get(5, TimeUnit.SECONDS);
                }
                assertTrue(task.isCancelled());
                assertEquals(1, callbackCalls.get(), "attempt " + attempt);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cancelShouldNotWaitForRunningActionToReleaseSchedulerLock() throws Exception {
        CountDownLatch actionStarted = new CountDownLatch(1);
        CountDownLatch releaseAction = new CountDownLatch(1);
        ReiScheduler.TickRunnable task = new ReiScheduler.TickRunnable(1, () -> {
            actionStarted.countDown();
            try {
                if (!releaseAction.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release scheduler action");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while running scheduler action", e);
            }
        }, false, 0);
        AtomicInteger callbackCalls = new AtomicInteger();
        task.setFinishCallback(callbackCalls::incrementAndGet);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> ticking = executor.submit(task::doTick);
            assertTrue(actionStarted.await(5, TimeUnit.SECONDS));

            Future<?> cancel = executor.submit(task::cancel);
            cancel.get(1, TimeUnit.SECONDS);
            assertTrue(task.isCancelled());
            assertEquals(1, callbackCalls.get());

            releaseAction.countDown();
            ticking.get(5, TimeUnit.SECONDS);
            assertFalse(readBooleanField(task, "actionInProgress"));
        } finally {
            releaseAction.countDown();
            executor.shutdownNow();
        }
    }

    private static boolean readBooleanField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to read field " + fieldName, e);
        }
    }
}
