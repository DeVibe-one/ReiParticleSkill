// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.packet.PacketRenderEntityS2C;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import com.reiasu.reiparticlesapi.renderer.client.ClientRenderEntityManager;
import net.minecraft.client.Minecraft;

import java.util.function.Function;

public final class ClientRenderEntityPacketHandler {
    private ClientRenderEntityPacketHandler() {
    }

    public static void receive(PacketRenderEntityS2C packet) {
        PacketRenderEntityS2C.Method method = packet.getMethod();
        byte[] data = packet.getEntityData();

        Function<byte[], RenderEntity> codec = ClientRenderEntityManager.INSTANCE.getCodecFromID(packet.getId());
        if (codec == null) return;

        RenderEntity entity = codec.apply(data);
        if (entity == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            entity.setWorld(mc.level);
        }

        switch (method) {
            case CREATE -> ClientRenderEntityManager.INSTANCE.add(entity);
            case TOGGLE -> {
                RenderEntity existing = ClientRenderEntityManager.INSTANCE.getFrom(packet.getUuid());
                if (existing != null) {
                    existing.loadProfileFromEntity(entity);
                }
            }
            case REMOVE -> {
                RenderEntity existing = ClientRenderEntityManager.INSTANCE.getFrom(packet.getUuid());
                if (existing != null) {
                    existing.setCanceled(true);
                }
            }
        }
    }
}

