// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

/**
 * Configuration options for a barrage (projectile) entity.
 * Controls collision behavior, speed, acceleration, and lifetime.
 */
public final class BarrageOption {

    /** Gravity constant used by barrages. */
    public static final double G = 0.0245;

    private boolean acrossBlock = false;
    private boolean acrossEmptyCollisionShape = true;
    private boolean barrageIgnored = true;
    private boolean acrossLiquid = true;
    private boolean acrossable = false;
    private int maxAcrossCount = -1;
    private int maxLivingTick = -1;
    private int noneHitBoxTick = 3;
    private boolean enableSpeed = false;
    private double speed = -1.0;
    private double acceleration = 0.0;
    private boolean accelerationMaxSpeedEnabled = false;
    private double accelerationMaxSpeed = 1.0;

    // ---- Getters / Setters ----

    public boolean isAcrossBlock() { return acrossBlock; }
    public void setAcrossBlock(boolean v) { this.acrossBlock = v; }

    public boolean isAcrossEmptyCollisionShape() { return acrossEmptyCollisionShape; }
    public void setAcrossEmptyCollisionShape(boolean v) { this.acrossEmptyCollisionShape = v; }

    public boolean isBarrageIgnored() { return barrageIgnored; }
    public void setBarrageIgnored(boolean v) { this.barrageIgnored = v; }

    public boolean isAcrossLiquid() { return acrossLiquid; }
    public void setAcrossLiquid(boolean v) { this.acrossLiquid = v; }

    public boolean isAcrossable() { return acrossable; }
    public void setAcrossable(boolean v) { this.acrossable = v; }

    public int getMaxAcrossCount() { return maxAcrossCount; }
    public void setMaxAcrossCount(int v) { this.maxAcrossCount = v; }

    public int getMaxLivingTick() { return maxLivingTick; }
    public void setMaxLivingTick(int v) { this.maxLivingTick = v; }

    public int getNoneHitBoxTick() { return noneHitBoxTick; }
    public void setNoneHitBoxTick(int v) { this.noneHitBoxTick = v; }

    public boolean isEnableSpeed() { return enableSpeed; }
    public void setEnableSpeed(boolean v) { this.enableSpeed = v; }

    public double getSpeed() { return speed; }
    public void setSpeed(double v) { this.speed = v; }

    public double getAcceleration() { return acceleration; }
    public void setAcceleration(double v) { this.acceleration = v; }

    public boolean isAccelerationMaxSpeedEnabled() { return accelerationMaxSpeedEnabled; }
    public void setAccelerationMaxSpeedEnabled(boolean v) { this.accelerationMaxSpeedEnabled = v; }

    public double getAccelerationMaxSpeed() { return accelerationMaxSpeed; }
    public void setAccelerationMaxSpeed(double v) { this.accelerationMaxSpeed = v; }
}
