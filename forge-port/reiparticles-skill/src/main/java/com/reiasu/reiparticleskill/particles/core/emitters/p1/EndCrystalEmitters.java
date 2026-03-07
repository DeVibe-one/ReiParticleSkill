// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
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
import java.util.List;
import java.util.UUID;

/**
 * End crystal emitter that travels from a summon position toward a target,
 * spawning particles in a rotating ring whose radius expands and contracts
 * based on distance progress. Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EndCrystalEmitters extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "end_crystal");

    private static final DustParticleOptions CRYSTAL_COLOR =
            new DustParticleOptions(new Vector3f(0.45f, 0.15f, 0.75f), 0.5f);

    private final RandomSource random = RandomSource.create();

    private Vec3 target = Vec3.ZERO;
    private Vec3 summonPos = Vec3.ZERO;
    private double movementSpeed = 3.0;
    private double maxRadius = 6.0;
    private int particleMinAge = 10;
    private int particleMaxAge = 30;
    private int countMin = 8;
    private int countMax = 16;
    private double rotationSpeed = 0.15;
    private double currentRotation = 0.0;
    private double lastRadius = 0.0;

    public EndCrystalEmitters(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        this.summonPos = spawn;
        setMaxTick(400);
    }

    public EndCrystalEmitters setTarget(Vec3 target) {
        this.target = target;
        return this;
    }

    public EndCrystalEmitters setSummonPos(Vec3 summonPos) {
        this.summonPos = summonPos;
        return this;
    }

    public EndCrystalEmitters setMovementSpeed(double speed) {
        this.movementSpeed = speed;
        return this;
    }

    public EndCrystalEmitters setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
        return this;
    }

    public EndCrystalEmitters setCountRange(int min, int max) {
        this.countMin = min;
        this.countMax = max;
        return this;
    }

    public EndCrystalEmitters setRotationSpeed(double rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(target.x);
        buf.writeDouble(target.y);
        buf.writeDouble(target.z);
        buf.writeDouble(summonPos.x);
        buf.writeDouble(summonPos.y);
        buf.writeDouble(summonPos.z);
        buf.writeDouble(movementSpeed);
        buf.writeDouble(maxRadius);
        buf.writeDouble(rotationSpeed);
    }

    public static EndCrystalEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 tgt = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 summon = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double spd = buf.readDouble();
        double maxR = buf.readDouble();
        double rotSpd = buf.readDouble();

        EndCrystalEmitters emitter = new EndCrystalEmitters(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.target = tgt;
        emitter.summonPos = summon;
        emitter.movementSpeed = spd;
        emitter.maxRadius = maxR;
        emitter.rotationSpeed = rotSpd;
        if (canceled) emitter.cancel();
        return emitter;
    }

    /**
     * Computes the current ring radius based on distance progress.
     * Radius peaks at 50% travel, shrinks to zero at endpoints.
     */
    public double getCurrentRadius() {
        double originalDistance = summonPos.distanceTo(target);
        if (originalDistance < 0.01) return 0.0;
        double distance = position().distanceTo(target);
        if (distance <= 0.5) return 0.0;

        double progress = 1.0 - distance / originalDistance;
        double stepTo = Math.abs(progress - 0.5) * 2.0;
        return GraphMathHelper.lerp((float) stepTo, maxRadius, 0.0);
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 currentPos = position();

        // Move toward target
        Vec3 dir = target.subtract(currentPos);
        double dist = dir.length();
        if (dist < 0.01) {
            cancel();
            return;
        }
        dir = dir.normalize();
        Vec3 movement = dir.scale(movementSpeed);

        // Check overshoot
        Vec3 nextPos = currentPos.add(movement);
        if (target.subtract(nextPos).dot(dir) < 0.0) {
            cancel();
            return;
        }
        teleportTo(nextPos);

        if (position().distanceTo(target) <= 0.4) {
            cancel();
            return;
        }

        // Update rotation
        lastRadius = getCurrentRadius();
        currentRotation += rotationSpeed;
        double currentRadius = getCurrentRadius();

        // Spawn ring particles at current rotation
        int count = random.nextInt(countMin, countMax + 1);
        // Build ring points rotated to face the travel direction
        List<Vec3> ringPositions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = currentRotation + (Math.PI * 2.0 * i / count);
            double r = currentRadius;
            // Base ring in XZ plane
            double rx = Math.cos(angle) * r;
            double rz = Math.sin(angle) * r;

            // Add random jitter
            double jx = (random.nextDouble() * 2 - 1) * 0.03;
            double jy = (random.nextDouble() * 2 - 1) * 0.03;
            double jz = (random.nextDouble() * 2 - 1) * 0.03;

            // Rotate ring to face travel direction
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 right = dir.cross(up).normalize();
            if (right.lengthSqr() < 1.0E-6) {
                right = new Vec3(1, 0, 0);
            }
            Vec3 realUp = right.cross(dir).normalize();

            Vec3 particlePos = currentPos
                    .add(right.scale(rx))
                    .add(realUp.scale(rz))
                    .add(new Vec3(jx, jy, jz));

            ringPositions.add(particlePos);
        }

        // Render
        for (Vec3 rPos : ringPositions) {
            float size = 0.2f + random.nextFloat() * 0.3f;
            serverLevel.sendParticles(
                    new DustParticleOptions(CRYSTAL_COLOR.getColor(), size),
                    rPos.x, rPos.y, rPos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient at current pos
        if (getTick() % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    currentPos.x, currentPos.y + 0.1, currentPos.z,
                    3, 0.15, 0.1, 0.15, 0.0);
        }
    }
}

