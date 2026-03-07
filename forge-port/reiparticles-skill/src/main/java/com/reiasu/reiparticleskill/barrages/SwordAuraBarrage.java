// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.barrages;

import com.reiasu.reiparticlesapi.barrages.AbstractBarrage;
import com.reiasu.reiparticlesapi.barrages.BarrageHitResult;
import com.reiasu.reiparticlesapi.barrages.BarrageOption;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SwordAuraBarrage extends AbstractBarrage {
    private static final DustParticleOptions MAIN_COLOR =
            new DustParticleOptions(new Vector3f(0.02F, 0.53F, 0.38F), 1.15F);
    private static final DustParticleOptions SUB_COLOR =
            new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.42F), 1.0F);

    private final AuraController controller;

    public SwordAuraBarrage(Vec3 loc, ServerLevel world, HitBox hitBox, BarrageOption options) {
        this(new AuraController(world, loc), loc, world, hitBox, options);
    }

    private SwordAuraBarrage(
            AuraController controller,
            Vec3 loc,
            ServerLevel world,
            HitBox hitBox,
            BarrageOption options
    ) {
        super(loc, world, hitBox, controller, options);
        this.controller = controller;
    }

    @Override
    public void tick() {
        if (!getValid()) {
            return;
        }
        super.tick();
        if (!getValid()) {
            return;
        }
        controller.setPosition(getLoc());
        controller.setDirection(getDirection());
        controller.tick();
        spawnWake();
    }

    @Override
    public boolean filterHitEntity(LivingEntity entity) {
        return entity != null && entity.isAlive() && entity != getShooter();
    }

    @Override
    public void onHit(BarrageHitResult result) {
        if (result == null) {
            return;
        }
        ServerLevel world = getWorld();
        Vec3 pos = getLoc();
        Vec3 push = getDirection().lengthSqr() > 1.0E-8
                ? getDirection().normalize().scale(Math.max(1.0, getOptions().getSpeed()))
                : Vec3.ZERO;

        DamageSource source = buildDamageSource();
        for (LivingEntity living : result.getEntities()) {
            living.hurt(source, 30.0F);
            living.invulnerableTime = 0;
            living.setDeltaMovement(push);
            living.hurtMarked = true;
        }

        world.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 8, 0.25, 0.25, 0.25, 0.02);
        world.sendParticles(MAIN_COLOR, pos.x, pos.y, pos.z, 42, 1.25, 1.25, 1.25, 0.02);
        world.sendParticles(SUB_COLOR, pos.x, pos.y, pos.z, 24, 1.0, 1.0, 1.0, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.15F);
    }

    @Override
    public void remove() {
        super.remove();
        controller.cancel();
    }

    private DamageSource buildDamageSource() {
        LivingEntity shooter = getShooter();
        if (shooter instanceof Player player) {
            return getWorld().damageSources().playerAttack(player);
        }
        if (shooter != null) {
            return getWorld().damageSources().mobAttack(shooter);
        }
        return getWorld().damageSources().magic();
    }

    private void spawnWake() {
        ServerLevel world = getWorld();
        Vec3 pos = getLoc();
        Vec3 dir = getDirection().lengthSqr() > 1.0E-8 ? getDirection().normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 right = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-8) {
            right = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        right = right.normalize();

        int ring = 12;
        for (int i = 0; i < ring; i++) {
            double angle = (Math.PI * 2.0 * i) / ring + getOptions().getSpeed() * 0.06;
            Vec3 offset = right.scale(Math.cos(angle) * 0.5).add(0.0, Math.sin(angle * 1.6) * 0.2, 0.0);
            Vec3 p = pos.add(offset);
            world.sendParticles(MAIN_COLOR, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 3 == 0) {
                world.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static final class AuraController implements ServerController<AuraController> {
        private final ServerLevel world;
        private Vec3 position;
        private Vec3 direction = new Vec3(0.0, 0.0, 1.0);
        private boolean canceled;
        private int tick;

        private AuraController(ServerLevel world, Vec3 position) {
            this.world = world;
            this.position = position == null ? Vec3.ZERO : position;
        }

        @Override
        public void tick() {
            if (canceled) {
                return;
            }
            tick++;
            if (tick % 2 != 0) {
                return;
            }
            Vec3 dir = direction.lengthSqr() > 1.0E-8 ? direction.normalize() : new Vec3(0.0, 0.0, 1.0);
            Vec3 p = position.subtract(dir.scale(0.35));
            world.sendParticles(SUB_COLOR, p.x, p.y, p.z, 2, 0.04, 0.04, 0.04, 0.0);
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }

        @Override
        public boolean getCanceled() {
            return canceled;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public void teleportTo(net.minecraft.world.phys.Vec3 pos) {
            this.position = pos == null ? Vec3.ZERO : pos;
        }

        private void setPosition(Vec3 position) {
            this.position = position == null ? Vec3.ZERO : position;
        }

        private void setDirection(Vec3 direction) {
            this.direction = direction == null ? Vec3.ZERO : direction;
        }
    }
}
