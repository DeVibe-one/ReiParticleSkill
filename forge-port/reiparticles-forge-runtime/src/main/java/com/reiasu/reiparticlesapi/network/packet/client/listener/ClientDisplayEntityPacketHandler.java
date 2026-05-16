// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.packet.PacketDisplayEntityS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public final class ClientDisplayEntityPacketHandler {
    private ClientDisplayEntityPacketHandler() {
    }

    public static void receive(PacketDisplayEntityS2C packet) {
        DisplayEntity existing = DisplayEntityManager.INSTANCE.getClientView().get(packet.uuid());
        if (packet.method() == PacketDisplayEntityS2C.Method.REMOVE) {
            DisplayEntity removed = DisplayEntityManager.INSTANCE.getClientView().remove(packet.uuid());
            if (removed != null) {
                removed.cancel();
            }
            return;
        }

        Function<FriendlyByteBuf, DisplayEntity> decoder =
                DisplayEntityManager.INSTANCE.getRegisteredTypes().get(packet.type());
        if (decoder == null) {
            return;
        }
        DisplayEntity display = FriendlyByteBufs.decodeFromByteArray(packet.data(), decoder);
        if (display == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.level != null) {
            display.bindLevel(minecraft.level);
        }

        if (existing == null) {
            DisplayEntityManager.INSTANCE.addClient(display);
            return;
        }
        existing.update(display);
        if (minecraft != null && minecraft.level != null) {
            existing.bindLevel(minecraft.level);
        }
    }
}
