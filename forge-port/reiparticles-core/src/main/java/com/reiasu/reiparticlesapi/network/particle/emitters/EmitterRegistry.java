// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry mapping {@link ResourceLocation} keys to integer IDs and decoders
 * for {@link ParticleEmitters}. Both client and server must register in the
 * same order so that the auto-assigned integer IDs match.
 * <p>
 * Network packets transmit VarInt IDs instead of full class-name strings.
 */
public final class EmitterRegistry {
    public static final EmitterRegistry INSTANCE = new EmitterRegistry();

    private final List<ResourceLocation> idToKey = new ArrayList<>();
    private final Map<ResourceLocation, Integer> keyToId = new ConcurrentHashMap<>();
    private final Map<Integer, Function<FriendlyByteBuf, ParticleEmitters>> decoders = new ConcurrentHashMap<>();

    private EmitterRegistry() {
    }

    /**
     * Register an emitter type. Must be called in identical order on client and server.
     *
     * @param key     unique {@link ResourceLocation} for this emitter type
     * @param decoder function that reads a {@link ParticleEmitters} from a buffer
     * @return the assigned integer ID
     */
    public synchronized int register(ResourceLocation key, Function<FriendlyByteBuf, ParticleEmitters> decoder) {
        if (key == null || decoder == null) {
            throw new IllegalArgumentException("key and decoder must not be null");
        }
        Integer existing = keyToId.get(key);
        if (existing != null) {
            decoders.put(existing, decoder);
            return existing;
        }
        int id = idToKey.size();
        idToKey.add(key);
        keyToId.put(key, id);
        decoders.put(id, decoder);
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

    public Function<FriendlyByteBuf, ParticleEmitters> getDecoder(int id) {
        return decoders.get(id);
    }

    public Function<FriendlyByteBuf, ParticleEmitters> getDecoder(ResourceLocation key) {
        Integer id = keyToId.get(key);
        return id != null ? decoders.get(id) : null;
    }

    public int size() {
        return idToKey.size();
    }

    public List<ResourceLocation> keys() {
        return Collections.unmodifiableList(idToKey);
    }
}
