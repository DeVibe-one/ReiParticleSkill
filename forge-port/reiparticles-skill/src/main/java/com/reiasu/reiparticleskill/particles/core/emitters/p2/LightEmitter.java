// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p2;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Lightning-attenuation emitter that spawns branching particle bolts
 * outward from the emitter origin, matching the Fabric original's visual intent.
 */
@ReiAutoRegister
 public final class LightEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "light");

    private static final DustParticleOptions BOLT_COLOR =
            new DustParticleOptions(new Vector3f(0.85f, 0.92f, 1.0f), 0.55f);

    private final RandomSource random = RandomSource.create();
    private final List<BoltState> bolts = new ArrayList<>();

    private double lightTargetOffsetMin = 12.0;
    private double lightTargetOffsetMax = 40.0;
    private double offsetMin = 1.0;
    private double offsetMax = 2.0;
    private int sampStepMin = 50;
    private int sampStepMax = 60;
    private double att = 0.3;

    public LightEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(120);
    }

    public LightEmitter setLightTargetOffset(double min, double max) {
        this.lightTargetOffsetMin = min;
        this.lightTargetOffsetMax = max;
        return this;
    }

    public LightEmitter setOffsetRange(double min, double max) {
        this.offsetMin = min;
        this.offsetMax = max;
        return this;
    }

    public LightEmitter setSampStepRange(int min, int max) {
        this.sampStepMin = min;
        this.sampStepMax = max;
        return this;
    }

    public LightEmitter setAtt(double att) {
        this.att = att;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(lightTargetOffsetMin);
        buf.writeDouble(lightTargetOffsetMax);
        buf.writeDouble(att);
    }

    public static LightEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double targetMin = buf.readDouble();
        double targetMax = buf.readDouble();
        double att = buf.readDouble();

        LightEmitter emitter = new LightEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.lightTargetOffsetMin = targetMin;
        emitter.lightTargetOffsetMax = targetMax;
        emitter.att = att;
        if (canceled) {
            emitter.cancel();
        }
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = position();

        // Generate a new lightning bolt each tick
        double targetDist = randomBetween(lightTargetOffsetMin, lightTargetOffsetMax);
        Vec3 target = randomUnitVector().scale(targetDist);
        int steps = random.nextInt(sampStepMin, sampStepMax + 1);
        double offset = randomBetween(offsetMin, offsetMax);

        generateBolt(center, target, steps, offset);

        // Tick and render existing bolts
        Iterator<BoltState> it = bolts.iterator();
        while (it.hasNext()) {
            BoltState bolt = it.next();
            bolt.age++;
            if (bolt.age > bolt.maxAge) {
                it.remove();
                continue;
            }

            float progress = (float) bolt.age / bolt.maxAge;
            float size = Mth.clamp(0.15f * (1.0f - progress * 0.6f), 0.05f, 0.5f);
            DustParticleOptions dust = new DustParticleOptions(BOLT_COLOR.getColor(), size);

            for (Vec3 point : bolt.points) {
                serverLevel.sendParticles(dust,
                        point.x, point.y, point.z,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        // Ambient glow at center
        if (getTick() % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    center.x, center.y + 0.1, center.z,
                    2, 0.05, 0.05, 0.05, 0.0);
        }

        // Cap active bolts
        if (bolts.size() > 64) {
            bolts.subList(0, bolts.size() - 64).clear();
        }
    }

    private void generateBolt(Vec3 center, Vec3 target, int steps, double offset) {
        List<Vec3> points = new ArrayList<>(steps);
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            Vec3 lerped = center.add(target.scale(t));
            // Attenuation factor: stronger near center, weaker at ends
            double attFactor = att * Math.sin(Math.PI * t);
            Vec3 noise = randomUnitVector().scale(offset * attFactor);
            points.add(lerped.add(noise));
        }
        bolts.add(new BoltState(points, random.nextInt(3, 8)));
    }

    private Vec3 randomUnitVector() {
        while (true) {
            double x = randomBetween(-1.0, 1.0);
            double y = randomBetween(-1.0, 1.0);
            double z = randomBetween(-1.0, 1.0);
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) {
                continue;
            }
            return new Vec3(x, y, z).normalize();
        }
    }

    private double randomBetween(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static final class BoltState {
        private final List<Vec3> points;
        private int age;
        private final int maxAge;

        private BoltState(List<Vec3> points, int maxAge) {
            this.points = points;
            this.maxAge = maxAge;
        }
    }
}
