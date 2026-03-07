// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.client;

import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.client.ClientPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.client.ClientPreTickEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldPreTickEvent;
import net.minecraft.client.Minecraft;

public final class ClientTickEventForwarder {
    private ClientTickEventForwarder() {
    }

    public static void onClientStartTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ReiEventBus.call(new ClientPreTickEvent(minecraft));
        if (minecraft.level != null) {
            ReiEventBus.call(new ClientWorldPreTickEvent(minecraft.level));
        }
    }

    public static void onClientEndTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ReiEventBus.call(new ClientWorldPostTickEvent(minecraft.level));
        }
        ReiEventBus.call(new ClientPostTickEvent(minecraft));
    }
}

