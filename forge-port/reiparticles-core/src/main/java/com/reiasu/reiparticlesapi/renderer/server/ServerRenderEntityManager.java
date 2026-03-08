// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.renderer.server;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.packet.PacketRenderEntityS2C;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all server-side {@link RenderEntity} instances,
 * handling visibility, tick updates, and network sync.
 */
public final class ServerRenderEntityManager {
    public static final ServerRenderEntityManager INSTANCE = new ServerRenderEntityManager();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYER_SHARD_COUNT = 4;

    private final HashMap<UUID, RenderEntity> entities = new HashMap<>();
    private final HashMap<UUID, HashSet<RenderEntity>> playerViewable = new HashMap<>();
    private long visibilityTick;

    private ServerRenderEntityManager() {
    }

    public HashMap<UUID, RenderEntity> getEntities() {
        return entities;
    }

    public HashMap<UUID, HashSet<RenderEntity>> getPlayerViewable() {
        return playerViewable;
    }

    public void spawn(RenderEntity entity) {
        if (entity == null) {
            return;
        }
        entities.put(entity.getUuid(), entity);
    }

    public void remove(RenderEntity entity) {
        if (entity == null) {
            return;
        }
        entities.remove(entity.getUuid());
        clearTrackedView(entity, true);
    }

    public void clear() {
        for (RenderEntity entity : new ArrayList<>(entities.values())) {
            entity.setCanceled(true);
            clearTrackedView(entity, false);
        }
        entities.clear();
        playerViewable.clear();
        visibilityTick = 0L;
    }

    public void tick() {
        Iterator<Map.Entry<UUID, RenderEntity>> it = entities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, RenderEntity> entry = it.next();
            RenderEntity entity = entry.getValue();
            if (entity.getCanceled()) {
                clearTrackedView(entity, true);
                it.remove();
                continue;
            }
            try {
                entity.tick();
            } catch (Exception e) {
                LOGGER.warn("Render entity {} ({}) failed during server tick; removing entity",
                        entity.getUuid(), entity.getClass().getName(), e);
                entity.setCanceled(true);
            }
            if (entity.getCanceled()) {
                clearTrackedView(entity, true);
                it.remove();
            }
        }
        if (entities.isEmpty()) {
            playerViewable.clear();
        }
    }

    public void upgrade(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return;
        }
        ServerSyncPacketBudget.beginServerTick(server.getTickCount());
        pruneDisconnectedPlayers(server);
        long tick = beginVisibilityTick();

        List<RenderEntity> entityList = new ArrayList<>(entities.values());
        for (RenderEntity entity : entityList) {
            if (entity.getCanceled()) {
                remove(entity);
                continue;
            }

            Level world = entity.getWorld();
            if (!(world instanceof ServerLevel serverLevel)) {
                continue;
            }

            boolean dirty = entity.shouldSync();
            boolean deferredDirty = false;
            List<ServerPlayer> players = serverLevel.players();
            for (int i = 0; i < players.size(); i++) {
                ServerPlayer player = players.get(i);
                HashSet<RenderEntity> viewSet = playerViewable.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
                boolean alreadyVisible = viewSet.contains(entity);

                if (!shouldProcessPlayerIndex(i, tick)) {
                    if (dirty && alreadyVisible) {
                        deferredDirty = true;
                    }
                    continue;
                }

                boolean shouldView = canView(entity, player);
                if (!shouldView && alreadyVisible) {
                    viewSet.remove(entity);
                    sendRemovePacket(player, entity);
                    continue;
                }
                if (shouldView && !alreadyVisible) {
                    if (trySendSpawnPacket(player, entity)) {
                        viewSet.add(entity);
                    }
                    continue;
                }
                if (shouldView && dirty) {
                    double dist = entity.getPos().distanceTo(player.position());
                    int lodInterval = computeLodInterval(dist, entity.getRenderRange());
                    if (lodInterval > 1 && (entity.getAge() % lodInterval) != 0) {
                        deferredDirty = true;
                        continue;
                    }
                    if (!trySendSyncPacket(player, entity)) {
                        deferredDirty = true;
                    }
                }
            }
            if (!dirty || !deferredDirty) {
                entity.clearDirty();
            }
        }

        if (entities.isEmpty()) {
            playerViewable.clear();
        }
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

    static boolean canView(RenderEntity entity, ServerPlayer player) {
        if (entity == null || player == null || entity.getWorld() == null || entity.getPos() == null) {
            return false;
        }
        if (player.isRemoved() || player.isSpectator()) {
            return false;
        }
        if (entity.getWorld() != player.level()) {
            return false;
        }
        return entity.getPos().distanceTo(player.position()) <= entity.getRenderRange();
    }

    private long beginVisibilityTick() {
        return visibilityTick++;
    }

    private boolean trySendSpawnPacket(ServerPlayer player, RenderEntity entity) {
        return trySendPacket(player, PacketRenderEntityS2C.ofSpawn(entity));
    }

    private boolean trySendSyncPacket(ServerPlayer player, RenderEntity entity) {
        return trySendPacket(player, PacketRenderEntityS2C.ofSync(entity));
    }

    private boolean trySendPacket(ServerPlayer player, PacketRenderEntityS2C packet) {
        if (!ServerSyncPacketBudget.tryAcquire()) {
            return false;
        }
        ReiParticlesNetwork.sendTo(player, packet);
        return true;
    }

    private void sendRemovePacket(ServerPlayer player, RenderEntity entity) {
        PacketRenderEntityS2C packet = PacketRenderEntityS2C.ofRemove(entity);
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private void clearTrackedView(RenderEntity entity, boolean notifyClients) {
        if (entity == null) {
            return;
        }
        ServerLevel level = entity.getWorld() instanceof ServerLevel serverLevel ? serverLevel : null;
        PacketRenderEntityS2C removePacket = notifyClients ? PacketRenderEntityS2C.ofRemove(entity) : null;
        Iterator<Map.Entry<UUID, HashSet<RenderEntity>>> iterator = playerViewable.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HashSet<RenderEntity>> entry = iterator.next();
            HashSet<RenderEntity> tracked = entry.getValue();
            boolean removed = tracked.remove(entity);
            if (removed && notifyClients && level != null) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    ReiParticlesNetwork.sendTo(player, removePacket);
                }
            }
            if (tracked.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void pruneDisconnectedPlayers(net.minecraft.server.MinecraftServer server) {
        playerViewable.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            return player == null || player.hasDisconnected();
        });
    }
}



