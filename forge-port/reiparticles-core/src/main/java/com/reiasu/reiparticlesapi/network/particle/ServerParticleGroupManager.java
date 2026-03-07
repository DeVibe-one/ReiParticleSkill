// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active {@link ServerParticleGroup} instances on the server.
 * Handles visibility tracking, tick updates, and network synchronization.
 */
public final class ServerParticleGroupManager {
    public static final ServerParticleGroupManager INSTANCE = new ServerParticleGroupManager();

    private final ConcurrentHashMap<UUID, ServerParticleGroup> groups = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<ServerParticleGroup>> visible = new ConcurrentHashMap<>();

    private ServerParticleGroupManager() {}

    public ServerParticleGroup getParticleGroup(UUID uuid) {
        return groups.get(uuid);
    }

    public void addParticleGroup(ServerParticleGroup group, Vec3 pos, ServerLevel world) {
        group.initServerGroup(pos, world);
        groups.put(group.getUuid(), group);
        group.onGroupDisplay(pos, world);
    }

    public void removeParticleGroup(ServerParticleGroup group) {
        groups.remove(group.getUuid());
        // Clean up visibility entries
        for (Map.Entry<UUID, Set<ServerParticleGroup>> entry : visible.entrySet()) {
            entry.getValue().remove(group);
        }
    }

    /**
     * Returns the set of player UUIDs that can currently see the given group.
     */
    public Set<UUID> filterVisiblePlayer(ServerParticleGroup group) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<ServerParticleGroup>> entry : visible.entrySet()) {
            if (entry.getValue().contains(group)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Called each server tick to update all groups and manage visibility.
     */
    public void upgrade(MinecraftServer server) {
        if (server == null) return;
        clearOfflineVisible(server);

        List<ServerParticleGroup> groupList = new ArrayList<>(groups.values());
        for (ServerParticleGroup group : groupList) {
            if (group.getCanceled() || !group.getValid()) {
                groups.remove(group.getUuid());
                continue;
            }

            if (group.getWorld() == null) continue;
            if (!(group.getWorld() instanceof ServerLevel serverLevel)) continue;

            List<ServerPlayer> players = serverLevel.players();
            for (ServerPlayer player : players) {
                Set<ServerParticleGroup> visibleSet = visible.computeIfAbsent(
                        player.getUUID(), k -> ConcurrentHashMap.newKeySet());

                // Different world
                if (!player.level().equals(group.getWorld())) {
                    if (visibleSet.contains(group)) {
                        removeGroupPlayerView(player, group);
                        visibleSet.remove(group);
                    }
                    continue;
                }

                // Player is dead/spectator
                if (player.isRemoved()) {
                    if (visibleSet.contains(group)) {
                        removeGroupPlayerView(player, group);
                        visibleSet.remove(group);
                    }
                    continue;
                }

                // Within visible range
                if (group.getPos().distanceTo(player.position()) <= group.getVisibleRange()) {
                    if (!visibleSet.contains(group)) {
                        addGroupPlayerView(player, group);
                        togglePacketView(player, group);
                    }
                } else {
                    if (visibleSet.contains(group)) {
                        removeGroupPlayerView(player, group);
                    }
                    visibleSet.remove(group);
                }
            }

            group.tick();
        }
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

    private void togglePacketView(ServerPlayer target, ServerParticleGroup group) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(group.getPos()));
        args.put(PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(group.getAxis().toVector()));
        args.put(PacketParticleGroupS2C.PacketArgsType.CURRENT_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(group.getClientTick()));
        args.put(PacketParticleGroupS2C.PacketArgsType.MAX_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(group.getClientMaxTick()));

        PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                group.getUuid(), ControlType.CHANGE, args);
        ReiParticlesNetwork.sendTo(target, packet);
    }

    private void removeGroupPlayerView(ServerPlayer target, ServerParticleGroup targetGroup) {
        PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                targetGroup.getUuid(), ControlType.REMOVE, new HashMap<>());
        ReiParticlesNetwork.sendTo(target, packet);
    }

    private void addGroupPlayerView(ServerPlayer target, ServerParticleGroup targetGroup) {
        Set<ServerParticleGroup> visibleSet = visible.computeIfAbsent(
                target.getUUID(), k -> ConcurrentHashMap.newKeySet());
        visibleSet.add(targetGroup);

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(targetGroup.getPos()));

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

        PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                targetGroup.getUuid(), ControlType.CREATE, args);
        ReiParticlesNetwork.sendTo(target, packet);
    }
}
