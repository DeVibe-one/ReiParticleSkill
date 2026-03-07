// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity;

import com.reiasu.reiparticlesapi.event.api.EventCancelable;
import com.reiasu.reiparticlesapi.event.api.EventInterruptible;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class EntityMoveEvent extends EntityEvent implements EventCancelable, EventInterruptible {
    private final Vec3 movement;
    private Vec3 moveTo;
    private boolean cancelled;
    private boolean interrupted;

    public EntityMoveEvent(Entity entity, Vec3 movement, Vec3 moveTo) {
        super(entity);
        this.movement = movement;
        this.moveTo = moveTo;
    }

    public Vec3 getMovement() {
        return movement;
    }

    public Vec3 getMoveTo() {
        return moveTo;
    }

    public void setMoveTo(Vec3 moveTo) {
        this.moveTo = moveTo;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
    }

    @Override
    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }
}

