// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleEmittersS2C;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public final class ClientParticleEmittersPacketHandler {
    private ClientParticleEmittersPacketHandler() {
    }

    public static void receive(PacketParticleEmittersS2C packet) {
        if (packet.type() == PacketParticleEmittersS2C.PacketType.REMOVE) {
            ParticleEmitters target = ParticleEmittersManager.getClientEmitters().get(packet.emitterUuid());
            if (target != null) {
                target.cancel();
            }
            return;
        }

        Function<FriendlyByteBuf, ParticleEmitters> decoder = ParticleEmittersManager.getCodecFromID(packet.emitterKey());
        if (decoder == null) {
            return;
        }
        ParticleEmitters emitters = FriendlyByteBufs.decodeFromByteArray(packet.emitterData(), decoder);
        if (emitters == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        switch (packet.type()) {
            case CHANGE_OR_CREATE -> {
                if (minecraft.level != null) {
                    ParticleEmittersManager.createOrChangeClient(emitters, minecraft.level);
                }
            }
            case REMOVE -> throw new IllegalStateException("remove packets are handled before decode");
        }
    }
}

