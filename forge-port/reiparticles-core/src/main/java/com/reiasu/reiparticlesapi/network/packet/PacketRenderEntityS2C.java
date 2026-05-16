// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class PacketRenderEntityS2C {
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

    private UUID uuid;
    private byte[] entityData;
    private ResourceLocation id;
    private Method method;

    public PacketRenderEntityS2C(UUID uuid, byte[] entityData, ResourceLocation id, Method method) {
        this.uuid = uuid;
        this.entityData = entityData == null ? new byte[0] : entityData.clone();
        this.id = id;
        this.method = method;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public byte[] getEntityData() {
        return entityData.clone();
    }

    public void setEntityData(byte[] entityData) {
        this.entityData = entityData == null ? new byte[0] : entityData.clone();
    }

    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public static PacketRenderEntityS2C ofSpawn(RenderEntity entity) {
        return new PacketRenderEntityS2C(entity.getUuid(), entity.encodeToBytes(), entity.getRenderID(), Method.CREATE);
    }

    public static PacketRenderEntityS2C ofSync(RenderEntity entity) {
        return new PacketRenderEntityS2C(entity.getUuid(), entity.encodeToBytes(), entity.getRenderID(), Method.TOGGLE);
    }

    public static PacketRenderEntityS2C ofRemove(RenderEntity entity) {
        return new PacketRenderEntityS2C(entity.getUuid(), new byte[0], entity.getRenderID(), Method.REMOVE);
    }

    public static void encode(PacketRenderEntityS2C packet, FriendlyByteBuf buf) {
        byte[] entityData = packet.entityData == null ? new byte[0] : packet.entityData;
        buf.writeInt(packet.method.getId());
        buf.writeUUID(packet.uuid);
        buf.writeResourceLocation(packet.id);
        buf.writeInt(entityData.length);
        buf.writeBytes(entityData);
    }

    public static PacketRenderEntityS2C decode(FriendlyByteBuf buf) {
        Method method = Method.idOf(buf.readInt());
        UUID uuid = buf.readUUID();
        ResourceLocation id = buf.readResourceLocation();
        int size = buf.readInt();
        byte[] entity = FriendlyByteBufs.readPayload(buf, size, "render entity payload");
        return new PacketRenderEntityS2C(uuid, entity, id, method);
    }
}
