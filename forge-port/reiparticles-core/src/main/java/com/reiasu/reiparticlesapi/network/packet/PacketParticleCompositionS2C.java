// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public final class PacketParticleCompositionS2C {
    private final UUID uuid;
    private final String type;
    private final byte[] data;
    private boolean distanceRemove;

    public PacketParticleCompositionS2C(UUID uuid, String type, byte[] data) {
        this.uuid = uuid;
        this.type = type;
        this.data = data;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public boolean getDistanceRemove() {
        return distanceRemove;
    }

    public void setDistanceRemove(boolean distanceRemove) {
        this.distanceRemove = distanceRemove;
    }

    public static void encode(PacketParticleCompositionS2C packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.type);
        buf.writeUUID(packet.uuid);
        buf.writeBoolean(packet.distanceRemove);
        buf.writeInt(packet.data.length);
        buf.writeBytes(packet.data);
    }

    public static PacketParticleCompositionS2C decode(FriendlyByteBuf buf) {
        String type = buf.readUtf();
        UUID uuid = buf.readUUID();
        boolean distanceRemove = buf.readBoolean();
        int size = buf.readInt();
        byte[] data = new byte[size];
        buf.readBytes(data);
        PacketParticleCompositionS2C packet = new PacketParticleCompositionS2C(uuid, type, data);
        packet.setDistanceRemove(distanceRemove);
        return packet;
    }
}
