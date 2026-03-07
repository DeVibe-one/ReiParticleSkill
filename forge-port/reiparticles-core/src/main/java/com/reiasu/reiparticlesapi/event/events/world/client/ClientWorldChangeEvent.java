// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.world.client;

import com.reiasu.reiparticlesapi.event.events.world.WorldEvent;
import net.minecraft.world.level.Level;

public final class ClientWorldChangeEvent extends WorldEvent {
    public ClientWorldChangeEvent(Level world) {
        super(world);
    }
}

