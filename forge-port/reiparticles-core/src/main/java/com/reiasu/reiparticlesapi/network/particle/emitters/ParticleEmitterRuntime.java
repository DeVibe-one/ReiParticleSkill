// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.config.APIConfig;
import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.particle.emitter.EmitterRemoveEvent;
import com.reiasu.reiparticlesapi.event.events.particle.emitter.EmitterSpawnEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

final class ParticleEmitterRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final List<ParticleEmitters> emitters = new ArrayList<>();
    private final ParticleEmitterVisibilityTracker visibilityTracker = new ParticleEmitterVisibilityTracker();
    private final ParticleEmitterClientStore clientStore = new ParticleEmitterClientStore();
    private volatile boolean builtinsRegistered;

    void registerBuiltinCodecs() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        registerCodec(DebugParticleEmitters.CODEC_ID, DebugParticleEmitters::decode);
        registerCodec(DebugRailgunEmitters.CODEC_ID, DebugRailgunEmitters::decode);
    }

    int registerCodec(ResourceLocation id, Function<FriendlyByteBuf, ParticleEmitters> decoder) {
        if (id == null || decoder == null) {
            return -1;
        }
        return EmitterRegistry.INSTANCE.register(id, decoder);
    }

    Function<FriendlyByteBuf, ParticleEmitters> getCodecFromID(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return EmitterRegistry.INSTANCE.getDecoder(id);
    }

    void spawnEmitters(Object emitter, ServerLevel level, double x, double y, double z) {
        if (!(emitter instanceof ParticleEmitters particleEmitters)) {
            return;
        }
        if (level != null) {
            particleEmitters.bind(level, x, y, z);
        }
        boolean spawned = false;
        int emitterLimit = APIConfig.INSTANCE.getParticleCountLimit();
        synchronized (emitters) {
            if (emitters.size() < emitterLimit) {
                emitters.add(particleEmitters);
                spawned = true;
            }
        }
        if (spawned) {
            ReiEventBus.call(new EmitterSpawnEvent(particleEmitters, false));
        }
    }

    void createOrChangeClient(ParticleEmitters emitters, Level viewWorld) {
        clientStore.createOrChange(emitters, viewWorld);
    }

    int[] getLastTickStats() {
        return visibilityTracker.getLastTickStats();
    }

    String getDebugInfo() {
        int serverCount = activeCount();
        int[] stats = getLastTickStats();
        return String.format(
                "Emitters: server=%d, client=%d | Viewers: %d players tracking | Last tick: synced=%d, skippedLod=%d, skippedShard=%d, throttled=%d",
                serverCount,
                clientStore.size(),
                visibilityTracker.trackedPlayerCount(),
                stats[0],
                stats[1],
                stats[2],
                stats[3]);
    }

    void tickAll() {
        long tick = visibilityTracker.beginTick();
        synchronized (emitters) {
            Iterator<ParticleEmitters> iterator = emitters.iterator();
            while (iterator.hasNext()) {
                ParticleEmitters current = iterator.next();
                try {
                    visibilityTracker.updateClientVisible(current, tick);
                    current.tick();
                } catch (Exception e) {
                    LOGGER.warn("Emitter {} ({}) failed during server tick; removing emitter",
                            current.getUuid(), current.getClass().getName(), e);
                    current.cancel();
                }
                if (!current.getCanceled()) {
                    continue;
                }
                discardServerEmitter(current, true);
                iterator.remove();
            }
        }
        visibilityTracker.pruneDisconnectedPlayers(serverEmittersSnapshot());
    }

    void tickClient() {
        clientStore.tickClient();
    }

    int activeCount() {
        synchronized (emitters) {
            return emitters.size();
        }
    }

    void clearServer() {
        synchronized (emitters) {
            for (ParticleEmitters emitter : emitters) {
                discardServerEmitter(emitter, false);
            }
            emitters.clear();
        }
        visibilityTracker.clear();
    }

    void clear() {
        synchronized (emitters) {
            for (ParticleEmitters emitter : emitters) {
                discardServerEmitter(emitter, true);
            }
            emitters.clear();
        }
        visibilityTracker.clear();
        clientStore.clear();
    }

    void clearClient() {
        clientStore.clear();
    }

    List<ParticleEmitters> getEmitters() {
        return Collections.unmodifiableList(serverEmittersSnapshot());
    }

    Map<UUID, ParticleEmitters> getClientEmitters() {
        return clientStore.snapshot();
    }

    private List<ParticleEmitters> serverEmittersSnapshot() {
        synchronized (emitters) {
            return new ArrayList<>(emitters);
        }
    }

    private void discardServerEmitter(ParticleEmitters emitter, boolean notifyClients) {
        if (emitter == null) {
            return;
        }
        emitter.cancel();
        if (notifyClients) {
            visibilityTracker.removeAllViews(emitter);
        }
        ReiEventBus.call(new EmitterRemoveEvent(emitter, false));
    }
}
