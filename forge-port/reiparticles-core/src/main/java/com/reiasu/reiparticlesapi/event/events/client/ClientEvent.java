// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.client;

import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import net.minecraft.client.Minecraft;

public abstract class ClientEvent extends ReiEvent {
    private final Minecraft client;

    protected ClientEvent(Minecraft client) {
        this.client = client;
    }

    public final Minecraft getClient() {
        return client;
    }
}

