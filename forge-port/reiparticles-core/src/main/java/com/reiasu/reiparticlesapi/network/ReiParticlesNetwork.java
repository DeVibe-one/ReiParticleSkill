// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network;

import com.reiasu.reiparticlesapi.ReiParticlesConstants;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReiParticlesNetwork {
    @FunctionalInterface
    public interface PacketSender {
        void sendTo(ServerPlayer player, Object packet);
    }

    private static final AtomicBoolean WARNED_UNBOUND = new AtomicBoolean(false);
    private static volatile PacketSender sender = (player, packet) -> {
        if (WARNED_UNBOUND.compareAndSet(false, true)) {
            ReiParticlesConstants.logger.warn("Packet sender is not bound; dropping packets until runtime initialization completes");
        }
    };

    private ReiParticlesNetwork() {
    }

    public static void bindSender(PacketSender packetSender) {
        sender = Objects.requireNonNull(packetSender, "packetSender");
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        if (player == null || packet == null) {
            return;
        }
        sender.sendTo(player, packet);
    }
}
