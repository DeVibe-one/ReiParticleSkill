// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordFormationExplosionEffectGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordFormationSigilEffectGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordFormationVortexEffectGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordFormationWaveEffectGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordRuneEffectGroup;
import com.reiasu.reiparticleskill.sounds.SkillSoundEvents;
import com.reiasu.reiparticleskill.util.SwordFormationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class HugeSwordFormation extends ServerOnlyDisplayGroup {
    private Player owner;
    private int age;
    private boolean playSound;

    public HugeSwordFormation(Vec3 pos, Level world, Player owner) {
        super(pos, world);
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    public boolean getPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        Map<Supplier<Object>, RelativeLocation> result = new LinkedHashMap<>();
        Vec3 direction = owner == null ? new Vec3(0.0, 0.0, 1.0) : owner.getLookAngle();
        result.put(() -> createInnerRing(direction), new RelativeLocation());
        result.put(() -> createOuterRing(direction), new RelativeLocation());
        result.put(() -> {
            SwordFormationVortexEffectGroup effect = new SwordFormationVortexEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(direction);
            effect.setRingSamples(118);
            effect.setInnerRadius(45.0);
            effect.setOuterRadius(47.0);
            return effect;
        }, new RelativeLocation(0.0, -12.0, 0.0));
        result.put(() -> {
            SwordFormationWaveEffectGroup effect = new SwordFormationWaveEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(direction);
            effect.setDelay(2);
            effect.setMinCount(16);
            effect.setMaxCount(32);
            effect.setMinParticleSpeed(0.5);
            effect.setMaxParticleSpeed(0.52);
            effect.setRadianCountMin(2);
            effect.setRadianCountMax(5);
            effect.setMinRadian(Math.PI * 0.5);
            effect.setMaxRadian(Math.PI * 2.0);
            effect.setMinParticleAge(40);
            effect.setMaxParticleAge(70);
            effect.setRandomOffsetMin(0.02);
            effect.setRandomOffsetMax(0.3);
            effect.setRadianProgressMinSpeedScale(0.4);
            effect.setRadianProgressMaxSpeedScale(1.2);
            return effect;
        }, new RelativeLocation(0.0, -12.0, 0.0));
        result.put(() -> {
            SwordFormationSigilEffectGroup effect = new SwordFormationSigilEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(direction);
            effect.setScale(1.0);
            effect.setOuterRingSamples(280);
            return effect;
        }, new RelativeLocation(0.0, 2.0, 0.0));
        result.put(() -> {
            SwordRuneEffectGroup effect = new SwordRuneEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(direction);
            effect.setOuterRadius(8.0);
            effect.setRuneScale(1.0);
            effect.setRingSamples(132);
            return effect;
        }, new RelativeLocation(0.0, 2.0, 0.0));
        return result;
    }

    @Override
    public void tick() {
        if (owner == null || !owner.isAlive()) {
            remove();
            return;
        }

        boolean settled = true;
        Vec3 direction = owner.getLookAngle();
        for (Object value : getDisplayed().keySet()) {
            if (value instanceof SwordRingFormation ring) {
                ring.setDirection(RelativeLocation.of(direction));
                if (!ring.getAllSet()) {
                    settled = false;
                }
            }
            if (value instanceof SwordFormationVortexEffectGroup effect) {
                effect.setDirection(direction);
            }
            if (value instanceof SwordFormationWaveEffectGroup effect) {
                effect.setDirection(direction);
            }
            if (value instanceof SwordFormationSigilEffectGroup effect) {
                effect.setDirection(direction);
            }
            if (value instanceof SwordRuneEffectGroup effect) {
                effect.setDirection(direction);
            }
        }

        if (settled) {
            if (!playSound) {
                playSwordSound();
                spawnExplosion(direction);
                playSound = true;
            }
            tickAttackNearby();
            age++;
            if (age > 600) {
                remove();
                return;
            }
        }

        rotateToPoint(RelativeLocation.of(direction));
        teleportTo(owner.getEyePosition().add(direction.scale(-6.0)).add(0.0, -1.0, 0.0));
    }

    @Override
    public void onDisplay() {
        age = 0;
        playSound = false;
    }

    private void playSwordSound() {
        Level level = getWorld();
        if (!(level instanceof ServerLevel serverLevel) || !SkillSoundEvents.SWORD_FORMATION.isPresent()) {
            return;
        }
        Vec3 pos = getPos();
        serverLevel.playSound(
                null,
                pos.x,
                pos.y,
                pos.z,
                SkillSoundEvents.SWORD_FORMATION.get(),
                SoundSource.PLAYERS,
                2.5F,
                1.0F
        );
    }

    private void spawnExplosion(Vec3 direction) {
        if (getWorld() == null) {
            return;
        }
        SwordFormationExplosionEffectGroup explosion = new SwordFormationExplosionEffectGroup(getPos(), getWorld());
        explosion.setDirection(direction);
        ServerDisplayGroupManager.INSTANCE.spawn(explosion);
    }

    private void tickAttackNearby() {
        Level level = getWorld();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB range = new AABB(getPos(), getPos()).inflate(96.0);
        List<LivingEntity> entities = new ArrayList<>(serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                range,
                entity -> entity.isAlive() && entity != owner
        ));
        if (entities.isEmpty()) {
            return;
        }
        java.util.Collections.shuffle(entities);
        int limit = Math.min(3, entities.size());
        for (int i = 0; i < limit; i++) {
            SwordFormationHelper.INSTANCE.attackEntityFormation1(entities.get(i), owner);
        }
    }

    private SwordRingFormation createInnerRing(Vec3 direction) {
        List<RelativeLocation> points = new ArrayList<>();
        points.addAll(new PointsBuilder().addCircle(18.0, 18).createWithoutClone());
        points.addAll(new PointsBuilder().addCircle(32.0, 32).createWithoutClone());
        java.util.Collections.shuffle(points);
        return createRingFromPoints(direction, points, Math.PI / 128.0, 0);
    }

    private SwordRingFormation createOuterRing(Vec3 direction) {
        List<RelativeLocation> points = new ArrayList<>();
        points.addAll(new PointsBuilder().addDiscreteCircleXZ(40.0, 32, 8.0).createWithoutClone());
        points.addAll(new PointsBuilder().addDiscreteCircleXZ(60.0, 128, 15.0).createWithoutClone());
        points.addAll(new PointsBuilder().addDiscreteCircleXZ(72.0, 16, 20.0).createWithoutClone());
        return createRingFromPoints(direction, points, -Math.PI / 256.0, 1);
    }

    private SwordRingFormation createRingFromPoints(
            Vec3 direction,
            List<RelativeLocation> points,
            double ringSpeed,
            int blendCount
    ) {
        Map<Supplier<Object>, RelativeLocation> map = new LinkedHashMap<>();
        for (RelativeLocation point : points) {
            RelativeLocation copy = point.copy();
            map.put(() -> createSwordDisplay(blendCount), copy);
        }

        SwordRingFormation ring = new SwordRingFormation(Vec3.ZERO, getWorld(), ringSpeed, map);
        ring.setAxis(RelativeLocation.of(direction));
        ring.setDirection(ring.getAxis());
        return ring;
    }

    private static BarrageItemDisplay createSwordDisplay(int blendCount) {
        BarrageItemDisplay display = new BarrageItemDisplay(Vec3.ZERO);
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        display.setItem(stack);
        display.setTargetScale(10.0F);
        display.setScale(0.0F);
        display.setPreScale(0.0F);
        display.setScaledSpeed(2.0F);
        display.setBlendCount(Math.max(0, blendCount));
        display.setRotateSpeed(5.0F);
        display.setDisplayTick(2);
        display.setYaw(ThreadLocalRandom.current().nextFloat() * 360.0F);
        display.setTargetYaw(display.getYaw());
        return display;
    }
}
