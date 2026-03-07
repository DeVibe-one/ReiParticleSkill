// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PacketParticleEmittersS2C(ResourceLocation emitterKey, byte[] emitterData, PacketType type) {
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
        buf.writeVarInt(packet.emitterData.length);
        buf.writeBytes(packet.emitterData);
    }

    public static PacketParticleEmittersS2C decode(FriendlyByteBuf buf) {
        PacketType packetType = PacketType.fromID(buf.readVarInt());
        ResourceLocation key = buf.readResourceLocation();
        int size = buf.readVarInt();
        byte[] data = new byte[size];
        buf.readBytes(data);
        return new PacketParticleEmittersS2C(key, data, packetType);
    }
}
