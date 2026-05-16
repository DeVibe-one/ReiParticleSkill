// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Composition that maintains its own particle entry map.
 * Subclasses populate the map via {@link #addParticle} and the
 * parent lifecycle handles display and tick automatically.
 */
public class AutoParticleComposition extends ParticleComposition {

    private final Map<CompositionData, RelativeLocation> particles = new ConcurrentHashMap<>();

    public AutoParticleComposition() {
        super();
    }

    public AutoParticleComposition(net.minecraft.world.phys.Vec3 position, net.minecraft.world.level.Level world) {
        super(position, world);
    }

    /**
     * Adds a particle entry at the given relative location.
     * The composition will display it automatically when {@link #display()} is called.
     */
    public AutoParticleComposition addParticle(CompositionData data, RelativeLocation location) {
        particles.put(Objects.requireNonNull(data, "data"), Objects.requireNonNull(location, "location").copy());
        markDirty();
        return this;
    }

    /**
     * Removes a particle entry from this composition.
     */
    public AutoParticleComposition removeParticle(CompositionData data) {
        particles.remove(Objects.requireNonNull(data, "data"));
        markDirty();
        return this;
    }

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        Map<CompositionData, RelativeLocation> snapshot = new LinkedHashMap<>();
        for (Map.Entry<CompositionData, RelativeLocation> entry : particles.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().copy());
        }
        return snapshot;
    }

    @Override
    public void onDisplay() {
    }
}
