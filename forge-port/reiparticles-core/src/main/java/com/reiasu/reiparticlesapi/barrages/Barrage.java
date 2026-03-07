// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

import com.reiasu.reiparticlesapi.network.particle.ServerController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A projectile-like entity that travels in a direction, detects hits against
 * blocks, entities, and other barrages, and can pass through targets.
 * <p>
 * Implementations should use {@link AbstractBarrage} as a base class.
 */
public interface Barrage {

    /**
     * Current world position.
     */
    Vec3 getLoc();

    void setLoc(Vec3 loc);

    /**
     * The server world this barrage exists in.
     */
    ServerLevel getWorld();

    /**
     * The axis-aligned hit box for collision detection.
     */
    HitBox getHitBox();

    void setHitBox(HitBox hitBox);

    /**
     * The entity that shot this barrage (may be null).
     */
    @Nullable
    LivingEntity getShooter();

    void setShooter(@Nullable LivingEntity shooter);

    /**
     * The movement direction vector.
     */
    Vec3 getDirection();

    void setDirection(Vec3 direction);

    /**
     * Whether this barrage has been launched (is actively ticking/moving).
     */
    boolean getLaunch();

    void setLaunch(boolean launch);

    /**
     * Whether this barrage is still valid (not yet removed).
     */
    boolean getValid();

    /**
     * Barrage configuration options.
     */
    BarrageOption getOptions();

    /**
     * Unique identifier for this barrage instance.
     */
    UUID getUuid();

    /**
     * The server-side particle/display controller bound to this barrage.
     */
    ServerController<?> getBindControl();

    /**
     * Called when a hit is detected. Implementations should invoke
     * {@link #onHit(BarrageHitResult)} then handle removal/pass-through logic.
     */
    void hit(BarrageHitResult result);

    /**
     * User-overridable callback when a hit occurs.
     */
    void onHit(BarrageHitResult result);

    /**
     * Whether this barrage currently ignores collisions
     * (e.g. during initial no-hitbox grace period).
     */
    boolean noclip();

    /**
     * Called each server tick to update position, detect hits, etc.
     */
    void tick();
}
