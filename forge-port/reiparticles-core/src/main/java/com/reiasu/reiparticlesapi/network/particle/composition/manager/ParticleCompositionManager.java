// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition.manager;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.annotations.composition.handler.ParticleCompositionHelper;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ParticleCompositionManager {
    public static final ParticleCompositionManager INSTANCE = new ParticleCompositionManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<ParticleComposition> compositions = new ArrayList<>();
    private final Map<UUID, ParticleComposition> clientView = new ConcurrentHashMap<>();
    private final Map<UUID, ParticleComposition> serverView = new ConcurrentHashMap<>();
    private final Map<String, Function<FriendlyByteBuf, ParticleComposition>> registeredTypes = new ConcurrentHashMap<>();
    private final Map<String, BufferCodec<ParticleComposition>> registeredCodecs = new ConcurrentHashMap<>();
    private final Map<Class<? extends ParticleComposition>, String> typeIdsByClass = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> visibleByPlayer = new ConcurrentHashMap<>();
    private final ParticleCompositionVisibilityTracker visibilityTracker =
            new ParticleCompositionVisibilityTracker(visibleByPlayer, this::buildPacket);
    private final Set<String> warnedUnregisteredTypes = ConcurrentHashMap.newKeySet();

    private ParticleCompositionManager() {
    }

    public Map<UUID, ParticleComposition> getClientView() {
        return clientView;
    }

    public Map<UUID, ParticleComposition> getServerView() {
        return serverView;
    }

    public Map<String, Function<FriendlyByteBuf, ParticleComposition>> getRegisteredTypes() {
        return registeredTypes;
    }

    public void registerType(String type, Function<FriendlyByteBuf, ParticleComposition> decoder) {
        if (type == null || type.isBlank() || decoder == null) {
            return;
        }
        registeredTypes.put(type, decoder);
    }

    public void registerType(String type,
                             BufferCodec<ParticleComposition> codec,
                             Class<? extends ParticleComposition> compositionClass) {
        if (type == null || type.isBlank() || codec == null || compositionClass == null) {
            return;
        }
        registerType(type, codec::decode);
        registeredCodecs.put(type, codec);
        typeIdsByClass.put(compositionClass, type);
        warnedUnregisteredTypes.remove(compositionClass.getName());
    }

    public void registerAutoType(Class<? extends ParticleComposition> compositionClass) {
        if (compositionClass == null) {
            return;
        }
        try {
            Constructor<? extends ParticleComposition> constructor =
                    compositionClass.getDeclaredConstructor(Vec3.class, Level.class);
            constructor.setAccessible(true);
            ParticleComposition sample = constructor.newInstance(Vec3.ZERO, null);
            BufferCodec<ParticleComposition> codec = ParticleCompositionHelper.INSTANCE.generateCodec(sample);
            registerType(compositionClass.getName(), codec, compositionClass);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to auto-register particle composition " + compositionClass.getName(), e);
        }
    }

    public void spawn(ParticleComposition composition) {
        if (composition == null) {
            return;
        }
        composition.display();
        synchronized (compositions) {
            compositions.add(composition);
            serverView.put(composition.getControlUUID(), composition);
        }
        if (composition.getWorld() instanceof ServerLevel level) {
            visibilityTracker.syncSpawnVisible(composition, level);
        }
        composition.clearDirty();
    }

    public void addClient(ParticleComposition composition) {
        clientView.put(composition.getControlUUID(), composition);
        composition.display();
    }

    public void tickAll() {
        long visibilityTick = visibilityTracker.beginTick();
        synchronized (compositions) {
            Iterator<ParticleComposition> iterator = compositions.iterator();
            while (iterator.hasNext()) {
                ParticleComposition composition = iterator.next();
                ServerLevel serverLevel = composition.getWorld() instanceof ServerLevel level ? level : null;
                try {
                    composition.tick();
                } catch (Exception e) {
                    LOGGER.warn("Particle composition {} ({}) failed during server tick; removing composition",
                            composition.getControlUUID(), composition.getClass().getName(), e);
                    composition.cancel();
                }
                if (composition.getCanceled()) {
                    iterator.remove();
                    serverView.remove(composition.getControlUUID());
                    visibilityTracker.removeAllViews(composition, serverLevel);
                    continue;
                }
                PacketParticleCompositionS2C dirtyPacket = composition.consumeDirty()
                        ? buildPacket(composition, false)
                        : null;
                if (serverLevel == null) {
                    visibilityTracker.removeAllViews(composition, null);
                    continue;
                }
                visibilityTracker.updateClientVisible(composition, serverLevel, visibilityTick, dirtyPacket);
            }
        }
        if (serverView.isEmpty()) {
            visibilityTracker.clear();
        } else {
            visibilityTracker.pruneDisconnectedPlayers(serverView.values());
        }
    }

    public void tickClient() {
        Iterator<Map.Entry<UUID, ParticleComposition>> iterator = clientView.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ParticleComposition> entry = iterator.next();
            ParticleComposition composition = entry.getValue();
            try {
                composition.tick();
            } catch (Exception e) {
                LOGGER.warn("Particle composition {} ({}) failed during client tick; removing composition",
                        composition.getControlUUID(), composition.getClass().getName(), e);
                composition.cancel();
            }
            if (composition.getCanceled()) {
                composition.remove();
                iterator.remove();
            }
        }
    }

    public int activeCount() {
        synchronized (compositions) {
            return compositions.size();
        }
    }

    public void clearClient() {
        for (ParticleComposition composition : clientView.values()) {
            composition.remove();
        }
        clientView.clear();
    }

    public void clearServer() {
        synchronized (compositions) {
            for (ParticleComposition composition : compositions) {
                composition.cancel();
            }
            compositions.clear();
        }
        serverView.clear();
        visibilityTracker.clear();
    }

    public void clear() {
        clearServer();
        clearClient();
    }

    public List<ParticleComposition> getCompositions() {
        synchronized (compositions) {
            return Collections.unmodifiableList(new ArrayList<>(compositions));
        }
    }

    PacketParticleCompositionS2C buildPacket(ParticleComposition composition, boolean distanceRemove) {
        String typeId = resolveTypeId(composition);
        if (typeId == null) {
            return null;
        }
        BufferCodec<ParticleComposition> codec = registeredCodecs.get(typeId);
        if (codec == null) {
            warnUnregisteredType(composition.getClass());
            return null;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, composition);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        PacketParticleCompositionS2C packet =
                new PacketParticleCompositionS2C(composition.getControlUUID(), typeId, data);
        packet.setDistanceRemove(distanceRemove);
        return packet;
    }

    private String resolveTypeId(ParticleComposition composition) {
        if (composition == null) {
            return null;
        }
        String typeId = typeIdsByClass.get(composition.getClass());
        if (typeId == null) {
            warnUnregisteredType(composition.getClass());
        }
        return typeId;
    }

    private void warnUnregisteredType(Class<? extends ParticleComposition> compositionClass) {
        String className = compositionClass.getName();
        if (warnedUnregisteredTypes.add(className)) {
            LOGGER.warn("Particle composition {} is not registered for network sync; skipping client synchronization", className);
        }
    }
}
