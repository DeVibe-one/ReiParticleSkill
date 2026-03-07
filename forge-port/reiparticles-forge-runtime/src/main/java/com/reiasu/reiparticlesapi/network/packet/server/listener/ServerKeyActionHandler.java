// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.server.listener;

import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.key.KeyActionEvent;
import com.reiasu.reiparticlesapi.network.packet.PacketKeyActionC2S;
import net.minecraft.server.level.ServerPlayer;

public final class ServerKeyActionHandler {
    private ServerKeyActionHandler() {
    }

    public static void receive(PacketKeyActionC2S packet, ServerPlayer player) {
        ReiEventBus.call(new KeyActionEvent(
                player,
                packet.keyId(),
                packet.action(),
                packet.pressTick(),
                packet.isRelease(),
                true
        ));
    }
}

