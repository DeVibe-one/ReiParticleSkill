// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry mapping {@link ResourceLocation} keys to integer IDs and
 * {@link ParticleStyleProvider} instances for {@link ParticleGroupStyle}.
 * Both client and server must register in the same order so that the
 * auto-assigned integer IDs match.
 * <p>
 * Network packets transmit VarInt IDs instead of full class-name strings.
 */
public final class StyleRegistry {
    public static final StyleRegistry INSTANCE = new StyleRegistry();

    private final List<ResourceLocation> idToKey = new ArrayList<>();
    private final Map<ResourceLocation, Integer> keyToId = new ConcurrentHashMap<>();
    private final Map<Integer, ParticleStyleProvider<?>> providers = new ConcurrentHashMap<>();

    private StyleRegistry() {
    }

    /**
     * Register a style type. Must be called in identical order on client and server.
     *
     * @param key      unique {@link ResourceLocation} for this style type
     * @param provider the provider that creates / changes style instances
     * @return the assigned integer ID
     */
    public synchronized int register(ResourceLocation key, ParticleStyleProvider<?> provider) {
        if (key == null || provider == null) {
            throw new IllegalArgumentException("key and provider must not be null");
        }
        Integer existing = keyToId.get(key);
        if (existing != null) {
            providers.put(existing, provider);
            return existing;
        }
        int id = idToKey.size();
        idToKey.add(key);
        keyToId.put(key, id);
        providers.put(id, provider);
        return id;
    }

    public int getId(ResourceLocation key) {
        Integer id = keyToId.get(key);
        return id != null ? id : -1;
    }

    public ResourceLocation getKey(int id) {
        if (id < 0 || id >= idToKey.size()) {
            return null;
        }
        return idToKey.get(id);
    }

    public ParticleStyleProvider<?> getProvider(int id) {
        return providers.get(id);
    }

    public ParticleStyleProvider<?> getProvider(ResourceLocation key) {
        Integer id = keyToId.get(key);
        return id != null ? providers.get(id) : null;
    }

    public int size() {
        return idToKey.size();
    }

    public List<ResourceLocation> keys() {
        return Collections.unmodifiableList(idToKey);
    }
}
