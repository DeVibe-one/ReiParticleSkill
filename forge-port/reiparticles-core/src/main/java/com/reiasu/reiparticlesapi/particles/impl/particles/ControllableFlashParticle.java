// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.impl.particles;

import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFlashEffect;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ControllableFlashParticle extends ControllableParticle {
    private final SpriteSet provider;

    public ControllableFlashParticle(ClientLevel world, Vec3 pos, Vec3 velocity,
                                    UUID controlUUID, boolean faceToCamera, SpriteSet provider) {
        super(world, pos, velocity, controlUUID, faceToCamera);
        this.provider = provider;
        this.pickSprite(provider);
        this.getController().addPreTickAction(p -> p.setSpriteFromAge(this.provider));
    }

    public SpriteSet getProvider() { return provider; }

    public static class Factory implements ParticleProvider<ControllableFlashEffect> {
        private final SpriteSet provider;

        public Factory(SpriteSet provider) { this.provider = provider; }
        public SpriteSet getProvider() { return provider; }

        @Override
        public Particle createParticle(ControllableFlashEffect parameters, ClientLevel world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new ControllableFlashParticle(world,
                    new Vec3(x, y, z), new Vec3(velocityX, velocityY, velocityZ),
                    parameters.getControlUUID(), parameters.getFaceToPlayer(), this.provider);
        }
    }
}
