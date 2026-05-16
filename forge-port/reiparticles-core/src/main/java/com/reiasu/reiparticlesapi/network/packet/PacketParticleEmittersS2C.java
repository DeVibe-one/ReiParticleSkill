// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PacketParticleEmittersS2C(
        ResourceLocation emitterKey,
        UUID emitterUuid,
        byte[] emitterData,
        PacketType type
) {
    public enum PacketType {
        CHANGE_OR_CREATE(0),
        REMOVE(1);

        private final int id;

        PacketType(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static PacketType fromID(int id) {
            return switch (id) {
                case 0 -> CHANGE_OR_CREATE;
                case 1 -> REMOVE;
                default -> CHANGE_OR_CREATE;
            };
        }
    }

    public static void encode(PacketParticleEmittersS2C packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type.getId());
        buf.writeResourceLocation(packet.emitterKey);
        buf.writeUUID(packet.emitterUuid);
        buf.writeVarInt(packet.emitterData.length);
        buf.writeBytes(packet.emitterData);
    }

    public static PacketParticleEmittersS2C decode(FriendlyByteBuf buf) {
        PacketType packetType = PacketType.fromID(buf.readVarInt());
        ResourceLocation key = buf.readResourceLocation();
        UUID uuid = buf.readUUID();
        int size = buf.readVarInt();
        byte[] data = FriendlyByteBufs.readPayload(buf, size, "particle emitter payload");
        return new PacketParticleEmittersS2C(key, uuid, data, packetType);
    }
}
