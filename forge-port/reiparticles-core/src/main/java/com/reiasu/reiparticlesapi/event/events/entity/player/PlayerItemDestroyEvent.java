// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerItemDestroyEvent extends PlayerEvent {
    private final ItemStack original;

    public PlayerItemDestroyEvent(Player player, ItemStack original) {
        super(player);
        this.original = original;
    }

    public ItemStack getOriginal() {
        return original;
    }
}

