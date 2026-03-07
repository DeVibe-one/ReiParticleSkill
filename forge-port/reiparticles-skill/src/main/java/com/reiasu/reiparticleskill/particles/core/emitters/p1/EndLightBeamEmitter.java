// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

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

import java.util.UUID;

/**
 * End light beam emitter that travels from source toward a target position,
 * spawning a trail of particles. Used in end respawn sequences.
 * Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EndLightBeamEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "end_light_beam");

    private static final DustParticleOptions BEAM_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.6f);

    private final RandomSource random = RandomSource.create();

    private Vec3 targetPos = Vec3.ZERO;
    private double movementSpeed = 5.0;
    private int particleMinAge = 10;
    private int particleMaxAge = 20;
    private int countMin = 10;
    private int countMax = 30;

    public EndLightBeamEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(200);
    }

    public EndLightBeamEmitter setTargetPos(Vec3 targetPos) {
        this.targetPos = targetPos;
        return this;
    }

    public EndLightBeamEmitter setMovementSpeed(double speed) {
        this.movementSpeed = speed;
        return this;
    }

    public EndLightBeamEmitter setParticleAgeRange(int min, int max) {
        this.particleMinAge = min;
        this.particleMaxAge = max;
        return this;
    }

    public EndLightBeamEmitter setCountRange(int min, int max) {
        this.countMin = min;
        this.countMax = max;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(targetPos.x);
        buf.writeDouble(targetPos.y);
        buf.writeDouble(targetPos.z);
        buf.writeDouble(movementSpeed);
    }

    public static EndLightBeamEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double speed = buf.readDouble();

        EndLightBeamEmitter emitter = new EndLightBeamEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.targetPos = target;
        emitter.movementSpeed = speed;
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

        Vec3 currentPos = position();

        // Move toward target
        Vec3 dir = targetPos.subtract(currentPos);
        double dist = dir.length();
        if (dist < 1.0E-6) {
            cancel();
            return;
        }
        dir = dir.normalize();
        Vec3 movement = dir.scale(movementSpeed);

        // Check if we'd overshoot
        Vec3 nextPos = currentPos.add(movement);
        if (targetPos.subtract(nextPos).dot(dir) < 0.0) {
            cancel();
            return;
        }
        teleportTo(nextPos);

        if (currentPos.distanceTo(targetPos) <= 1.0) {
            cancel();
            return;
        }

        // Spawn trail particles
        int count = random.nextInt(countMin, countMax + 1);
        for (int i = 0; i < count; i++) {
            Vec3 randomOffset = randomSmallVec(0.03);
            Vec3 dirVel = dir.scale(random.nextDouble() * 0.1);
            Vec3 spawnOffset = randomSmallVec(0.2);
            Vec3 particlePos = currentPos.add(spawnOffset);

            int age = random.nextInt(particleMinAge, particleMaxAge + 1);
            float size = Mth.clamp(0.3f + random.nextFloat() * 0.4f, 0.1f, 1.0f);

            serverLevel.sendParticles(
                    new DustParticleOptions(BEAM_COLOR.getColor(), size),
                    particlePos.x, particlePos.y, particlePos.z,
                    1, randomOffset.x + dirVel.x, randomOffset.y + dirVel.y,
                    randomOffset.z + dirVel.z, 0.0);
        }

        // Ambient glow at head
        if (getTick() % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    currentPos.x, currentPos.y + 0.1, currentPos.z,
                    2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private Vec3 randomSmallVec(double scale) {
        return new Vec3(
                (random.nextDouble() * 2 - 1) * scale,
                (random.nextDouble() * 2 - 1) * scale,
                (random.nextDouble() * 2 - 1) * scale
        );
    }
}
