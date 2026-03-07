// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.impl;

import com.reiasu.reiparticlesapi.particles.ControllableParticleEffect;
import com.reiasu.reiparticlesapi.particles.ReiModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class ControllableSplashEffect implements ControllableParticleEffect {
    private UUID uuid;
    private final boolean faceToPlayer;

    public ControllableSplashEffect(UUID uuid, boolean faceToPlayer) {
        this.uuid = uuid;
        this.faceToPlayer = faceToPlayer;
    }

    public ControllableSplashEffect(UUID uuid, boolean faceToPlayer, int ignored, Object ignored2) {
        this(uuid, faceToPlayer);
    }

    @Override
    public UUID getControlUUID() { return uuid; }

    @Override
    public void setControlUUID(UUID uuid) { this.uuid = uuid; }

    @Override
    public ControllableSplashEffect clone() {
        return new ControllableSplashEffect(UUID.randomUUID(), faceToPlayer);
    }

    @Override public boolean getFaceToPlayer() { return faceToPlayer; }

    @Override public ParticleType<?> getType() { return ReiModParticles.CONTROLLABLE_SPLASH.get(); }
    @Override public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(uuid); buf.writeBoolean(faceToPlayer);
    }
    @Override public String writeToString() { return "coo:splash"; }
}
