// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network;

import com.reiasu.reiparticlesapi.animation.Animate;
import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.animation.AnimateManager;
import com.reiasu.reiparticlesapi.animation.AnimateNode;
import com.reiasu.reiparticlesapi.display.DebugDisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.particle.ServerParticleGroup;
import com.reiasu.reiparticlesapi.network.particle.ServerParticleGroupManager;
import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle.StyleData;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import com.reiasu.reiparticlesapi.renderer.server.ServerRenderEntityManager;
import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import com.reiasu.reiparticlesapi.test.TestManager;
import com.reiasu.reiparticlesapi.test.api.TestGroup;
import com.reiasu.reiparticlesapi.test.api.TestOption;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRuntimeStateResetTest {
    @AfterEach
    void cleanup() {
        AnimateManager.INSTANCE.clear();
        ParticleEmittersManager.clear();
        DisplayEntityManager.INSTANCE.clear();
        ParticleCompositionManager.INSTANCE.clear();
        ParticleStyleManager.clear();
        ServerParticleGroupManager.INSTANCE.clear();
        ServerRenderEntityManager.INSTANCE.clear();
        ReiScheduler.INSTANCE.clear();
        TestManager.INSTANCE.clear();
        ServerSyncPacketBudget.reset();
    }

    @Test
    void resetShouldClearTrackedServerRuntimeState() {
        AtomicInteger serverScheduledRuns = new AtomicInteger();
        AnimateManager.INSTANCE.displayAnimateServer(new Animate().addNode(new AnimateNode().addAction(new BlockingAnimateAction())));
        ParticleEmittersManager.spawnEmitters(new CountingEmitter());
        DisplayEntityManager.INSTANCE.spawn(new DebugDisplayEntity(0.0, 64.0, 0.0, "group"));
        ParticleCompositionManager.INSTANCE.spawn(new CountingComposition());

        CountingStyle serverStyle = new CountingStyle();
        ParticleStyleManager.getServerViewStyles().put(serverStyle.getUuid(), serverStyle);
        Set<UUID> trackedStyles = ConcurrentHashMap.newKeySet();
        trackedStyles.add(serverStyle.getUuid());
        ParticleStyleManager.getVisible().put(UUID.randomUUID(), trackedStyles);

        ServerLevel serverLevel = UnsafeAllocator.allocate(ServerLevel.class);
        CountingServerGroup serverGroup = new CountingServerGroup();
        ServerParticleGroupManager.INSTANCE.addParticleGroup(serverGroup, Vec3.ZERO, serverLevel);

        ServerRenderEntityManager.INSTANCE.spawn(new CountingRenderEntity());
        ReiScheduler.INSTANCE.runTask(1, serverScheduledRuns::incrementAndGet);
        TestManager.INSTANCE.getValidGroupsServer().add(new StubTestGroup("server"));
        ServerSyncPacketBudget.beginServerTick(System.nanoTime());
        assertTrue(ServerSyncPacketBudget.tryAcquire());

        ServerRuntimeStateReset.reset();

        assertEquals(0, AnimateManager.INSTANCE.activeCount());
        assertEquals(0, ParticleEmittersManager.activeCount());
        assertEquals(0, DisplayEntityManager.INSTANCE.activeCount());
        assertEquals(0, ParticleCompositionManager.INSTANCE.activeCount());
        assertTrue(ParticleStyleManager.getServerViewStyles().isEmpty());
        assertTrue(ParticleStyleManager.getVisible().isEmpty());
        assertNull(ServerParticleGroupManager.INSTANCE.getParticleGroup(serverGroup.getUuid()));
        assertTrue(ServerRenderEntityManager.INSTANCE.getEntities().isEmpty());
        assertTrue(TestManager.INSTANCE.getValidGroupsServer().isEmpty());
        assertEquals(0, ServerSyncPacketBudget.usedPackets());

        ReiScheduler.INSTANCE.doServerTick();
        assertEquals(0, serverScheduledRuns.get());
    }

    @Test
    void resetShouldPreserveClientState() {
        AtomicInteger clientScheduledRuns = new AtomicInteger();
        AnimateManager.INSTANCE.displayAnimateClient(new Animate().addNode(new AnimateNode().addAction(new BlockingAnimateAction())));

        ClientLevel clientLevel = UnsafeAllocator.allocate(ClientLevel.class);
        CountingEmitter clientEmitter = new CountingEmitter();
        clientEmitter.setUuid(UUID.randomUUID());
        ParticleEmittersManager.createOrChangeClient(clientEmitter, clientLevel);
        DisplayEntityManager.INSTANCE.addClient(new DebugDisplayEntity(0.0, 64.0, 0.0, "group"));
        ParticleCompositionManager.INSTANCE.addClient(new CountingComposition());
        CountingStyle clientStyle = new CountingStyle();
        ParticleStyleManager.getClientViewStyles().put(clientStyle.getUuid(), clientStyle);
        ReiScheduler.INSTANCE.runClientTask(1, clientScheduledRuns::incrementAndGet);
        TestManager.INSTANCE.getValidGroupsClient().add(new StubTestGroup("client"));

        ServerRuntimeStateReset.reset();

        assertEquals(1, AnimateManager.INSTANCE.clientActiveCount());
        assertEquals(1, ParticleEmittersManager.getClientEmitters().size());
        assertEquals(1, DisplayEntityManager.INSTANCE.getClientView().size());
        assertEquals(1, ParticleCompositionManager.INSTANCE.getClientView().size());
        assertEquals(1, ParticleStyleManager.getClientViewStyles().size());
        assertEquals(1, TestManager.INSTANCE.getValidGroupsClient().size());

        ReiScheduler.INSTANCE.doClientTick();
        assertEquals(1, clientScheduledRuns.get());
    }

    private static final class BlockingAnimateAction extends AnimateAction {
        @Override
        public boolean checkDone() {
            return false;
        }

        @Override
        public void tick() {
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onDone() {
        }
    }

    private static final class CountingEmitter extends ParticleEmitters {
        @Override
        protected void emitTick() {
        }
    }

    private static final class CountingComposition extends ParticleComposition {
        @Override
        public Map<CompositionData, RelativeLocation> getParticles() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }
    }

    private static final class CountingStyle extends ParticleGroupStyle {
        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }
    }

    private static final class CountingServerGroup extends ServerParticleGroup {
        @Override
        public Map<String, ParticleControllerDataBuffer<?>> otherPacketArgs() {
            return Map.of();
        }

        @Override
        public Class<? extends ControllableParticleGroup> getClientType() {
            return null;
        }

        @Override
        public void onGroupDisplay(Vec3 pos, ServerLevel world) {
        }

        @Override
        public void onTickAliveDeath() {
        }

        @Override
        public void onClientViewDeath() {
        }

        @Override
        public void doTickClient() {
        }

        @Override
        public void doTickAlive() {
        }
    }

    private static final class CountingRenderEntity extends RenderEntity {
        @Override
        public void clientTick() {
        }

        @Override
        public void serverTick() {
        }

        @Override
        public ResourceLocation getRenderID() {
            return new ResourceLocation("test", "render");
        }
    }

    private static final class StubTestGroup implements TestGroup {
        private final String id;

        private StubTestGroup(String id) {
            this.id = id;
        }

        @Override
        public ServerPlayer getUser() {
            return null;
        }

        @Override
        public TestGroup appendOption(Supplier<TestOption> supplier) {
            return this;
        }

        @Override
        public void init() {
        }

        @Override
        public void start() {
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public TestOption skipCurrent() {
            return null;
        }

        @Override
        public void doTick() {
        }

        @Override
        public void onOptionFailure(Throwable t, TestOption option) {
        }

        @Override
        public void onOptionSuccess(TestOption option) {
        }

        @Override
        public void onGroupFinished() {
        }

        @Override
        public String groupID() {
            return id;
        }
    }
}
