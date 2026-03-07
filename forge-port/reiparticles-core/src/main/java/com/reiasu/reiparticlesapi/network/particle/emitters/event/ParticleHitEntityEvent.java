// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.event;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.entity.Entity;

/**
 * Fired when a particle's trajectory intersects with a living entity.
 */
public final class ParticleHitEntityEvent implements ParticleEvent {

    public static final String EVENT_ID = "ParticleHitEntityEvent";

    private ControllableParticle particle;
    private ControllableParticleData particleData;
    private Entity hit;
    private boolean canceled;

    public ParticleHitEntityEvent(ControllableParticle particle, ControllableParticleData particleData, Entity hit) {
        this.particle = particle;
        this.particleData = particleData;
        this.hit = hit;
    }

    @Override
    public String getEventID() {
        return EVENT_ID;
    }

    @Override
    public ControllableParticle getParticle() {
        return particle;
    }

    @Override
    public void setParticle(ControllableParticle particle) {
        this.particle = particle;
    }

    @Override
    public ControllableParticleData getParticleData() {
        return particleData;
    }

    @Override
    public void setParticleData(ControllableParticleData data) {
        this.particleData = data;
    }

    public Entity getHit() {
        return hit;
    }

    public void setHit(Entity hit) {
        this.hit = hit;
    }

    @Override
    public boolean getCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
