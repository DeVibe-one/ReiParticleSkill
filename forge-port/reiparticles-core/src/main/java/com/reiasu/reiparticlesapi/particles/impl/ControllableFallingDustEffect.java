// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.impl;

import com.reiasu.reiparticlesapi.particles.ControllableParticleEffect;
import com.reiasu.reiparticlesapi.particles.ReiModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ControllableFallingDustEffect implements ControllableParticleEffect {
    private UUID uuid;
    private final BlockState blockState;
    private final boolean faceToPlayer;

    public ControllableFallingDustEffect(UUID uuid, BlockState blockState, boolean faceToPlayer) {
        this.uuid = uuid;
        this.blockState = blockState;
        this.faceToPlayer = faceToPlayer;
    }

    public ControllableFallingDustEffect(UUID uuid, BlockState blockState, boolean faceToPlayer, int ignored, Object ignored2) {
        this(uuid, blockState, faceToPlayer);
    }

    @Override
    public UUID getControlUUID() { return uuid; }

    @Override
    public void setControlUUID(UUID uuid) { this.uuid = uuid; }

    @Override
    public ControllableFallingDustEffect clone() {
        return new ControllableFallingDustEffect(UUID.randomUUID(), blockState, faceToPlayer);
    }

    public BlockState getBlockState() { return blockState; }
    @Override public boolean getFaceToPlayer() { return faceToPlayer; }

    @Override public ParticleType<?> getType() { return ReiModParticles.CONTROLLABLE_FALLING_DUST.get(); }
    @Override public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(uuid); buf.writeBoolean(faceToPlayer);
    }
    @Override public String writeToString() { return "coo:falling_dust"; }
}
