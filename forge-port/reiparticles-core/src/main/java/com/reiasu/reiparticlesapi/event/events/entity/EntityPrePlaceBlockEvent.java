// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity;

import com.reiasu.reiparticlesapi.event.api.EventCancelable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class EntityPrePlaceBlockEvent extends EntityEvent implements EventCancelable {
    private final Level world;
    private final BlockPos block;
    private final Direction direction;
    private boolean cancelled;

    public EntityPrePlaceBlockEvent(Entity entity, Level world, BlockPos block, Direction direction) {
        super(entity);
        this.world = world;
        this.block = block;
        this.direction = direction;
    }

    public Level getWorld() {
        return world;
    }

    public BlockPos getBlock() {
        return block;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

