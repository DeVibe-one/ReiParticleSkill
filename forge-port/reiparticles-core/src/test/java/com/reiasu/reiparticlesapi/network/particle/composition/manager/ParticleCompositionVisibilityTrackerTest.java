// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition.manager;

import com.reiasu.reiparticlesapi.config.APIConfig;
import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParticleCompositionVisibilityTrackerTest {
    @AfterEach
    void cleanup() {
        APIConfig.INSTANCE.setPacketsPerTickLimit(512);
        ReiParticlesNetwork.bindSender((player, packet) -> {
        });
    }

    @Test
    void computeLodIntervalScalesWithViewerDistance() {
        assertEquals(1, ParticleCompositionVisibilityTracker.computeLodInterval(5.0, 100.0));
        assertEquals(3, ParticleCompositionVisibilityTracker.computeLodInterval(30.0, 100.0));
        assertEquals(6, ParticleCompositionVisibilityTracker.computeLodInterval(60.0, 100.0));
        assertEquals(12, ParticleCompositionVisibilityTracker.computeLodInterval(90.0, 100.0));
    }

    @Test
    void shouldShardPlayerProcessingByTick() {
        assertTrue(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(0, 0));
        assertFalse(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(1, 0));
        assertTrue(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(1, 1));
        assertFalse(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(3, 1));
    }

    @Test
    void syncSpawnVisibleShouldImmediatelySendCreateToVisiblePlayers() {
        Map<UUID, Set<UUID>> visible = new ConcurrentHashMap<>();
        ParticleCompositionVisibilityTracker tracker = new ParticleCompositionVisibilityTracker(
                visible,
                (composition, remove) -> new PacketParticleCompositionS2C(composition.getControlUUID(), "test", new byte[0]));
        ServerLevel level = allocateLevel();
        TestServerPlayer near = TestServerPlayer.allocate(level, new Vec3(2.0, 0.0, 0.0), false);
        TestServerPlayer far = TestServerPlayer.allocate(level, new Vec3(32.0, 0.0, 0.0), false);
        setPlayers(level, List.of(near, far));
        CountingComposition composition = new CountingComposition();
        composition.setWorld(level);
        composition.setPosition(Vec3.ZERO);
        composition.setVisibleRange(8.0);
        AtomicInteger sends = new AtomicInteger();
        ReiParticlesNetwork.bindSender((player, packet) -> sends.incrementAndGet());

        tracker.syncSpawnVisible(composition, level);

        assertEquals(1, sends.get());
        assertTrue(visible.getOrDefault(near.getUUID(), Set.of()).contains(composition.getControlUUID()));
        assertFalse(visible.getOrDefault(far.getUUID(), Set.of()).contains(composition.getControlUUID()));
    }

    @Test
    void shouldOnlyTrackCompositionAfterSuccessfulSend() {
        Set<UUID> visible = new HashSet<>();
        UUID compositionId = UUID.randomUUID();

        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> false));
        assertFalse(visible.contains(compositionId));
        assertTrue(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> true));
        assertTrue(visible.contains(compositionId));

        AtomicInteger sendAttempts = new AtomicInteger();
        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> {
            sendAttempts.incrementAndGet();
            return true;
        }));
        assertEquals(0, sendAttempts.get());
    }

    @Test
    void shouldRespectSharedPacketBudgetWhenTrackingNewComposition() {
        Set<UUID> visible = new HashSet<>();
        APIConfig.INSTANCE.setPacketsPerTickLimit(16);
        ServerSyncPacketBudget.beginServerTick(System.nanoTime());

        for (int i = 0; i < 16; i++) {
            UUID compositionId = UUID.randomUUID();
            assertTrue(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(
                    visible,
                    compositionId,
                    ServerSyncPacketBudget::tryAcquire));
        }

        UUID throttledCompositionId = UUID.randomUUID();
        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(
                visible,
                throttledCompositionId,
                ServerSyncPacketBudget::tryAcquire));
        assertFalse(visible.contains(throttledCompositionId));
        assertEquals(16, visible.size());
    }

    private static ServerLevel allocateLevel() {
        ServerLevel level = UnsafeAllocator.allocate(ServerLevel.class);
        setField(Level.class, level, "levelData", createLevelData(37L));
        return level;
    }

    private static void setPlayers(ServerLevel level, List<ServerPlayer> players) {
        setField(ServerLevel.class, level, "players", new ArrayList<>(players));
    }

    private static WritableLevelData createLevelData(long gameTime) {
        GameRules gameRules = new GameRules();
        return (WritableLevelData) Proxy.newProxyInstance(
                WritableLevelData.class.getClassLoader(),
                new Class<?>[]{WritableLevelData.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getGameTime", "getDayTime" -> gameTime;
                    case "getXSpawn", "getYSpawn", "getZSpawn" -> 0;
                    case "getSpawnAngle" -> 0.0F;
                    case "isThundering", "isRaining", "isHardcore", "isDifficultyLocked" -> false;
                    case "getGameRules" -> gameRules;
                    case "getDifficulty" -> Difficulty.NORMAL;
                    case "setRaining", "setXSpawn", "setYSpawn", "setZSpawn", "setSpawnAngle", "setSpawn" -> null;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "TestWritableLevelData";
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });
    }

    private static void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
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

    private static final class TestServerPlayer extends ServerPlayer {
        private UUID testUuid;
        private Vec3 testPosition;
        private Level testLevel;
        private boolean spectator;

        private TestServerPlayer() {
            super(null, null, null);
        }

        static TestServerPlayer allocate(Level level, Vec3 position, boolean spectator) {
            TestServerPlayer player = UnsafeAllocator.allocate(TestServerPlayer.class);
            player.testUuid = UUID.randomUUID();
            player.testPosition = position;
            player.testLevel = level;
            player.spectator = spectator;
            return player;
        }

        @Override
        public UUID getUUID() {
            return testUuid;
        }

        @Override
        public Vec3 position() {
            return testPosition;
        }

        @Override
        public Level level() {
            return testLevel;
        }

        @Override
        public boolean isSpectator() {
            return spectator;
        }
    }
}
