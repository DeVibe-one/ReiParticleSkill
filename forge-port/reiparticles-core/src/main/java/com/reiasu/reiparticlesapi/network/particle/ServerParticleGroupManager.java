// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active {@link ServerParticleGroup} instances on the server.
 * Handles visibility tracking, tick updates, and network synchronization.
 */
public final class ServerParticleGroupManager {
    public static final ServerParticleGroupManager INSTANCE = new ServerParticleGroupManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYER_SHARD_COUNT = 4;

    private final ConcurrentHashMap<UUID, ServerParticleGroup> groups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<ServerParticleGroup>> visible = new ConcurrentHashMap<>();
    private long visibilityTick;

    private ServerParticleGroupManager() {
    }

    public ServerParticleGroup getParticleGroup(UUID uuid) {
        return groups.get(uuid);
    }

    Map<UUID, ServerParticleGroup> getGroups() {
        return groups;
    }

    Map<UUID, Set<ServerParticleGroup>> getVisible() {
        return visible;
    }

    public void addParticleGroup(ServerParticleGroup group, Vec3 pos, ServerLevel world) {
        group.initServerGroup(pos, world);
        groups.put(group.getUuid(), group);
        group.onGroupDisplay(pos, world);
    }

    public void removeParticleGroup(ServerParticleGroup group) {
        if (group == null) {
            return;
        }
        groups.remove(group.getUuid());
        for (Map.Entry<UUID, Set<ServerParticleGroup>> entry : visible.entrySet()) {
            entry.getValue().remove(group);
            if (entry.getValue().isEmpty()) {
                visible.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    public void clear() {
        for (ServerParticleGroup group : groups.values()) {
            group.setCanceled(true);
            group.setValid(false);
        }
        groups.clear();
        visible.clear();
        visibilityTick = 0L;
    }

    /**
     * Returns the set of player UUIDs that can currently see the given group.
     */
    public Set<UUID> filterVisiblePlayer(ServerParticleGroup group) {
        Set<UUID> result = new HashSet<>();
        ServerLevel level = group != null && group.getWorld() instanceof ServerLevel serverLevel ? serverLevel : null;
        for (Map.Entry<UUID, Set<ServerParticleGroup>> entry : visible.entrySet()) {
            if (!entry.getValue().contains(group)) {
                continue;
            }
            if (level == null) {
                entry.getValue().remove(group);
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && canViewGroup(group, player)) {
                result.add(entry.getKey());
                continue;
            }
            entry.getValue().remove(group);
        }
        return result;
    }

    /**
     * Called each server tick to update all groups and manage visibility.
     */
    public void upgrade(MinecraftServer server) {
        if (server == null) {
            return;
        }
        clearOfflineVisible(server);
        long tick = beginVisibilityTick();

        List<ServerParticleGroup> groupList = new ArrayList<>(groups.values());
        for (ServerParticleGroup group : groupList) {
            if (group.getCanceled() || !group.getValid()) {
                discardGroup(group, true);
                continue;
            }
            if (!(group.getWorld() instanceof ServerLevel serverLevel)) {
                continue;
            }

            List<ServerPlayer> players = serverLevel.players();
            for (int i = 0; i < players.size(); i++) {
                if (!shouldProcessPlayerIndex(i, tick)) {
                    continue;
                }
                ServerPlayer player = players.get(i);
                Set<ServerParticleGroup> visibleSet = visible.computeIfAbsent(
                        player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
                boolean shouldView = canViewGroup(group, player);
                boolean alreadyVisible = visibleSet.contains(group);

                if (shouldView && !alreadyVisible) {
                    addGroupPlayerView(player, group, visibleSet);
                    continue;
                }
                if (!shouldView && alreadyVisible) {
                    removeGroupPlayerView(player, group, visibleSet);
                }
            }
        }

        tickTrackedGroups();
        if (groups.isEmpty()) {
            visible.clear();
        }
    }

    void tickTrackedGroups() {
        List<ServerParticleGroup> groupList = new ArrayList<>(groups.values());
        for (ServerParticleGroup group : groupList) {
            if (group.getCanceled() || !group.getValid()) {
                discardGroup(group, true);
                continue;
            }
            try {
                group.tick();
            } catch (Exception e) {
                LOGGER.warn("Particle group {} ({}) failed during server tick; removing group",
                        group.getUuid(), group.getClass().getName(), e);
                group.setCanceled(true);
                group.setValid(false);
                try {
                    group.onTickAliveDeath();
                } catch (Exception callbackError) {
                    LOGGER.warn("Particle group {} ({}) failed during death callback",
                            group.getUuid(), group.getClass().getName(), callbackError);
                }
            }
            if (group.getCanceled() || !group.getValid()) {
                discardGroup(group, true);
            }
        }
    }

    static boolean shouldProcessPlayerIndex(int playerIndex, long tick) {
        return playerIndex % PLAYER_SHARD_COUNT == (int) (tick % PLAYER_SHARD_COUNT);
    }

    static boolean canViewGroup(ServerParticleGroup group, ServerPlayer player) {
        if (group == null || player == null || group.getWorld() == null || group.getPos() == null) {
            return false;
        }
        if (player.isRemoved() || player.isSpectator()) {
            return false;
        }
        if (group.getWorld() != player.level()) {
            return false;
        }
        return group.getPos().distanceTo(player.position()) <= group.getVisibleRange();
    }

    static PacketParticleGroupS2C buildCreatePacket(ServerParticleGroup targetGroup) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(targetGroup.getPos()));
        args.put(PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(targetGroup.getAxis().toVector()));

        Class<? extends ControllableParticleGroup> clientType = targetGroup.getClientType();
        String typeName = clientType != null ? clientType.getName() : "";
        args.put(PacketParticleGroupS2C.PacketArgsType.GROUP_TYPE.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.string(typeName));

        args.put(PacketParticleGroupS2C.PacketArgsType.CURRENT_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(targetGroup.getClientTick()));
        args.put(PacketParticleGroupS2C.PacketArgsType.MAX_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(targetGroup.getClientMaxTick()));
        args.put(PacketParticleGroupS2C.PacketArgsType.SCALE.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.doubleValue(targetGroup.getScale()));
        args.putAll(targetGroup.otherPacketArgs());

        return new PacketParticleGroupS2C(targetGroup.getUuid(), ControlType.CREATE, args);
    }

    private long beginVisibilityTick() {
        return visibilityTick++;
    }

    private void clearOfflineVisible(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Set<ServerParticleGroup>>> it = visible.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<ServerParticleGroup>> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.hasDisconnected()) {
                it.remove();
            }
        }
    }

    private void discardGroup(ServerParticleGroup group, boolean notifyClients) {
        if (group == null) {
            return;
        }
        if (notifyClients && group.getWorld() instanceof ServerLevel serverLevel) {
            PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                    group.getUuid(), ControlType.REMOVE, Map.of());
            for (UUID playerId : filterVisiblePlayer(group)) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    ReiParticlesNetwork.sendTo(player, packet);
                }
            }
        }
        removeParticleGroup(group);
    }

    private void removeGroupPlayerView(ServerPlayer target, ServerParticleGroup targetGroup, Set<ServerParticleGroup> visibleSet) {
        visibleSet.remove(targetGroup);
        PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                targetGroup.getUuid(), ControlType.REMOVE, new HashMap<>());
        ReiParticlesNetwork.sendTo(target, packet);
    }

    private void addGroupPlayerView(ServerPlayer target, ServerParticleGroup targetGroup, Set<ServerParticleGroup> visibleSet) {
        PacketParticleGroupS2C packet = buildCreatePacket(targetGroup);
        if (!trySendPacket(target, packet)) {
            return;
        }
        visibleSet.add(targetGroup);
    }

    private boolean trySendPacket(ServerPlayer target, PacketParticleGroupS2C packet) {
        if (!ServerSyncPacketBudget.tryAcquire()) {
            return false;
        }
        ReiParticlesNetwork.sendTo(target, packet);
        return true;
    }
}

