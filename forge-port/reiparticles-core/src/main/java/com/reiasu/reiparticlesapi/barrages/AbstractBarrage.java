// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

import com.reiasu.reiparticlesapi.network.particle.ServerController;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Base implementation of {@link Barrage} providing standard projectile
 * movement, collision detection against blocks/entities/barrages, and
 * lifetime/pass-through management.
 * <p>
 * Subclasses must implement:
 * <ul>
 *   <li>{@link #filterHitEntity(LivingEntity)} —which entities this barrage can hit</li>
 *   <li>{@link #onHit(BarrageHitResult)} —what happens on impact</li>
 * </ul>
 * <p>
 * Forge port: barrage-barrage collision is now fully implemented via {@link BarrageManager}.
 */
public abstract class AbstractBarrage implements Barrage {

    private Vec3 loc;
    private final ServerLevel world;
    private HitBox hitBox;
    private final ServerController<?> bindControl;
    private final BarrageOption options;

    @Nullable
    private LivingEntity shooter;
    private Vec3 direction;
    private boolean launch;
    private int currentTick;
    private int spawnTick;
    private boolean isValid;
    private int currentAcrossCount;
    private boolean isBeingHit;
    private final UUID uuid;

    protected AbstractBarrage(Vec3 loc, ServerLevel world, HitBox hitBox,
                              ServerController<?> bindControl, BarrageOption options) {
        this.loc = loc;
        this.world = world;
        this.hitBox = hitBox;
        this.bindControl = bindControl;
        this.options = options;
        this.direction = Vec3.ZERO;
        this.isValid = true;
        this.uuid = UUID.randomUUID();
    }

    // ---- Barrage interface implementation ----

    @Override
    public Vec3 getLoc() { return loc; }

    @Override
    public void setLoc(Vec3 loc) { this.loc = loc; }

    @Override
    public ServerLevel getWorld() { return world; }

    @Override
    public HitBox getHitBox() { return hitBox; }

    @Override
    public void setHitBox(HitBox hitBox) { this.hitBox = hitBox; }

    @Override
    public ServerController<?> getBindControl() { return bindControl; }

    @Override
    public BarrageOption getOptions() { return options; }

    @Override
    @Nullable
    public LivingEntity getShooter() { return shooter; }

    @Override
    public void setShooter(@Nullable LivingEntity shooter) { this.shooter = shooter; }

    @Override
    public Vec3 getDirection() { return direction; }

    @Override
    public void setDirection(Vec3 direction) { this.direction = direction; }

    @Override
    public boolean getLaunch() { return launch; }

    @Override
    public void setLaunch(boolean launch) { this.launch = launch; }

    @Override
    public boolean getValid() { return isValid; }

    @Override
    public UUID getUuid() { return uuid; }

    // ---- Abstract/overridable ----

    /**
     * Return true if this barrage can hit the given entity.
     */
    public abstract boolean filterHitEntity(LivingEntity entity);

    /**
     * Return true if this barrage can collide with another barrage.
     * Default: collides with barrages from a different shooter.
     */
    public boolean filterHitBarrage(Barrage barrage) {
        return !Objects.equals(barrage.getShooter(), this.getShooter())
                && barrage != this;
    }

    /**
     * Returns the position used for the bound controller's teleport.
     * Override to offset from {@link #getLoc()} if needed.
     */
    public Vec3 getControllerLocation() {
        return getLoc();
    }

    // ---- Tick ----

    @Override
    public void tick() {
        if (!getLaunch() || !getValid()) {
            return;
        }

        // Skip if chunk not loaded
        if (!world.hasChunk((int) loc.x >> 4, (int) loc.z >> 4)) {
            return;
        }

        move();
        bindControl.teleportTo(getControllerLocation());

        // Lifetime check
        if (options.getMaxLivingTick() != -1) {
            if (currentTick++ > options.getMaxLivingTick()) {
                hit(new BarrageHitResult());
                return;
            }
        }

        BarrageHitResult result = new BarrageHitResult();
        boolean hitDetected = checkBlockCollision(result);

        // Grace period for entity/barrage hits
        if (spawnTick < options.getNoneHitBoxTick()) {
            spawnTick++;
            return;
        }

        hitDetected |= checkEntityCollision(result);
        hitDetected |= checkBarrageCollision(result);

        if (hitDetected) {
            hit(result);
        }
    }

    private void move() {
        if (options.isEnableSpeed()) {
            Vec3 moveVec = direction.normalize().scale(options.getSpeed());
            loc = loc.add(moveVec);
            options.setSpeed(options.getSpeed() + options.getAcceleration());
            if (options.isAccelerationMaxSpeedEnabled()) {
                options.setSpeed(Math.max(options.getAccelerationMaxSpeed(), options.getSpeed()));
            }
        } else {
            loc = loc.add(direction);
        }
    }

    private boolean checkBlockCollision(BarrageHitResult result) {
        boolean hitDetected = false;
        AABB aabb = hitBox.ofBox(loc);
        BlockPos minPos = new BlockPos(
                (int) Math.floor(aabb.minX),
                (int) Math.floor(aabb.minY),
                (int) Math.floor(aabb.minZ));
        BlockPos maxPos = new BlockPos(
                (int) Math.floor(aabb.maxX),
                (int) Math.floor(aabb.maxY),
                (int) Math.floor(aabb.maxZ));

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (!world.isLoaded(pos) || !world.isAreaLoaded(pos, 0)) continue;
            BlockState block = world.getBlockState(pos);
            if (block.isAir()) continue;
            if (block.liquid()) {
                if (!options.isAcrossLiquid()) {
                    result.setHitBlockState(block);
                    result.getHitBlocks().add(pos.immutable());
                    hitDetected = true;
                }
            } else {
                if (!options.isAcrossBlock()) {
                    result.setHitBlockState(block);
                    result.getHitBlocks().add(pos.immutable());
                    hitDetected = true;
                }
            }
        }
        return hitDetected;
    }

    private boolean checkEntityCollision(BarrageHitResult result) {
        Set<LivingEntity> hitEntities = hitBoxEntities();
        List<LivingEntity> filtered = hitEntities.stream()
                .filter(this::filterHitEntity)
                .collect(Collectors.toList());
        if (!filtered.isEmpty()) {
            result.getEntities().addAll(filtered);
            return true;
        }
        return false;
    }

    private boolean checkBarrageCollision(BarrageHitResult result) {
        List<Barrage> clipBarrages = BarrageManager.INSTANCE.collectClipBarrages(world, hitBox.ofBox(loc));
        List<Barrage> filteredBarrages = clipBarrages.stream()
                .filter(this::filterHitBarrage)
                .collect(Collectors.toList());
        if (!filteredBarrages.isEmpty()) {
            result.getBarrages().addAll(filteredBarrages);
            return true;
        }
        return false;
    }

    @Override
    public void hit(BarrageHitResult result) {
        if (isBeingHit) {
            return;
        }
        isBeingHit = true;
        try {
            hitInternal(result);
        } finally {
            isBeingHit = false;
        }
    }

    private void hitInternal(BarrageHitResult result) {
        onHit(result);

        boolean timeoutHit = options.getMaxLivingTick() <= currentTick
                && options.getMaxLivingTick() != -1;

        if (options.isAcrossable() && !timeoutHit) {
            if (options.getMaxAcrossCount() == -1) {
                return;
            }
            if (currentAcrossCount++ < options.getMaxAcrossCount()) {
                return;
            }
        }

        // Notify hit barrages (barrage-barrage collision chain)
        if (!result.getBarrages().isEmpty()) {
            for (Barrage barrage : result.getBarrages()) {
                if (barrage.getOptions().isBarrageIgnored()
                        || !(barrage instanceof AbstractBarrage)
                        || barrage == this) {
                    continue;
                }
                BarrageHitResult counterResult = new BarrageHitResult();
                counterResult.getBarrages().add(this);
                ((AbstractBarrage) barrage).hit(counterResult);
            }
        }

        remove();
    }


    /**
     * Removes this barrage: invalidates and removes the bound controller.
     */
    public void remove() {
        // Note: ServerController.remove() not in current interface; call cancel()
        bindControl.cancel();
        isValid = false;
    }

    @Override
    public boolean noclip() {
        return spawnTick < options.getNoneHitBoxTick();
    }

    /**
     * Collects all living entities overlapping this barrage's hit box.
     */
    public Set<LivingEntity> hitBoxEntities() {
        AABB aabb = hitBox.ofBox(loc);
        List<LivingEntity> found = world.getEntitiesOfClass(LivingEntity.class, aabb, e -> true);
        return new HashSet<>(found);
    }

    /**
     * Collects entities overlapping the hit box that pass the given filter.
     */
    public Set<LivingEntity> hitBoxEntities(Predicate<LivingEntity> filter) {
        AABB aabb = hitBox.ofBox(loc);
        List<LivingEntity> found = world.getEntitiesOfClass(LivingEntity.class, aabb, filter);
        return new HashSet<>(found);
    }
}
