// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.util;

import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.SwordLightDisplay;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class SwordFormationHelper {
    public static final SwordFormationHelper INSTANCE = new SwordFormationHelper();

    private SwordFormationHelper() {
    }

    public void attackEntityFormation1(LivingEntity target, Player attacker) {
        if (target == null || attacker == null) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        DamageSource source = attacker.damageSources().playerAttack(attacker);
        target.hurt(source, 5.0F);

        BarrageItemDisplay display = new BarrageItemDisplay(attacker.position());
        display.setScale(0.0F);
        display.setPreScale(0.0F);
        display.setTargetScale(2.0F);
        display.setScaledSpeed(0.5F);
        Vec3 initial = target.getEyePosition().subtract(attacker.position());
        if (initial.lengthSqr() > 1.0E-8) {
            initial = initial.normalize();
        }
        initial = initial.scale(-1.0).add(
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3,
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.2,
                (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.3
        );
        display.setVelocity(initial);
        display.setItem(new ItemStack(Items.IRON_SWORD));
        display.setRotateSpeed(30.0F);
        DisplayEntityManager.INSTANCE.spawn(display, level);

        double orbitRadius = 0.4 + ThreadLocalRandom.current().nextDouble(-0.3, 0.8);
        double orbitSpeed = ThreadLocalRandom.current().nextDouble(0.5, 0.8) * (ThreadLocalRandom.current().nextBoolean() ? 1.0 : -1.0);
        double pullGain = 1.5 + ThreadLocalRandom.current().nextDouble(0.3, 0.4);
        double maxPull = 2.8;
        double maxSpeed = 4.0;
        double targetYOffset = ThreadLocalRandom.current().nextDouble(0.0, 1.5);

        ReiScheduler.INSTANCE.runTask(
                ThreadLocalRandom.current().nextInt(10, 15),
                () -> {
                    ReiScheduler.TickRunnable runnable = ReiScheduler.INSTANCE.runTaskTimer(
                            4,
                            () -> driveOrbit(display, target, targetYOffset, orbitRadius, pullGain, maxPull, orbitSpeed, maxSpeed, attacker, source)
                    );
                    runnable.setCancelPredicate(it -> !display.getValid() || !target.isAlive());
                    runnable.setFinishCallback(display::cancel);
                }
        );

        Vec3 center = target.getEyePosition();
        Vec3 from = attacker.getEyePosition();
        Vec3 to = center.subtract(from);
        int slices = 28;
        for (int i = 0; i <= slices; i++) {
            double t = i / (double) slices;
            Vec3 p = from.add(to.scale(t));
            level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 2, 0.01, 0.01, 0.01, 0.01);
        }

        int ring = 42;
        double radius = 0.8 + ThreadLocalRandom.current().nextDouble(0.25);
        for (int i = 0; i < ring; i++) {
            double angle = (Math.PI * 2.0 * i) / ring;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.END_ROD, x, center.y - 0.25, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public void attackEntityFormation2(LivingEntity target, Player attacker) {
        if (target == null || attacker == null) {
            return;
        }
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        DamageSource source = attacker.damageSources().playerAttack(attacker);
        target.hurt(source, 6.0F);

        Vec3 randomVec = new Vec3(
                (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * ThreadLocalRandom.current().nextDouble(18.0, 28.0),
                (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * ThreadLocalRandom.current().nextDouble(18.0, 28.0),
                (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * ThreadLocalRandom.current().nextDouble(18.0, 28.0)
        );
        Vec3 displayPos = target.getEyePosition().add(0.0, -1.0, 0.0).add(randomVec);
        SwordLightDisplay display = new SwordLightDisplay(displayPos);
        display.setMaxAge(3);
        display.setEnd(target.position().subtract(randomVec));
        DisplayEntityManager.INSTANCE.spawn(display, level);

        Vec3 center = target.getEyePosition().add(0.0, -1.0, 0.0);
        for (int i = 0; i < 18; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
            double radius = ThreadLocalRandom.current().nextDouble(2.0, 5.0);
            Vec3 from = center.add(Math.cos(angle) * radius, ThreadLocalRandom.current().nextDouble(0.2, 1.2), Math.sin(angle) * radius);
            Vec3 dir = center.subtract(from);
            int samples = 14;
            for (int s = 0; s <= samples; s++) {
                double t = s / (double) samples;
                Vec3 p = from.add(dir.scale(t));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void driveOrbit(
            BarrageItemDisplay display,
            LivingEntity target,
            double targetYOffset,
            double orbitRadius,
            double pullGain,
            double maxPull,
            double orbitSpeed,
            double maxSpeed,
            Player attacker,
            DamageSource source
    ) {
        if (display.getAge() > 150) {
            display.cancel();
            return;
        }

        Vec3 offsetTarget = target.position().add(0.0, targetYOffset, 0.0);
        Vec3 toTarget = offsetTarget.subtract(display.getPos());
        double distSq = toTarget.lengthSqr();
        if (distSq > 1.0E-8) {
            double dist = Math.sqrt(distSq);
            Vec3 radial = toTarget.normalize();
            Vec3 tangent = radial.cross(new Vec3(0.0, 1.0, 0.0));
            if (tangent.lengthSqr() < 1.0E-8) {
                tangent = radial.cross(new Vec3(1.0, 0.0, 0.0));
            }
            tangent = tangent.normalize();
            double radiusError = dist - orbitRadius;
            double pull = Math.max(-maxPull, Math.min(maxPull, radiusError * pullGain));
            Vec3 desired = tangent.scale(orbitSpeed).add(radial.scale(pull));
            Vec3 velocity = display.getVelocity().scale(0.5).add(desired.scale(0.8));
            if (velocity.length() > maxSpeed) {
                velocity = velocity.normalize().scale(maxSpeed);
            }
            display.setVelocity(velocity);
        }

        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(8.0),
                it -> it != attacker
        )) {
            target.hurt(source, 5.0F);
            display.setAge(display.getAge() + 60);
            break;
        }
    }
}
