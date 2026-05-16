// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition.manager;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

final class ParticleCompositionVisibilityTracker {
    private static final int PLAYER_SHARD_COUNT = 4;

    private final Map<UUID, Set<UUID>> visible;
    private final BiFunction<ParticleComposition, Boolean, PacketParticleCompositionS2C> packetFactory;
    private long visibilityTick;
    private int statSynced;
    private int statSkippedLod;
    private int statSkippedShard;
    private int statThrottled;
    private volatile int[] lastTickStats = new int[4];

    ParticleCompositionVisibilityTracker(
            Map<UUID, Set<UUID>> visible,
            BiFunction<ParticleComposition, Boolean, PacketParticleCompositionS2C> packetFactory
    ) {
        this.visible = visible;
        this.packetFactory = packetFactory;
    }

    long beginTick() {
        lastTickStats = new int[]{statSynced, statSkippedLod, statSkippedShard, statThrottled};
        statSynced = 0;
        statSkippedLod = 0;
        statSkippedShard = 0;
        statThrottled = 0;
        return visibilityTick++;
    }

    void syncSpawnVisible(ParticleComposition composition, ServerLevel level) {
        beginSharedBudget(level);
        for (ServerPlayer player : level.players()) {
            Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            if (canViewComposition(composition, player) && !visibleSet.contains(composition.getControlUUID())) {
                addView(player, composition, visibleSet);
            }
        }
    }

    void updateClientVisible(ParticleComposition composition,
                             ServerLevel level,
                             long tick,
                             PacketParticleCompositionS2C dirtyPacket) {
        beginSharedBudget(level);
        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            if (!shouldProcessPlayerIndex(i, tick)) {
                statSkippedShard++;
                continue;
            }
            ServerPlayer player = players.get(i);
            Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            boolean shouldView = canViewComposition(composition, player);
            boolean alreadyVisible = visibleSet.contains(composition.getControlUUID());

            if (shouldView && !alreadyVisible) {
                addView(player, composition, visibleSet);
                continue;
            }
            if (!shouldView && alreadyVisible) {
                removeView(player, composition, visibleSet);
                continue;
            }
            if (shouldView && dirtyPacket != null) {
                double dist = Math.sqrt(player.position().distanceToSqr(composition.getPosition()));
                int lodInterval = computeLodInterval(dist, composition.getVisibleRange());
                if (lodInterval > 1 && (tick % lodInterval) != 0) {
                    statSkippedLod++;
                    continue;
                }
                sendPacket(player, dirtyPacket);
            }
        }
    }

    void removeAllViews(ParticleComposition composition, ServerLevel level) {
        UUID compositionId = composition.getControlUUID();
        PacketParticleCompositionS2C removePacket = packetFactory.apply(composition, true);
        for (Map.Entry<UUID, Set<UUID>> entry : visible.entrySet()) {
            if (!entry.getValue().remove(compositionId) || level == null || removePacket == null) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                ReiParticlesNetwork.sendTo(player, removePacket);
            }
        }
    }

    void pruneDisconnectedPlayers(Collection<ParticleComposition> compositions) {
        if (compositions.isEmpty()) {
            clear();
            return;
        }
        net.minecraft.server.MinecraftServer server = null;
        for (ParticleComposition composition : compositions) {
            if (composition.getWorld() instanceof ServerLevel serverLevel) {
                server = serverLevel.getServer();
                break;
            }
        }
        if (server == null) {
            return;
        }
        net.minecraft.server.MinecraftServer runtime = server;
        visible.entrySet().removeIf(entry -> runtime.getPlayerList().getPlayer(entry.getKey()) == null);
    }

    void clear() {
        visible.clear();
        visibilityTick = 0L;
        statSynced = 0;
        statSkippedLod = 0;
        statSkippedShard = 0;
        statThrottled = 0;
        lastTickStats = new int[4];
    }

    static boolean shouldProcessPlayerIndex(int playerIndex, long tick) {
        return playerIndex % PLAYER_SHARD_COUNT == (int) (tick % PLAYER_SHARD_COUNT);
    }

    static int computeLodInterval(double distance, double visibleRange) {
        double ratio = distance / Math.max(1.0, visibleRange);
        if (ratio < 0.25) {
            return 1;
        }
        if (ratio < 0.50) {
            return 3;
        }
        if (ratio < 0.75) {
            return 6;
        }
        return 12;
    }

    static boolean canViewComposition(ParticleComposition composition, ServerPlayer player) {
        if (composition == null || player == null || composition.getWorld() == null) {
            return false;
        }
        if (player.isRemoved() || player.isSpectator()) {
            return false;
        }
        if (composition.getWorld() != player.level()) {
            return false;
        }
        double visibleRange = Math.max(0.0, composition.getVisibleRange());
        return composition.getPosition().distanceToSqr(player.position()) <= visibleRange * visibleRange;
    }

    static boolean markVisibleAfterSuccessfulSend(Set<UUID> visibleSet,
                                                  UUID compositionId,
                                                  BooleanSupplier sendAction) {
        if (visibleSet.contains(compositionId)) {
            return false;
        }
        if (!sendAction.getAsBoolean()) {
            return false;
        }
        return visibleSet.add(compositionId);
    }

    private void addView(ServerPlayer player, ParticleComposition composition, Set<UUID> visibleSet) {
        markVisibleAfterSuccessfulSend(visibleSet, composition.getControlUUID(),
                () -> sendPacket(player, packetFactory.apply(composition, false)));
    }

    private void removeView(ServerPlayer player, ParticleComposition composition, Set<UUID> visibleSet) {
        visibleSet.remove(composition.getControlUUID());
        PacketParticleCompositionS2C removePacket = packetFactory.apply(composition, true);
        if (removePacket != null) {
            ReiParticlesNetwork.sendTo(player, removePacket);
        }
    }

    private boolean sendPacket(ServerPlayer player, PacketParticleCompositionS2C packet) {
        if (packet == null) {
            return false;
        }
        if (!ServerSyncPacketBudget.tryAcquire()) {
            statThrottled++;
            return false;
        }
        statSynced++;
        ReiParticlesNetwork.sendTo(player, packet);
        return true;
    }

    private static void beginSharedBudget(ServerLevel level) {
        if (level.getServer() != null) {
            ServerSyncPacketBudget.beginServerTick(level.getServer().getTickCount());
            return;
        }
        ServerSyncPacketBudget.beginServerTick(level.getGameTime());
    }
}
