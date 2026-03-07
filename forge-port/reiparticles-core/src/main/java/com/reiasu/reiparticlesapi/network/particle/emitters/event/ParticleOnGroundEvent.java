// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.event;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fired when a particle hits the ground (block collision from above/below).
 */
public final class ParticleOnGroundEvent implements ParticleEvent {

    public static final String EVENT_ID = "ParticleOnGroundEvent";

    private ControllableParticle particle;
    private ControllableParticleData particleData;
    private BlockPos hit;
    private Vec3 intersection;
    private HitResult res;
    private boolean canceled;

    public ParticleOnGroundEvent(ControllableParticle particle, ControllableParticleData particleData,
                                 BlockPos hit, Vec3 intersection, HitResult res) {
        this.particle = particle;
        this.particleData = particleData;
        this.hit = hit;
        this.intersection = intersection;
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

    public BlockPos getHit() {
        return hit;
    }

    public void setHit(BlockPos hit) {
        this.hit = hit;
    }

    public Vec3 getIntersection() {
        return intersection;
    }

    public void setIntersection(Vec3 intersection) {
        this.intersection = intersection;
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
