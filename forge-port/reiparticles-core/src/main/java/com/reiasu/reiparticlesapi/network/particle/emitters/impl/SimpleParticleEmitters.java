// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.impl;

import com.reiasu.reiparticlesapi.network.particle.emitters.ClassParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.type.EmittersShootType;
import com.reiasu.reiparticlesapi.network.particle.emitters.type.PointEmittersShootType;
import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * General-purpose particle emitter with configurable count, offset,
 * shoot type, and expression-evaluated position offsets per tick.
 * <p>
 * In the original Fabric version, math expressions (evalEmittersXWithT, etc.)
 * were used to compute dynamic positions. In this Forge port, these are stored
 * as strings for serialization compatibility but evaluated as constants.
 */
public final class SimpleParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "simple_particle");

    private ControllableParticleData templateData;
    private final Random random = new Random(System.currentTimeMillis());

    int count = 1;
    int countRandom = 0;
    Vec3 offset = Vec3.ZERO;
    EmittersShootType shootType = new PointEmittersShootType();
    String evalEmittersXWithT = "0";
    String evalEmittersYWithT = "0";
    String evalEmittersZWithT = "0";

    public SimpleParticleEmitters(Vec3 pos, Level world, ControllableParticleData templateData) {
        super(pos, world);
        this.templateData = templateData != null ? templateData : new ControllableParticleData();
    }

    public SimpleParticleEmitters(Vec3 pos, Level world) {
        this(pos, world, new ControllableParticleData());
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCountRandom() {
        return countRandom;
    }

    public void setCountRandom(int countRandom) {
        this.countRandom = countRandom;
    }

    public Vec3 getOffset() {
        return offset;
    }

    public void setOffset(Vec3 offset) {
        this.offset = offset;
    }

    public EmittersShootType getShootTypeInstance() {
        return shootType;
    }

    public void setShootTypeInstance(EmittersShootType shootType) {
        this.shootType = shootType;
    }

    public String getEvalEmittersXWithT() {
        return evalEmittersXWithT;
    }

    public void setEvalEmittersXWithT(String expr) {
        this.evalEmittersXWithT = expr;
    }

    public String getEvalEmittersYWithT() {
        return evalEmittersYWithT;
    }

    public void setEvalEmittersYWithT(String expr) {
        this.evalEmittersYWithT = expr;
    }

    public String getEvalEmittersZWithT() {
        return evalEmittersZWithT;
    }

    public void setEvalEmittersZWithT(String expr) {
        this.evalEmittersZWithT = expr;
    }

    public void setup() {
        // Initialize shoot type or other state after deserialization
    }

    @Override
    public void doTick() {
        double evalX = evalExpression(evalEmittersXWithT, getTick());
        double evalY = evalExpression(evalEmittersYWithT, getTick());
        double evalZ = evalExpression(evalEmittersZWithT, getTick());
        offset = new Vec3(evalX, evalY, evalZ);
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        int totalCount = count + (countRandom > 0 ? random.nextInt(countRandom) : 0);
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>(totalCount);
        for (int i = 0; i < totalCount; i++) {
            ControllableParticleData data = templateData.clone();
            RelativeLocation spawnPos = new RelativeLocation(offset.x, offset.y, offset.z);
            result.add(new AbstractMap.SimpleEntry<>(data, spawnPos));
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
        // Custom serialization matching Fabric's CODEC order
        List<com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler> handles =
                collectEventHandles();
        buf.writeInt(handles.size());
        for (com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler h : handles) {
            buf.writeUtf(h.getHandlerID());
        }
        buf.writeInt(count);
        buf.writeInt(countRandom);
        buf.writeDouble(position().x);
        buf.writeDouble(position().y);
        buf.writeDouble(position().z);
        buf.writeInt(getTick());
        buf.writeInt(getMaxTick());
        buf.writeInt(getDelay());
        buf.writeUUID(getUuid());
        buf.writeBoolean(getPlaying());
        buf.writeBoolean(getCanceled());
        buf.writeUtf(evalEmittersXWithT);
        buf.writeUtf(evalEmittersYWithT);
        buf.writeUtf(evalEmittersZWithT);
        buf.writeDouble(offset.x);
        buf.writeDouble(offset.y);
        buf.writeDouble(offset.z);
        buf.writeUtf(shootType.getID());
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        int handlerCount = buf.readInt();
        List<com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler> handlers =
                new ArrayList<>();
        for (int i = 0; i < handlerCount; i++) {
            String handleID = buf.readUtf();
            com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler handler =
                    com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandlerManager
                            .INSTANCE.getHandlerById(handleID);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        addEventHandlerList(handlers);
        count = buf.readInt();
        countRandom = buf.readInt();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        bind(level(), pos.x, pos.y, pos.z);
        setTick(buf.readInt());
        setMaxTick(buf.readInt());
        setDelay(buf.readInt());
        setUuid(buf.readUUID());
        setPlaying(buf.readBoolean());
        if (buf.readBoolean()) cancel();
        evalEmittersXWithT = buf.readUtf();
        evalEmittersYWithT = buf.readUtf();
        evalEmittersZWithT = buf.readUtf();
        offset = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        String typeID = buf.readUtf();
        java.util.function.Supplier<EmittersShootType> factory =
                com.reiasu.reiparticlesapi.network.particle.emitters.type.EmittersShootTypes.INSTANCE.fromID(typeID);
        if (factory != null) {
            shootType = factory.get();
        }
        templateData.readFromBuf(buf);
        setup();
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof SimpleParticleEmitters other) {
            this.templateData = other.templateData;
            this.count = other.count;
            this.countRandom = other.countRandom;
            this.offset = other.offset;
            this.shootType = other.shootType;
            this.evalEmittersXWithT = other.evalEmittersXWithT;
            this.evalEmittersYWithT = other.evalEmittersYWithT;
            this.evalEmittersZWithT = other.evalEmittersZWithT;
        }
    }

    /**
     * Evaluate a math expression string with variable {@code t} (tick).
     */
    private static double evalExpression(String expr, int tick) {
        if (expr == null || expr.isEmpty()) return 0.0;
        try {
            return new com.reiasu.reiparticlesapi.utils.math.ExpressionEvaluator(expr)
                    .with("t", tick)
                    .evaluate();
        } catch (Exception e) {
            return 0.0;
        }
    }
}
