// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleStyleS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleStyleManager {
    private static final Map<UUID, ParticleGroupStyle> SERVER_VIEW_STYLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> VISIBLE = new ConcurrentHashMap<>();
    private static final Map<UUID, ParticleGroupStyle> CLIENT_VIEW_STYLES = new ConcurrentHashMap<>();

    private ParticleStyleManager() {
    }

    public static int register(ResourceLocation key, ParticleStyleProvider<?> provider) {
        if (key == null || provider == null) {
            return -1;
        }
        return StyleRegistry.INSTANCE.register(key, provider);
    }

    public static ParticleStyleProvider<?> getProvider(ResourceLocation key) {
        return StyleRegistry.INSTANCE.getProvider(key);
    }

    public static ParticleStyleProvider<?> getProviderByRawID(int id) {
        return StyleRegistry.INSTANCE.getProvider(id);
    }

    public static int getStyleRegistryId(ResourceLocation key) {
        return StyleRegistry.INSTANCE.getId(key);
    }

    public static ResourceLocation getStyleKey(int id) {
        return StyleRegistry.INSTANCE.getKey(id);
    }

    public static Map<UUID, ParticleGroupStyle> getServerViewStyles() {
        return SERVER_VIEW_STYLES;
    }

    public static Map<UUID, Set<UUID>> getVisible() {
        return VISIBLE;
    }

    public static Map<UUID, ParticleGroupStyle> getClientViewStyles() {
        return CLIENT_VIEW_STYLES;
    }

    public static void spawnStyle(Level world, Vec3 pos, ParticleGroupStyle style) {
        if (world == null || pos == null || style == null) {
            return;
        }

        style.display(pos, world);
        if (world.isClientSide) {
            CLIENT_VIEW_STYLES.put(style.getUuid(), style);
            return;
        }

        SERVER_VIEW_STYLES.put(style.getUuid(), style);
        if (world instanceof ServerLevel serverLevel) {
            style.setLastUpdatedGameTime(serverLevel.getGameTime());
            for (ServerPlayer player : serverLevel.players()) {
                if (canViewStyle(style, player)) {
                    addStylePlayerView(player, style);
                }
            }
        }
    }

    public static void doTickClient() {
        CLIENT_VIEW_STYLES.entrySet().removeIf(entry -> {
            ParticleGroupStyle style = entry.getValue();
            style.tick();
            return style.getCanceled();
        });
    }

    public static void doTickServer() {
        SERVER_VIEW_STYLES.entrySet().removeIf(entry -> {
            ParticleGroupStyle style = entry.getValue();
            if (!(style.getWorld() instanceof ServerLevel serverLevel)) {
                removeAllPlayerView(style, null);
                return true;
            }

            style.setLastUpdatedGameTime(serverLevel.getGameTime());
            upgradeVisible(style, serverLevel);
            style.tick();

            if (style.getAutoToggle() && !style.getCanceled()) {
                PacketParticleStyleS2C changePacket = buildAutoTogglePacket(style);
                for (UUID playerId : filterVisiblePlayer(style.getUuid())) {
                    ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        ReiParticlesNetwork.sendTo(player, changePacket);
                    }
                }
            }

            if (style.getCanceled()) {
                removeAllPlayerView(style, serverLevel);
                return true;
            }
            return false;
        });

        pruneDisconnectedPlayers();
    }

    private static Set<UUID> filterVisiblePlayer(UUID styleId) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> entry : VISIBLE.entrySet()) {
            if (entry.getValue().contains(styleId)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static void upgradeVisible(ParticleGroupStyle style, ServerLevel level) {
        UUID styleId = style.getUuid();
        for (ServerPlayer player : level.players()) {
            Set<UUID> visibleSet = VISIBLE.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
            boolean shouldView = canViewStyle(style, player);
            boolean alreadyView = visibleSet.contains(styleId);

            if (shouldView && !alreadyView) {
                addStylePlayerView(player, style);
                continue;
            }

            if (!shouldView && alreadyView) {
                removeStylePlayerView(player, style);
            }
        }
    }

    private static boolean canViewStyle(ParticleGroupStyle style, ServerPlayer player) {
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

    private static void removeStylePlayerView(ServerPlayer player, ParticleGroupStyle style) {
        Set<UUID> visibleSet = VISIBLE.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
        visibleSet.remove(style.getUuid());
        PacketParticleStyleS2C packet = new PacketParticleStyleS2C(style.getUuid(), ControlType.REMOVE, Map.of());
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private static void addStylePlayerView(ServerPlayer player, ParticleGroupStyle style) {
        Set<UUID> visibleSet = VISIBLE.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet());
        if (!visibleSet.add(style.getUuid())) {
            return;
        }
        PacketParticleStyleS2C packet = buildCreatePacket(style, style.getPos());
        ReiParticlesNetwork.sendTo(player, packet);
    }

    private static PacketParticleStyleS2C buildAutoTogglePacket(ParticleGroupStyle style) {
        ParticleControllerDataBuffers buffers = ParticleControllerDataBuffers.INSTANCE;
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>(style.writePacketArgs());
        args.put("rotate", buffers.doubleValue(style.getRotate()));
        args.put("axis", buffers.vec3d(style.getAxis().toVector()));
        args.put("scale", buffers.doubleValue(style.getScale()));
        args.put("lastUpdatedGameTime", buffers.longValue(style.getLastUpdatedGameTime()));
        args.put("displayedTime", buffers.longValue(style.getDisplayedTime()));
        args.put("visibleRange", buffers.doubleValue(style.getVisibleRange()));
        args.put("autoToggle", buffers.bool(style.getAutoToggle()));
        return new PacketParticleStyleS2C(style.getUuid(), ControlType.CHANGE, args);
    }

    private static PacketParticleStyleS2C buildCreatePacket(ParticleGroupStyle style, Vec3 pos) {
        ParticleControllerDataBuffers buffers = ParticleControllerDataBuffers.INSTANCE;
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        ResourceLocation styleKey = style.getRegistryKey();
        int styleRegistryId = styleKey != null ? StyleRegistry.INSTANCE.getId(styleKey) : -1;
        args.put("style_type_id", buffers.intValue(styleRegistryId));
        args.put("pos", buffers.vec3d(pos));
        args.put("rotate", buffers.doubleValue(style.getRotate()));
        args.put("axis", buffers.vec3d(style.getAxis().toVector()));
        args.put("scale", buffers.doubleValue(style.getScale()));
        args.put("lastUpdatedGameTime", buffers.longValue(style.getLastUpdatedGameTime()));
        args.put("displayedTime", buffers.longValue(style.getDisplayedTime()));
        args.put("visibleRange", buffers.doubleValue(style.getVisibleRange()));
        args.put("autoToggle", buffers.bool(style.getAutoToggle()));
        args.putAll(style.writePacketArgs());
        return new PacketParticleStyleS2C(style.getUuid(), ControlType.CREATE, args);
    }

    private static void removeAllPlayerView(ParticleGroupStyle style, ServerLevel level) {
        UUID styleId = style.getUuid();
        for (Map.Entry<UUID, Set<UUID>> entry : VISIBLE.entrySet()) {
            UUID playerId = entry.getKey();
            Set<UUID> visibleSet = entry.getValue();
            if (!visibleSet.remove(styleId)) {
                continue;
            }
            if (level == null) {
                continue;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                PacketParticleStyleS2C packet = new PacketParticleStyleS2C(styleId, ControlType.REMOVE, Map.of());
                ReiParticlesNetwork.sendTo(player, packet);
            }
        }
    }

    private static void pruneDisconnectedPlayers() {
        if (SERVER_VIEW_STYLES.isEmpty()) {
            VISIBLE.clear();
            return;
        }
        VISIBLE.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            for (ParticleGroupStyle style : SERVER_VIEW_STYLES.values()) {
                if (style.getWorld() instanceof ServerLevel level) {
                    if (level.getServer().getPlayerList().getPlayer(playerId) != null) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    public static void clearAllVisible() {
        for (ParticleGroupStyle style : CLIENT_VIEW_STYLES.values()) {
            style.remove();
        }
        CLIENT_VIEW_STYLES.clear();
    }
}
