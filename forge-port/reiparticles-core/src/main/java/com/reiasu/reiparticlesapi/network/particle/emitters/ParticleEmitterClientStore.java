// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.particle.emitter.EmitterRemoveEvent;
import com.reiasu.reiparticlesapi.event.events.particle.emitter.EmitterSpawnEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ParticleEmitterClientStore {
    private final Map<UUID, ParticleEmitters> clientEmitters = new ConcurrentHashMap<>();

    void createOrChange(ParticleEmitters emitters, Level viewWorld) {
        if (emitters == null || viewWorld == null) {
            return;
        }
        ParticleEmitters current = clientEmitters.get(emitters.getUuid());
        if (current == null) {
            Vec3 pos = emitters.position();
            emitters.bind(viewWorld, pos.x, pos.y, pos.z);
            clientEmitters.put(emitters.getUuid(), emitters);
            ReiEventBus.call(new EmitterSpawnEvent(emitters, true));
            return;
        }
        current.update(emitters);
        Vec3 pos = emitters.position();
        current.bind(viewWorld, pos.x, pos.y, pos.z);
        if (emitters.getCanceled()) {
            current.cancel();
        }
    }

    void tickClient() {
        clientEmitters.entrySet().removeIf(entry -> {
            ParticleEmitters emitters = entry.getValue();
            emitters.tick();
            if (!emitters.getCanceled()) {
                return false;
            }
            ReiEventBus.call(new EmitterRemoveEvent(emitters, true));
            return true;
        });
    }

    int size() {
        return clientEmitters.size();
    }

    Map<UUID, ParticleEmitters> snapshot() {
        return Collections.unmodifiableMap(clientEmitters);
    }

    void clear() {
        for (ParticleEmitters emitter : clientEmitters.values()) {
            emitter.cancel();
        }
        clientEmitters.clear();
    }
}
