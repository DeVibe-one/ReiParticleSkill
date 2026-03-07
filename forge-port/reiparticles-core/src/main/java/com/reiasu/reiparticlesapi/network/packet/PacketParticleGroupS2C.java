// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PacketParticleGroupS2C(
        UUID uuid,
        ControlType type,
        Map<String, ParticleControllerDataBuffer<?>> args
) {
    public enum PacketArgsType {
        POS("pos"),
        CURRENT_TICK("current_tick"),
        MAX_TICK("max_tick"),
        ROTATE_TO("rotate_to"),
        ROTATE_AXIS("rotate_axis"),
        INVOKE("invoke"),
        AXIS("axis"),
        SCALE("scale"),
        GROUP_TYPE("groupType");

        private final String ofArgs;

        PacketArgsType(String ofArgs) {
            this.ofArgs = ofArgs;
        }

        public String getOfArgs() {
            return ofArgs;
        }

        public static PacketArgsType fromArgsName(String value) {
            for (PacketArgsType type : values()) {
                if (type.ofArgs.equals(value)) {
                    return type;
                }
            }
            return INVOKE;
        }
    }

    public PacketParticleGroupS2C {
        args = Map.copyOf(args);
    }

    public static void encode(PacketParticleGroupS2C packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.uuid);
        buf.writeInt(packet.type.getId());
        for (Map.Entry<String, ParticleControllerDataBuffer<?>> entry : packet.args.entrySet()) {
            byte[] encoded = ParticleControllerDataBuffers.INSTANCE.encode(entry.getValue());
            buf.writeInt(encoded.length);
            buf.writeUtf(entry.getKey());
            buf.writeBytes(encoded);
        }
    }

    public static PacketParticleGroupS2C decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        ControlType type = ControlType.getTypeById(buf.readInt());
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        while (buf.readableBytes() > 0) {
            int len = buf.readInt();
            String key = buf.readUtf();
            byte[] raw = new byte[len];
            buf.readBytes(raw);
            args.put(key, ParticleControllerDataBuffers.INSTANCE.decodeToBuffer(raw));
        }
        return new PacketParticleGroupS2C(uuid, type, args);
    }
}
