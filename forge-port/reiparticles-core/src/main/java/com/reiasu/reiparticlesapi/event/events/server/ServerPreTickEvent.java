// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.server;

import net.minecraft.server.MinecraftServer;

public final class ServerPreTickEvent extends ServerEvent {
    public ServerPreTickEvent(MinecraftServer server) {
        super(server);
    }
}

