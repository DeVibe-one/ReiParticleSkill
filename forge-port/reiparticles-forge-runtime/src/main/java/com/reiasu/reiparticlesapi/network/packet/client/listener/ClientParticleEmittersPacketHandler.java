// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.packet.PacketParticleEmittersS2C;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public final class ClientParticleEmittersPacketHandler {
    private ClientParticleEmittersPacketHandler() {
    }

    public static void receive(PacketParticleEmittersS2C packet) {
        Function<FriendlyByteBuf, ParticleEmitters> decoder = ParticleEmittersManager.getCodecFromID(packet.emitterKey());
        if (decoder == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.emitterData()));
        ParticleEmitters emitters = decoder.apply(buf);
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
            case REMOVE -> {
                ParticleEmitters target = ParticleEmittersManager.getClientEmitters().get(emitters.getUuid());
                if (target != null) {
                    target.cancel();
                }
            }
        }
    }
}

