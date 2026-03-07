// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.event;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.phys.HitResult;

/**
 * Fired when a particle collides with any block or entity (general ray-cast hit).
 */
public final class ParticleCollideEvent implements ParticleEvent {

    public static final String EVENT_ID = "ParticleColliderEvent";

    private ControllableParticle particle;
    private ControllableParticleData particleData;
    private HitResult res;
    private boolean canceled;

    public ParticleCollideEvent(ControllableParticle particle, ControllableParticleData particleData, HitResult res) {
        this.particle = particle;
        this.particleData = particleData;
        this.res = res;
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

    public HitResult getRes() {
        return res;
    }

    public void setRes(HitResult res) {
        this.res = res;
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
