// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PacketParticleStyleS2C(
        UUID uuid,
        ControlType type,
        Map<String, ParticleControllerDataBuffer<?>> args
) {
    public PacketParticleStyleS2C {
        args = Map.copyOf(args);
    }

    public static void encode(PacketParticleStyleS2C packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.uuid);
        buf.writeInt(packet.type.getId());
        buf.writeInt(packet.args.size());
        for (Map.Entry<String, ParticleControllerDataBuffer<?>> entry : packet.args.entrySet()) {
            byte[] encoded = ParticleControllerDataBuffers.INSTANCE.encode(entry.getValue());
            buf.writeInt(encoded.length);
            buf.writeUtf(entry.getKey());
            buf.writeBytes(encoded);
        }
    }

    public static PacketParticleStyleS2C decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        ControlType type = ControlType.getTypeById(buf.readInt());
        int argsCount = buf.readInt();
        if (argsCount < 0) {
            throw new IllegalArgumentException("particle style args count must not be negative: " + argsCount);
        }
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        for (int i = 0; i < argsCount; i++) {
            int len = buf.readInt();
            String key = buf.readUtf();
            byte[] raw = FriendlyByteBufs.readPayload(buf, len, "particle style arg payload");
            args.put(key, ParticleControllerDataBuffers.INSTANCE.decodeToBuffer(raw));
        }
        return new PacketParticleStyleS2C(uuid, type, args);
    }
}
