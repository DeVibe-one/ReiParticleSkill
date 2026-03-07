// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.util;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.barrages.BarrageOption;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticleskill.barrages.SwordAuraBarrage;
import com.reiasu.reiparticleskill.display.LightFlashDisplay;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import com.reiasu.reiparticleskill.display.group.impl.formation.HugeSword2CoreFormation;
import com.reiasu.reiparticleskill.display.group.impl.formation.HugeSwordFormation;
import com.reiasu.reiparticleskill.sounds.SkillSoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class SwordLightEnchantUtil {
    public static final SwordLightEnchantUtil INSTANCE = new SwordLightEnchantUtil();

    private static final Logger LOGGER = LogUtils.getLogger();

    private SwordLightEnchantUtil() {
    }

    public static void shoot(Player user, ItemStack stack) {
        if (user == null || stack == null) {
            return;
        }
        if (!(user.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 start = user.getEyePosition();
        Vec3 direction = user.getLookAngle().normalize();

        if (spawnSwordAuraBarrage(user, level, start, direction)) {
            playFormationSound(level, start.add(direction.scale(1.6)));
            return;
        }

        double range = 24.0;
        Vec3 end = start.add(direction.scale(range));

        int points = 56;
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            Vec3 pos = start.add(end.subtract(start).scale(t));
            level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.015, 0.015, 0.015, 0.01);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.01);
            }
        }
        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);

        DamageSource source = user.damageSources().playerAttack(user);
        AABB aabb = new AABB(start, end).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, aabb, it -> it.isAlive() && it != user)) {
            if (distanceToSegment(target.getEyePosition(), start, end) <= 1.2) {
                target.hurt(source, 6.0F);
            }
        }
        playFormationSound(level, end);
    }

    public static void placeSwordFormation(Player user, ItemStack stack) {
        if (user == null || stack == null) {
            return;
        }
        if (!(user.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = user.getEyePosition();
        ServerDisplayGroupManager.INSTANCE.spawn(new HugeSwordFormation(center, level, user));
        playFormationSound(level, center);
    }

    public static void placeSwordFormation2(Player user, ItemStack stack) {
        if (user == null || stack == null) {
            return;
        }
        if (!(user.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = user.position().add(0.0, 60.0, 0.0);
        ServerDisplayGroupManager.INSTANCE.spawn(new HugeSword2CoreFormation(center, level, user));

        Vec3 horizontalLook = user.getLookAngle().multiply(1.0, 0.0, 1.0);
        LightFlashDisplay display = new LightFlashDisplay(user.getEyePosition().add(horizontalLook));
        display.setMaxAge(15);
        display.setBloomCount(5);
        DisplayEntityManager.INSTANCE.spawn(display, level);
        playFormationSound(level, center);
    }

    private static double distanceToSegment(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 1.0E-8) {
            return point.distanceTo(start);
        }
        double projection = point.subtract(start).dot(segment) / lengthSquared;
        double clamped = Math.max(0.0, Math.min(1.0, projection));
        Vec3 nearest = start.add(segment.scale(clamped));
        return point.distanceTo(nearest);
    }

    private static void playFormationSound(ServerLevel level, Vec3 pos) {
        if (level == null || pos == null || !SkillSoundEvents.SWORD_FORMATION.isPresent()) {
            return;
        }
        level.playSound(
                null,
                pos.x,
                pos.y,
                pos.z,
                SkillSoundEvents.SWORD_FORMATION.get(),
                SoundSource.PLAYERS,
                0.75F,
                1.0F
        );
    }

    private static boolean spawnSwordAuraBarrage(Player user, ServerLevel level, Vec3 start, Vec3 direction) {
        try {
            BarrageOption options = new BarrageOption();
            options.setMaxLivingTick(40);
            options.setEnableSpeed(true);
            options.setSpeed(0.5);
            options.setAcceleration(0.1);
            options.setAccelerationMaxSpeedEnabled(false);
            options.setAcrossBlock(true);
            options.setAcrossLiquid(true);
            options.setAcrossable(true);
            options.setMaxAcrossCount(4);
            options.setNoneHitBoxTick(3);
            options.setAcrossEmptyCollisionShape(true);

            SwordAuraBarrage barrage = new SwordAuraBarrage(start, level, HitBox.of(3.4, 3.0, 3.4), options);
            barrage.setShooter(user);
            barrage.setDirection(direction);
            barrage.setLaunch(true);
            SkillBarrageManager.INSTANCE.spawn(barrage);
            return true;
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to spawn sword aura barrage for {}", user.getScoreboardName(), e);
            return false;
        }
    }
}
