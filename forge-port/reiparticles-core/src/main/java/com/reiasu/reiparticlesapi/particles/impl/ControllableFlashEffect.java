// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.impl;

import com.reiasu.reiparticlesapi.particles.ControllableParticleEffect;
import com.reiasu.reiparticlesapi.particles.ReiModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class ControllableFlashEffect implements ControllableParticleEffect {
    private UUID uuid;
    private final boolean faceToPlayer;

    public ControllableFlashEffect(UUID uuid, boolean faceToPlayer) {
        this.uuid = uuid;
        this.faceToPlayer = faceToPlayer;
    }

    public ControllableFlashEffect(UUID uuid, boolean faceToPlayer, int ignored, Object ignored2) {
        this(uuid, faceToPlayer);
    }

    @Override
    public UUID getControlUUID() { return uuid; }

    @Override
    public void setControlUUID(UUID uuid) { this.uuid = uuid; }

    @Override
    public ControllableFlashEffect clone() {
        return new ControllableFlashEffect(UUID.randomUUID(), faceToPlayer);
    }

    @Override public boolean getFaceToPlayer() { return faceToPlayer; }

    @Override public ParticleType<?> getType() { return ReiModParticles.CONTROLLABLE_FLASH.get(); }
    @Override public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(uuid); buf.writeBoolean(faceToPlayer);
    }
    @Override public String writeToString() { return "coo:flash"; }
}
