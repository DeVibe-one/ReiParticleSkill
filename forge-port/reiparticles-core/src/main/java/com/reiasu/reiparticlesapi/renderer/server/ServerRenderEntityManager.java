// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.renderer.server;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.packet.PacketRenderEntityS2C;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all server-side {@link RenderEntity} instances,
 * handling visibility, tick updates, and network sync.
 */
public final class ServerRenderEntityManager {
    public static final ServerRenderEntityManager INSTANCE = new ServerRenderEntityManager();

    private final HashMap<UUID, RenderEntity> entities = new HashMap<>();
    private final HashMap<UUID, HashSet<RenderEntity>> playerViewable = new HashMap<>();

    private ServerRenderEntityManager() {}

    public HashMap<UUID, RenderEntity> getEntities() { return entities; }
    public HashMap<UUID, HashSet<RenderEntity>> getPlayerViewable() { return playerViewable; }

    public void spawn(RenderEntity entity) {
        if (entity == null) return;
        entities.put(entity.getUuid(), entity);
    }

    public void remove(RenderEntity entity) {
        if (entity == null) return;
        entities.remove(entity.getUuid());
        for (HashSet<RenderEntity> set : playerViewable.values()) {
            set.remove(entity);
        }
    }

    public void tick() {
        Iterator<Map.Entry<UUID, RenderEntity>> it = entities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, RenderEntity> entry = it.next();
            RenderEntity entity = entry.getValue();
            if (entity.getCanceled()) {
                it.remove();
                continue;
            }
            entity.tick();
        }
    }

    public void upgrade(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        List<RenderEntity> entityList = new ArrayList<>(entities.values());
        for (RenderEntity entity : entityList) {
            if (entity.getCanceled()) {
                entities.remove(entity.getUuid());
                continue;
            }

            Level world = entity.getWorld();
            if (world == null || !(world instanceof ServerLevel serverLevel)) continue;

            for (ServerPlayer player : serverLevel.players()) {
                HashSet<RenderEntity> viewSet = playerViewable.computeIfAbsent(
                        player.getUUID(), k -> new HashSet<>());

                double dist = entity.getPos().distanceTo(player.position());
                if (dist <= entity.getRenderRange()) {
                    if (!viewSet.contains(entity)) {
                        viewSet.add(entity);
                        sendSpawnPacket(player, entity);
                    } else if (entity.shouldSync()) {
                        sendSyncPacket(player, entity);
                    }
                } else {
                    if (viewSet.contains(entity)) {
                        viewSet.remove(entity);
                        sendRemovePacket(player, entity);
                    }
                }
            }
            entity.clearDirty();
        }
    }

    private void sendSpawnPacket(ServerPlayer player, RenderEntity entity) {
        PacketRenderEntityS2C packet = PacketRenderEntityS2C.ofSpawn(entity);
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private void sendSyncPacket(ServerPlayer player, RenderEntity entity) {
        PacketRenderEntityS2C packet = PacketRenderEntityS2C.ofSync(entity);
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private void sendRemovePacket(ServerPlayer player, RenderEntity entity) {
        PacketRenderEntityS2C packet = PacketRenderEntityS2C.ofRemove(entity);
        ReiParticlesNetwork.sendTo(player, packet);
    }
}
