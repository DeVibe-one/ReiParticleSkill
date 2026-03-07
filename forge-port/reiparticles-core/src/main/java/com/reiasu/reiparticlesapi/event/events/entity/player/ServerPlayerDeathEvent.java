// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity.player;

import com.reiasu.reiparticlesapi.event.api.EventCancelable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class ServerPlayerDeathEvent extends PlayerEvent implements EventCancelable {
    private final Level world;
    private final DamageSource source;
    private boolean cancelled;

    public ServerPlayerDeathEvent(Player player, Level world, DamageSource source) {
        super(player);
        this.world = world;
        this.source = source;
    }

    public Level getWorld() {
        return world;
    }

    public DamageSource getSource() {
        return source;
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

