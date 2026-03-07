// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.config.APIConfig;
import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleEmittersS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class ParticleEmitterVisibilityTracker {
    private static final int PLAYER_SHARD_COUNT = 4;

    private final Map<UUID, Set<UUID>> visible = new ConcurrentHashMap<>();
    private final AtomicInteger packetsThisTick = new AtomicInteger(0);
    private long visibilityTick;
    private int statSynced;
    private int statSkippedLod;
    private int statSkippedShard;
    private int statThrottled;
    private volatile int[] lastTickStats = new int[4];

    long beginTick() {
        lastTickStats = new int[]{statSynced, statSkippedLod, statSkippedShard, statThrottled};
        statSynced = 0;
        statSkippedLod = 0;
        statSkippedShard = 0;
        statThrottled = 0;
        packetsThisTick.set(0);
        return visibilityTick++;
    }

    int[] getLastTickStats() {
        return lastTickStats.clone();
    }

    int trackedPlayerCount() {
        return visible.size();
    }

    void updateClientVisible(ParticleEmitters emitters, long tick) {
        if (!(emitters.level() instanceof ServerLevel level)) {
            return;
        }

        List<ServerPlayer> players = level.players();
        for (int i = 0; i < players.size(); i++) {
            if (i % PLAYER_SHARD_COUNT != (int) (tick % PLAYER_SHARD_COUNT)) {
                statSkippedShard++;
                continue;
            }
            ServerPlayer player = players.get(i);
            Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            boolean shouldView = canViewEmitter(emitters, player);
            boolean isViewing = visibleSet.contains(emitters.getUuid());

            if (shouldView && !isViewing) {
                addView(player, emitters);
                continue;
            }
            if (!shouldView && isViewing) {
                removeView(player, emitters);
                continue;
            }
            if (shouldView) {
                double dist = player.position().distanceTo(emitters.position());
                int lodInterval = computeLodInterval(dist, emitters.getVisibleRange());
                if (lodInterval > 1 && (emitters.getTick() % lodInterval) != 0) {
                    statSkippedLod++;
                    continue;
                }
                sendChange(emitters, player);
            }
        }
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

    static boolean canViewEmitter(ParticleEmitters emitters, ServerPlayer player) {
        if (emitters.level() == null || player == null) {
            return false;
        }
        if (player.isRemoved() || player.isSpectator()) {
            return false;
        }
        if (emitters.level() != player.level()) {
            return false;
        }
        double range = Math.min(emitters.getVisibleRange(), APIConfig.INSTANCE.getMaxEmitterVisibleRange());
        return player.position().distanceTo(emitters.position()) <= range;
    }

    void removeAllViews(ParticleEmitters emitters) {
        ServerLevel level = emitters.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        for (Map.Entry<UUID, Set<UUID>> entry : visible.entrySet()) {
            Set<UUID> visibleSet = entry.getValue();
            if (!visibleSet.remove(emitters.getUuid()) || level == null) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            ResourceLocation key = emitters.getEmittersID();
            if (key == null || EmitterRegistry.INSTANCE.getDecoder(key) == null) {
                continue;
            }
            PacketParticleEmittersS2C packet = new PacketParticleEmittersS2C(
                    key,
                    emitters.encodeToBytes(),
                    PacketParticleEmittersS2C.PacketType.REMOVE
            );
            ReiParticlesNetwork.sendTo(player, packet);
        }
    }

    void pruneDisconnectedPlayers(List<ParticleEmitters> emitters) {
        if (emitters.isEmpty()) {
            visible.clear();
            return;
        }
        net.minecraft.server.MinecraftServer server = null;
        for (ParticleEmitters emitter : emitters) {
            if (emitter.level() instanceof ServerLevel serverLevel) {
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
        visibilityTick = 0;
        statSynced = 0;
        statSkippedLod = 0;
        statSkippedShard = 0;
        statThrottled = 0;
        lastTickStats = new int[4];
        packetsThisTick.set(0);
    }

    private void sendChange(ParticleEmitters emitters, ServerPlayer player) {
        if (packetsThisTick.incrementAndGet() > APIConfig.INSTANCE.getPacketsPerTickLimit()) {
            statThrottled++;
            return;
        }
        statSynced++;
        ResourceLocation key = emitters.getEmittersID();
        if (key == null || EmitterRegistry.INSTANCE.getDecoder(key) == null) {
            return;
        }
        PacketParticleEmittersS2C packet = new PacketParticleEmittersS2C(
                key,
                emitters.encodeToBytes(),
                PacketParticleEmittersS2C.PacketType.CHANGE_OR_CREATE
        );
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private void addView(ServerPlayer player, ParticleEmitters emitters) {
        Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
        if (!visibleSet.add(emitters.getUuid())) {
            return;
        }
        sendChange(emitters, player);
    }

    private void removeView(ServerPlayer player, ParticleEmitters emitters) {
        Set<UUID> visibleSet = visible.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
        visibleSet.remove(emitters.getUuid());
        ResourceLocation key = emitters.getEmittersID();
        if (key == null || EmitterRegistry.INSTANCE.getDecoder(key) == null) {
            return;
        }
        PacketParticleEmittersS2C packet = new PacketParticleEmittersS2C(
                key,
                emitters.encodeToBytes(),
                PacketParticleEmittersS2C.PacketType.REMOVE
        );
        ReiParticlesNetwork.sendTo(player, packet);
    }
}
