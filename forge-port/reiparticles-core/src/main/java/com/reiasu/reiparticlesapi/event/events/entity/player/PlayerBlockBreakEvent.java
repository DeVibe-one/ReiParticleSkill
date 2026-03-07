// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity.player;

import com.reiasu.reiparticlesapi.event.api.EventCancelable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PlayerBlockBreakEvent extends PlayerEvent implements EventCancelable {
    private final Level world;
    private final BlockPos pos;
    private final BlockState state;
    private boolean cancelled;

    public PlayerBlockBreakEvent(Player player, Level world, BlockPos pos, BlockState state) {
        super(player);
        this.world = world;
        this.pos = pos;
        this.state = state;
    }

    public Level getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
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

