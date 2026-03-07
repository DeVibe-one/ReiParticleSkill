// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity;

import com.reiasu.reiparticlesapi.event.api.EventCancelable;
import com.reiasu.reiparticlesapi.event.api.EventInterruptible;
import net.minecraft.world.entity.Entity;

public final class EntityPreTickEvent extends EntityEvent implements EventCancelable, EventInterruptible {
    private boolean cancelled;
    private boolean interrupted;

    public EntityPreTickEvent(Entity entity) {
        super(entity);
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

