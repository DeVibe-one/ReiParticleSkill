// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.server.dedicated.DedicatedServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerParticleGroupManagerTest {
    @AfterEach
    void cleanup() {
        ServerParticleGroupManager.INSTANCE.getGroups().clear();
        ServerParticleGroupManager.INSTANCE.getVisible().clear();
    }

    @Test
    void shouldContinueTickingOtherGroupsAfterFailure() {
        FailingGroup failing = new FailingGroup();
        CountingGroup healthy = new CountingGroup();
        ServerParticleGroupManager.INSTANCE.getGroups().put(failing.getUuid(), failing);
        ServerParticleGroupManager.INSTANCE.getGroups().put(healthy.getUuid(), healthy);

        ServerParticleGroupManager.INSTANCE.tickTrackedGroups();

        assertEquals(1, healthy.ticks);
        assertEquals(1, ServerParticleGroupManager.INSTANCE.getGroups().size());
        assertSame(healthy, ServerParticleGroupManager.INSTANCE.getGroups().get(healthy.getUuid()));
    }

    @Test
    void upgradeShouldDriveTrackedGroups() {
        CountingGroup group = new CountingGroup();
        ServerParticleGroupManager.INSTANCE.getGroups().put(group.getUuid(), group);

        ServerParticleGroupManager.INSTANCE.upgrade(UnsafeAllocator.allocate(DedicatedServer.class));

        assertEquals(1, group.ticks);
    }

    @Test
    void shouldDropStaleTrackedPlayersWhenFilteringVisiblePlayers() {
        CountingGroup group = new CountingGroup();
        UUID playerId = UUID.randomUUID();
        Set<ServerParticleGroup> tracked = ConcurrentHashMap.newKeySet();
        tracked.add(group);
        ServerParticleGroupManager.INSTANCE.getVisible().put(playerId, tracked);

        assertTrue(ServerParticleGroupManager.INSTANCE.filterVisiblePlayer(group).isEmpty());
        assertFalse(ServerParticleGroupManager.INSTANCE.getVisible().get(playerId).contains(group));
    }

    @Test
    void shouldShardVisibilityChecksByTick() {
        assertTrue(ServerParticleGroupManager.shouldProcessPlayerIndex(0, 0));
        assertFalse(ServerParticleGroupManager.shouldProcessPlayerIndex(1, 0));
        assertTrue(ServerParticleGroupManager.shouldProcessPlayerIndex(1, 1));
    }

    @Test
    void shouldIncludeAxisInCreatePacket() {
        CountingGroup group = new CountingGroup();
        group.setAxis(new RelativeLocation(1.0, 2.0, 3.0));

        PacketParticleGroupS2C packet = ServerParticleGroupManager.buildCreatePacket(group);

        assertTrue(packet.args().containsKey(PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs()));
        assertTrue(packet.args().containsKey(PacketParticleGroupS2C.PacketArgsType.GROUP_TYPE.getOfArgs()));
    }

    private static final class CountingGroup extends ServerParticleGroup {
        private int ticks;

        @Override
        public Map<String, ParticleControllerDataBuffer<?>> otherPacketArgs() {
            return Map.of();
        }

        @Override
        public Class<? extends ControllableParticleGroup> getClientType() {
            return null;
        }

        @Override
        public void onGroupDisplay(net.minecraft.world.phys.Vec3 pos, net.minecraft.server.level.ServerLevel world) {
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
            ticks++;
        }
    }

    private static final class FailingGroup extends ServerParticleGroup {
        @Override
        public Map<String, ParticleControllerDataBuffer<?>> otherPacketArgs() {
            return Map.of();
        }

        @Override
        public Class<? extends ControllableParticleGroup> getClientType() {
            return null;
        }

        @Override
        public void onGroupDisplay(net.minecraft.world.phys.Vec3 pos, net.minecraft.server.level.ServerLevel world) {
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
            throw new IllegalStateException("boom");
        }
    }
}

