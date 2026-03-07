// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.packet.PacketParticleS2C;
import net.minecraft.client.Minecraft;

public final class ClientParticlePacketHandler {
    private ClientParticlePacketHandler() {
    }

    public static void receive(PacketParticleS2C packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        minecraft.level.addParticle(
                packet.type(),
                true,
                packet.pos().x,
                packet.pos().y,
                packet.pos().z,
                packet.velocity().x,
                packet.velocity().y,
                packet.velocity().z
        );
    }
}

