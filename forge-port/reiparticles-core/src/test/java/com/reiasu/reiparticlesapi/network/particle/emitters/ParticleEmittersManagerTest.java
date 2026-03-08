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

import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.config.APIConfig;
import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.particle.emitter.EmitterRemoveEvent;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleEmittersManagerTest {
    @AfterEach
    void cleanup() {
        ReiEventBus.INSTANCE.clear();
        ParticleEmittersManager.clear();
        APIConfig.INSTANCE.setParticleCountLimit(131072);
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

    @Test
    void shouldContinueServerTickAfterEmitterFailure() {
        FailingEmitter failing = new FailingEmitter();
        CountingEmitter healthy = new CountingEmitter();
        healthy.setMaxTick(4);
        ParticleEmittersManager.spawnEmitters(failing);
        ParticleEmittersManager.spawnEmitters(healthy);

        ParticleEmittersManager.tickAll();

        assertEquals(1, healthy.emittedTicks);
        assertEquals(1, ParticleEmittersManager.activeCount());
        assertSame(healthy, ParticleEmittersManager.getEmitters().get(0));
    }

    @Test
    void shouldContinueClientTickAfterEmitterFailure() {
        ClientLevel clientWorld = UnsafeAllocator.allocate(ClientLevel.class);
        FailingEmitter failing = new FailingEmitter();
        failing.setUuid(UUID.randomUUID());
        CountingEmitter healthy = new CountingEmitter();
        healthy.setUuid(UUID.randomUUID());
        healthy.setMaxTick(4);
        ParticleEmittersManager.createOrChangeClient(failing, clientWorld);
        ParticleEmittersManager.createOrChangeClient(healthy, clientWorld);

        ParticleEmittersManager.tickClient();

        assertEquals(1, healthy.emittedTicks);
        assertEquals(1, ParticleEmittersManager.getClientEmitters().size());
        assertSame(healthy, ParticleEmittersManager.getClientEmitters().get(healthy.getUuid()));
    }

    @Test
    void shouldClearOnlyClientEmitters() {
        ClientLevel clientWorld = UnsafeAllocator.allocate(ClientLevel.class);
        CountingEmitter serverEmitter = new CountingEmitter();
        CountingEmitter clientEmitter = new CountingEmitter();
        clientEmitter.setUuid(UUID.randomUUID());
        ParticleEmittersManager.spawnEmitters(serverEmitter);
        ParticleEmittersManager.createOrChangeClient(clientEmitter, clientWorld);

        ParticleEmittersManager.clearClient();

        assertEquals(1, ParticleEmittersManager.activeCount());
        assertTrue(ParticleEmittersManager.getClientEmitters().isEmpty());
        assertTrue(clientEmitter.getCanceled());
        assertFalse(serverEmitter.getCanceled());
    }

    @Test
    void shouldRespectConfiguredActiveEmitterLimit() {
        APIConfig.INSTANCE.setParticleCountLimit(1);
        CountingEmitter first = new CountingEmitter();
        CountingEmitter second = new CountingEmitter();

        ParticleEmittersManager.spawnEmitters(first);
        ParticleEmittersManager.spawnEmitters(second);

        assertEquals(1, ParticleEmittersManager.activeCount());
        assertSame(first, ParticleEmittersManager.getEmitters().get(0));
    }

    @Test
    void shouldEnforceConfiguredEmitterLimitUnderConcurrentSpawn() throws InterruptedException {
        APIConfig.INSTANCE.setParticleCountLimit(1);
        ServerLevel level = UnsafeAllocator.allocate(ServerLevel.class);
        int concurrentSpawns = 8;
        CountDownLatch enteredBind = new CountDownLatch(concurrentSpawns);
        CountDownLatch releaseBind = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentSpawns);
        try {
            for (int i = 0; i < concurrentSpawns; i++) {
                executor.submit(() -> ParticleEmittersManager.spawnEmitters(
                        new BlockingBindEmitter(enteredBind, releaseBind),
                        level,
                        1.0,
                        2.0,
                        3.0));
            }
            assertTrue(enteredBind.await(5, TimeUnit.SECONDS));
            releaseBind.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            releaseBind.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, ParticleEmittersManager.activeCount());
    }

    @Test
    void clearShouldPublishRemoveEventForEveryServerEmitter() {
        RemoveListener listener = new RemoveListener();
        ReiEventBus.INSTANCE.registerListenerInstance("test", listener);
        ParticleEmittersManager.spawnEmitters(new CountingEmitter());
        ParticleEmittersManager.spawnEmitters(new CountingEmitter());

        ParticleEmittersManager.clear();

        assertEquals(2, listener.serverRemovals);
        assertEquals(0, listener.clientRemovals);
    }

    @Test
    void clearClientShouldPublishRemoveEventForEveryClientEmitter() {
        ClientLevel clientWorld = UnsafeAllocator.allocate(ClientLevel.class);
        RemoveListener listener = new RemoveListener();
        ReiEventBus.INSTANCE.registerListenerInstance("test", listener);
        CountingEmitter first = new CountingEmitter();
        CountingEmitter second = new CountingEmitter();
        first.setUuid(UUID.randomUUID());
        second.setUuid(UUID.randomUUID());
        ParticleEmittersManager.createOrChangeClient(first, clientWorld);
        ParticleEmittersManager.createOrChangeClient(second, clientWorld);

        ParticleEmittersManager.clearClient();

        assertEquals(0, listener.serverRemovals);
        assertEquals(2, listener.clientRemovals);
        assertTrue(ParticleEmittersManager.getClientEmitters().isEmpty());
    }

    private static final class CountingEmitter extends ParticleEmitters {
        private int emittedTicks;

        @Override
        protected void emitTick() {
            emittedTicks++;
        }
    }

    private static final class BlockingBindEmitter extends ParticleEmitters {
        private final CountDownLatch enteredBind;
        private final CountDownLatch releaseBind;

        private BlockingBindEmitter(CountDownLatch enteredBind, CountDownLatch releaseBind) {
            this.enteredBind = enteredBind;
            this.releaseBind = releaseBind;
        }

        @Override
        public ParticleEmitters bind(Level level, double x, double y, double z) {
            super.bind(level, x, y, z);
            enteredBind.countDown();
            try {
                if (!releaseBind.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release bind barrier");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for bind barrier", e);
            }
            return this;
        }
    }

    private static final class FailingEmitter extends ParticleEmitters {
        @Override
        protected void emitTick() {
            throw new IllegalStateException("boom");
        }
    }

    private static final class RemoveListener {
        private int serverRemovals;
        private int clientRemovals;

        @EventHandler
        public void onRemove(EmitterRemoveEvent event) {
            if (event.isClientSide()) {
                clientRemovals++;
                return;
            }
            serverRemovals++;
        }
    }
}
