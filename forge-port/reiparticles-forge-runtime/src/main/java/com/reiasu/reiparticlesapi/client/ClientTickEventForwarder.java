// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.client;

import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.client.ClientPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.client.ClientPreTickEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldChangeEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldPreTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class ClientTickEventForwarder {
    private static Level lastKnownLevel;

    private ClientTickEventForwarder() {
    }

    public static void onClientStartTick() {
        Minecraft minecraft = Minecraft.getInstance();
        forwardWorldChangeIfNeeded(minecraft.level);
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

    static void forwardWorldChangeIfNeeded(Level currentLevel) {
        Level changedWorld = consumeChangedWorld(currentLevel);
        if (changedWorld != null) {
            ReiEventBus.call(new ClientWorldChangeEvent(changedWorld));
        }
    }

    static Level consumeChangedWorld(Level currentLevel) {
        if (currentLevel == lastKnownLevel) {
            return null;
        }
        Level changedWorld = currentLevel != null ? currentLevel : lastKnownLevel;
        lastKnownLevel = currentLevel;
        return changedWorld;
    }

    static void resetTrackedWorld() {
        lastKnownLevel = null;
    }
}

