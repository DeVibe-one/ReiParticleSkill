// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Thin static facade for the particle emitter runtime.
 */
public final class ParticleEmittersManager {
    private static final ParticleEmitterRuntime RUNTIME = new ParticleEmitterRuntime();

    private ParticleEmittersManager() {
    }

    public static void registerBuiltinCodecs() {
        RUNTIME.registerBuiltinCodecs();
    }

    public static int registerCodec(ResourceLocation id, Function<FriendlyByteBuf, ParticleEmitters> decoder) {
        return RUNTIME.registerCodec(id, decoder);
    }

    public static Function<FriendlyByteBuf, ParticleEmitters> getCodecFromID(ResourceLocation id) {
        return RUNTIME.getCodecFromID(id);
    }

    public static void spawnEmitters(Object emitter) {
        RUNTIME.spawnEmitters(emitter, null, 0.0, 0.0, 0.0);
    }

    public static void spawnEmitters(Object emitter, ServerLevel level, double x, double y, double z) {
        RUNTIME.spawnEmitters(emitter, level, x, y, z);
    }

    public static void createOrChangeClient(ParticleEmitters emitters, Level viewWorld) {
        RUNTIME.createOrChangeClient(emitters, viewWorld);
    }

    public static int[] getLastTickStats() {
        return RUNTIME.getLastTickStats();
    }

    public static String getDebugInfo() {
        return RUNTIME.getDebugInfo();
    }

    public static void tickAll() {
        RUNTIME.tickAll();
    }

    public static void tickClient() {
        RUNTIME.tickClient();
    }

    public static int activeCount() {
        return RUNTIME.activeCount();
    }

    public static void clear() {
        RUNTIME.clear();
    }

    public static List<ParticleEmitters> getEmitters() {
        return RUNTIME.getEmitters();
    }

    public static Map<UUID, ParticleEmitters> getClientEmitters() {
        return RUNTIME.getClientEmitters();
    }
}
