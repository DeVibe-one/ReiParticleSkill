// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity.player;

import net.minecraft.world.entity.player.Player;

public final class PlayerLoggedInEvent extends PlayerEvent {
    public PlayerLoggedInEvent(Player player) {
        super(player);
    }
}

