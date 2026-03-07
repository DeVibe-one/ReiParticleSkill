// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.impl;

import com.reiasu.reiparticlesapi.network.particle.emitters.ClassParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Shield/defend emitter that spawns particles in a spherical shell
 * with inward velocity, creating a contracting shield effect.
 */
public final class DefendClassParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "defend_class");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());
    private UUID player;

    public DefendClassParticleEmitters(UUID player, Vec3 pos, Level world) {
        super(pos, world);
        this.player = player;
    }

    public DefendClassParticleEmitters(Vec3 pos, Level world) {
        this(null, pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();

        // Solid circle layer
        List<RelativeLocation> circlePoints = new PointsBuilder()
                .addCircle(1.0, 20)
                .create();
        for (RelativeLocation pt : circlePoints) {
            ControllableParticleData data = templateData.clone();
            result.add(new AbstractMap.SimpleEntry<>(data, pt));
        }

        // Translucent polygon layers (shrinking hexagons)
        for (double step = 1.0; step > 0.0; step -= 0.1) {
            int sides = 6;
            int count = Math.max((int) Math.round(5 * step), 1);
            List<RelativeLocation> polyPoints = new PointsBuilder()
                    .addCircle(step, count * sides)
                    .create();
            for (RelativeLocation pt : polyPoints) {
                ControllableParticleData data = templateData.clone();
                data.setAlpha(0.15f);
                result.add(new AbstractMap.SimpleEntry<>(data, pt));
            }
        }
        return result;
    }

    @Override
    public void singleParticleAction(Controllable<?> controller, SerializableData data,
                                      RelativeLocation spawnPos, Level spawnWorld,
                                      float particleLerpProgress, float posLerpProgress) {
    }

    @Override
    public ResourceLocation getEmittersID() {
        return ID;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        if (player != null) buf.writeUUID(player); else buf.writeUUID(UUID.randomUUID());
        ClassParticleEmitters.Companion.encodeBase(this, buf);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        player = buf.readUUID();
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof DefendClassParticleEmitters other) {
            this.templateData = other.templateData;
            this.player = other.player;
        }
    }
}
