// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleStyleS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

final class ParticleStyleVisibilityTracker {
    private static final int PLAYER_SHARD_COUNT = 4;

    private final Map<UUID, Set<UUID>> visible;
    private long visibilityTick;
    private int statSynced;
    private int statSkippedLod;
    private int statSkippedShard;
    private int statThrottled;
    private volatile int[] lastTickStats = new int[4];

    ParticleStyleVisibilityTracker(Map<UUID, Set<UUID>> visible) {
        this.visible = visible;
    }

    long beginTick() {
        lastTickStats = new int[]{statSynced, statSkippedLod, statSkippedShard, statThrottled};
        statSynced = 0;
        statSkippedLod = 0;
        statSkippedShard = 0;
        statThrottled = 0;
        return visibilityTick++;
    }

    void syncSpawnVisible(ParticleGroupStyle style, ServerLevel level) {
        beginSharedBudget(level);
        for (ServerPlayer player : level.players()) {
            Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            if (canViewStyle(style, player) && !visibleSet.contains(style.getUuid())) {
                addView(player, style, visibleSet);
            }
        }
    }

    void updateClientVisible(ParticleGroupStyle style,
                             ServerLevel level,
                             long tick,
                             PacketParticleStyleS2C dirtyPacket) {
        beginSharedBudget(level);
        java.util.List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            if (!shouldProcessPlayerIndex(i, tick)) {
                statSkippedShard++;
                continue;
            }
            ServerPlayer player = players.get(i);
            Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            boolean shouldView = canViewStyle(style, player);
            boolean alreadyView = visibleSet.contains(style.getUuid());

            if (shouldView && !alreadyView) {
                addView(player, style, visibleSet);
                continue;
            }
            if (!shouldView && alreadyView) {
                removeView(player, style, visibleSet);
                continue;
            }
            if (shouldView && dirtyPacket != null) {
                double dist = player.position().distanceTo(style.getPos());
                int lodInterval = computeLodInterval(dist, style.getVisibleRange());
                if (lodInterval > 1 && (tick % lodInterval) != 0) {
                    statSkippedLod++;
                    continue;
                }
                sendPacket(player, dirtyPacket);
            }
        }
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

    static boolean canViewStyle(ParticleGroupStyle style, ServerPlayer player) {
        if (style.getWorld() == null || player == null) {
            return false;
        }
        if (player.isRemoved() || player.isSpectator()) {
            return false;
        }
        if (style.getWorld() != player.level()) {
            return false;
        }
        return style.getPos().distanceTo(player.position()) <= style.getVisibleRange();
    }

    static boolean markVisibleAfterSuccessfulSend(Set<UUID> visibleSet, UUID styleId, BooleanSupplier sendAction) {
        if (visibleSet.contains(styleId)) {
            return false;
        }
        if (!sendAction.getAsBoolean()) {
            return false;
        }
        return visibleSet.add(styleId);
    }

    private void addView(ServerPlayer player, ParticleGroupStyle style, Set<UUID> visibleSet) {
        markVisibleAfterSuccessfulSend(visibleSet, style.getUuid(),
                () -> sendPacket(player, ParticleStyleManager.buildCreatePacket(style, style.getPos())));
    }

    private void removeView(ServerPlayer player, ParticleGroupStyle style, Set<UUID> visibleSet) {
        visibleSet.remove(style.getUuid());
        ReiParticlesNetwork.sendTo(player, new PacketParticleStyleS2C(style.getUuid(), ControlType.REMOVE, Map.of()));
    }

    private boolean sendPacket(ServerPlayer player, PacketParticleStyleS2C packet) {
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
