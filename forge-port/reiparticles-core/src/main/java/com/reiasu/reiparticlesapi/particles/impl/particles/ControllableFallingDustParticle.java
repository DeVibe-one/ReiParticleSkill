// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.impl.particles;

import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFallingDustEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

public final class ControllableFallingDustParticle extends ControllableParticle {
    private float uo;
    private float vo;

    public ControllableFallingDustParticle(ClientLevel world, Vec3 pos, Vec3 velocity,
                                          UUID controlUUID, boolean faceToCamera, BlockState state) {
        super(world, pos, velocity, controlUUID, faceToCamera);
        BlockPos pos2 = BlockPos.containing(pos.x, pos.y, pos.z);
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModelShaper().getParticleIcon(state);
        Vector3f finalColor = new Vector3f(0.6f, 0.6f, 0.6f);
        if (!state.is(Blocks.GRASS_BLOCK)) {
            int i = Minecraft.getInstance().getBlockColors().getColor(state, world, pos2, 0);
            float r = 0.6f * (float) (i >> 16 & 0xFF) / 255.0f;
            float g = 0.6f * (float) (i >> 8 & 0xFF) / 255.0f;
            float b = 0.6f * (float) (i & 0xFF) / 255.0f;
            finalColor = new Vector3f(r, g, b);
        }
        this.setSprite(sprite);
        this.setColor(finalColor);
        this.quadSize /= 2.0f;
        this.uo = this.random.nextFloat() * 3.0f;
        this.vo = this.random.nextFloat() * 3.0f;
    }

    public float getUo() { return uo; }
    public void setUo(float uo) { this.uo = uo; }
    public float getVo() { return vo; }
    public void setVo(float vo) { this.vo = vo; }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0f) / 4.0f);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0f);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0f);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0f) / 4.0f);
    }

    public static class Factory implements ParticleProvider<ControllableFallingDustEffect> {
        @Override
        public Particle createParticle(ControllableFallingDustEffect parameters, ClientLevel world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            BlockState state = parameters.getBlockState();
            if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) {
                return null;
            }
            return new ControllableFallingDustParticle(world,
                    new Vec3(x, y, z), new Vec3(velocityX, velocityY, velocityZ),
                    parameters.getControlUUID(), parameters.getFaceToPlayer(), state);
        }
    }
}
