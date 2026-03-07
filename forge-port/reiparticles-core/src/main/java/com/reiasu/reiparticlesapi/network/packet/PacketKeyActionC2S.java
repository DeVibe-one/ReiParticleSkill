// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.event.events.key.KeyActionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PacketKeyActionC2S(ResourceLocation keyId, KeyActionType action, int pressTick, boolean isRelease) {
    public static void encode(PacketKeyActionC2S packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.keyId);
        buf.writeInt(packet.action.getId());
        buf.writeInt(packet.pressTick);
        buf.writeBoolean(packet.isRelease);
    }

    public static PacketKeyActionC2S decode(FriendlyByteBuf buf) {
        ResourceLocation keyId = buf.readResourceLocation();
        KeyActionType action = KeyActionType.fromId(buf.readInt());
        int pressTick = buf.readInt();
        boolean isRelease = buf.readBoolean();
        return new PacketKeyActionC2S(keyId, action, pressTick, isRelease);
    }
}
