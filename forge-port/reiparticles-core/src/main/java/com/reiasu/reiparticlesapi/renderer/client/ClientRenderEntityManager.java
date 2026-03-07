// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.renderer.client;

import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Client-side manager for {@link RenderEntity} instances received via packets.
 * <p>
 * Tracks visible entities, ticks them, and provides codec registration
 * for decoding entities from server-side packets.
 */
public final class ClientRenderEntityManager {
    public static final ClientRenderEntityManager INSTANCE = new ClientRenderEntityManager();

    private final ConcurrentHashMap<UUID, RenderEntity> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ResourceLocation, Function<byte[], RenderEntity>> codecs = new ConcurrentHashMap<>();

    private ClientRenderEntityManager() {}

    public ConcurrentHashMap<UUID, RenderEntity> getEntities() {
        return entities;
    }

    public void registerCodec(ResourceLocation id, Function<byte[], RenderEntity> codec) {
        codecs.put(id, codec);
    }

    public Function<byte[], RenderEntity> getCodecFromID(ResourceLocation id) {
        return codecs.get(id);
    }

    public void add(RenderEntity entity) {
        if (entity == null) return;
        entities.put(entity.getUuid(), entity);
    }

    public RenderEntity getFrom(UUID uuid) {
        return entities.get(uuid);
    }

    public void remove(UUID uuid) {
        RenderEntity entity = entities.remove(uuid);
        if (entity != null) {
            entity.setCanceled(true);
        }
    }

    public void clear() {
        for (RenderEntity entity : entities.values()) {
            entity.setCanceled(true);
        }
        entities.clear();
    }

    /**
     * Called every client tick. Ticks all visible render entities and removes canceled ones.
     */
    public void doClientTick() {
        entities.entrySet().removeIf(entry -> {
            RenderEntity entity = entry.getValue();
            if (entity.getCanceled()) return true;
            entity.tick();
            return false;
        });
    }
}
