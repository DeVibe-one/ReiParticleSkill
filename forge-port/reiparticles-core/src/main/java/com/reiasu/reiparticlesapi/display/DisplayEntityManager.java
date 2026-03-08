// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.display;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.packet.PacketDisplayEntityS2C;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class DisplayEntityManager {
    public static final DisplayEntityManager INSTANCE = new DisplayEntityManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<DisplayEntity> displays = new ArrayList<>();
    private final Map<UUID, DisplayEntity> serverView = new ConcurrentHashMap<>();
    private final Map<UUID, DisplayEntity> clientView = new ConcurrentHashMap<>();
    private final Map<String, Function<FriendlyByteBuf, DisplayEntity>> registeredTypes = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> visibleByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingDirtySyncs = new ConcurrentHashMap<>();
    private final DisplayEntityVisibilityTracker visibilityTracker = new DisplayEntityVisibilityTracker(visibleByPlayer);
    private volatile boolean builtinTypesRegistered;

    private DisplayEntityManager() {
    }

    public void registerBuiltinTypes() {
        if (builtinTypesRegistered) {
            return;
        }
        builtinTypesRegistered = true;
        registerType(DebugDisplayEntity.TYPE_ID, DebugDisplayEntity::decode);
    }

    public Map<UUID, DisplayEntity> getServerView() {
        return serverView;
    }

    public Map<UUID, DisplayEntity> getClientView() {
        return clientView;
    }

    Map<UUID, Set<UUID>> getVisibleByPlayer() {
        return visibleByPlayer;
    }

    public Map<String, Function<FriendlyByteBuf, DisplayEntity>> getRegisteredTypes() {
        return registeredTypes;
    }

    public void registerType(String id, Function<FriendlyByteBuf, DisplayEntity> decoder) {
        if (id == null || id.isBlank() || decoder == null) {
            return;
        }
        registeredTypes.put(id, decoder);
    }

    public void spawn(Object display) {
        spawn(display, null);
    }

    public void spawn(Object display, ServerLevel level) {
        if (display instanceof DisplayEntity entity) {
            if (level != null) {
                entity.bindLevel(level);
            }
            synchronized (displays) {
                displays.add(entity);
            }
            serverView.put(entity.getControlUUID(), entity);
            pendingDirtySyncs.remove(entity.getControlUUID());
            entity.clearDirty();
        }
    }

    public void addClient(DisplayEntity entity) {
        clientView.put(entity.getControlUUID(), entity);
    }

    public void tickAll() {
        long visibilityTick = visibilityTracker.beginTick();
        synchronized (displays) {
            Iterator<DisplayEntity> iterator = displays.iterator();
            while (iterator.hasNext()) {
                DisplayEntity display = iterator.next();
                ServerLevel serverLevel = display.level() instanceof ServerLevel level ? level : null;
                try {
                    display.tick();
                } catch (Exception e) {
                    LOGGER.warn("Display entity {} ({}) failed during server tick; removing display",
                            display.getControlUUID(), display.getClass().getName(), e);
                    display.cancel();
                }
                if (display.getCanceled()) {
                    iterator.remove();
                    serverView.remove(display.getControlUUID());
                    pendingDirtySyncs.remove(display.getControlUUID());
                    visibilityTracker.removeAllViews(display, serverLevel);
                    continue;
                }
                if (display.consumeDirty()) {
                    pendingDirtySyncs.put(display.getControlUUID(), DisplayEntityVisibilityTracker.playerShardCount());
                }
                if (!hasSyncType(display)) {
                    visibilityTracker.removeAllViews(display, serverLevel);
                    pendingDirtySyncs.remove(display.getControlUUID());
                    continue;
                }
                if (serverLevel == null) {
                    visibilityTracker.removeAllViews(display, null);
                    pendingDirtySyncs.remove(display.getControlUUID());
                    continue;
                }
                boolean shouldSyncDirty = pendingDirtySyncs.containsKey(display.getControlUUID());
                PacketDisplayEntityS2C dirtyPacket = shouldSyncDirty ? PacketDisplayEntityS2C.ofToggle(display) : null;
                boolean dirtySyncComplete = visibilityTracker.updateClientVisible(display, serverLevel, visibilityTick, dirtyPacket);
                if (shouldSyncDirty && dirtySyncComplete) {
                    advanceDirtySyncWindow(display.getControlUUID());
                }
            }
        }
        if (serverView.isEmpty()) {
            pendingDirtySyncs.clear();
            visibilityTracker.clear();
        } else {
            visibilityTracker.pruneDisconnectedPlayers(serverView.values());
        }
    }

    public void tickClient() {
        Iterator<Map.Entry<UUID, DisplayEntity>> iterator = clientView.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, DisplayEntity> entry = iterator.next();
            DisplayEntity display = entry.getValue();
            try {
                display.tick();
            } catch (Exception e) {
                LOGGER.warn("Display entity {} ({}) failed during client tick; removing display",
                        display.getControlUUID(), display.getClass().getName(), e);
                display.cancel();
            }
            if (display.getCanceled()) {
                iterator.remove();
            }
        }
    }

    public int activeCount() {
        synchronized (displays) {
            return displays.size();
        }
    }

    public void clearClient() {
        for (DisplayEntity display : clientView.values()) {
            display.cancel();
        }
        clientView.clear();
    }

    public void clearServer() {
        synchronized (displays) {
            for (DisplayEntity display : displays) {
                display.cancel();
            }
            displays.clear();
        }
        serverView.clear();
        pendingDirtySyncs.clear();
        visibilityTracker.clear();
    }

    public void clear() {
        clearServer();
        for (DisplayEntity display : clientView.values()) {
            display.cancel();
        }
        clientView.clear();
    }

    public List<DisplayEntity> getDisplays() {
        synchronized (displays) {
            return Collections.unmodifiableList(new ArrayList<>(displays));
        }
    }

    static boolean isWithinVisibleRange(Vec3 displayPos, Vec3 viewerPos, double visibleRange) {
        if (displayPos == null || viewerPos == null) {
            return false;
        }
        return displayPos.distanceTo(viewerPos) <= Math.max(0.0, visibleRange);
    }

    private void advanceDirtySyncWindow(UUID displayId) {
        pendingDirtySyncs.computeIfPresent(displayId, (ignored, remaining) -> remaining <= 1 ? null : remaining - 1);
    }

    private static boolean hasSyncType(DisplayEntity display) {
        return display.typeId() != null && !display.typeId().isBlank();
    }
}

