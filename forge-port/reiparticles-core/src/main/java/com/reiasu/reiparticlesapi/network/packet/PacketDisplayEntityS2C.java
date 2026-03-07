// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record PacketDisplayEntityS2C(UUID uuid, String type, byte[] data, Method method) {
    public enum Method {
        CREATE(0),
        TOGGLE(1),
        REMOVE(2);

        private final int id;

        Method(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Method idOf(int id) {
            return switch (id) {
                case 0 -> CREATE;
                case 1 -> TOGGLE;
                case 2 -> REMOVE;
                default -> CREATE;
            };
        }
    }

    public static PacketDisplayEntityS2C ofCreate(DisplayEntity entity) {
        return new PacketDisplayEntityS2C(entity.getControlUUID(), entity.typeId(), entity.encodeToBytes(), Method.CREATE);
    }

    public static PacketDisplayEntityS2C ofToggle(DisplayEntity entity) {
        return new PacketDisplayEntityS2C(entity.getControlUUID(), entity.typeId(), entity.encodeToBytes(), Method.TOGGLE);
    }

    public static PacketDisplayEntityS2C ofRemove(DisplayEntity entity) {
        return new PacketDisplayEntityS2C(entity.getControlUUID(), entity.typeId(), entity.encodeToBytes(), Method.REMOVE);
    }

    public static void encode(PacketDisplayEntityS2C packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.method.getId());
        buf.writeUtf(packet.type);
        buf.writeUUID(packet.uuid);
        buf.writeInt(packet.data.length);
        buf.writeBytes(packet.data);
    }

    public static PacketDisplayEntityS2C decode(FriendlyByteBuf buf) {
        Method method = Method.idOf(buf.readInt());
        String type = buf.readUtf();
        UUID uuid = buf.readUUID();
        int size = buf.readInt();
        byte[] data = new byte[size];
        buf.readBytes(data);
        return new PacketDisplayEntityS2C(uuid, type, data, method);
    }
}
