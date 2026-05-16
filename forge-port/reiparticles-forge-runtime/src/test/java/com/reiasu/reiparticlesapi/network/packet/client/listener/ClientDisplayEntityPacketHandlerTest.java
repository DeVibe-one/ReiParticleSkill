// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.packet.PacketDisplayEntityS2C;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientDisplayEntityPacketHandlerTest {
    @AfterEach
    void cleanup() {
        DisplayEntityManager.INSTANCE.clearClient();
    }

    @Test
    void shouldCreateAndUpdateDisplayEntityWithoutMinecraftInstance() {
        DisplayEntityManager.INSTANCE.registerType(TestDisplayEntity.TYPE_ID, TestDisplayEntity::decode);
        TestDisplayEntity source = new TestDisplayEntity();
        source.setPos(new Vec3(1.0, 2.0, 3.0));
        source.setScale(1.5f);

        ClientDisplayEntityPacketHandler.receive(PacketDisplayEntityS2C.ofCreate(source));

        DisplayEntity created = DisplayEntityManager.INSTANCE.getClientView().get(source.getControlUUID());
        assertNotNull(created);
        assertEquals(source.getPos(), created.getPos());
        assertEquals(source.getScale(), created.getScale());
        assertEquals(0, TestDisplayEntity.lastDecodeBuffer.refCnt());

        source.setPos(new Vec3(4.0, 5.0, 6.0));
        source.setScale(2.0f);
        ClientDisplayEntityPacketHandler.receive(PacketDisplayEntityS2C.ofToggle(source));

        DisplayEntity updated = DisplayEntityManager.INSTANCE.getClientView().get(source.getControlUUID());
        assertNotNull(updated);
        assertEquals(source.getPos(), updated.getPos());
        assertEquals(source.getScale(), updated.getScale());
        assertEquals(0, TestDisplayEntity.lastDecodeBuffer.refCnt());
    }

    @Test
    void shouldRemoveTrackedDisplayEntityWithoutDecoder() {
        TestDisplayEntity entity = new TestDisplayEntity();
        DisplayEntityManager.INSTANCE.addClient(entity);

        ClientDisplayEntityPacketHandler.receive(new PacketDisplayEntityS2C(
                entity.getControlUUID(),
                "unknown:type",
                new byte[0],
                PacketDisplayEntityS2C.Method.REMOVE
        ));

        assertNull(DisplayEntityManager.INSTANCE.getClientView().get(entity.getControlUUID()));
        assertTrue(entity.getCanceled());
    }

    private static final class TestDisplayEntity extends DisplayEntity {
        private static final String TYPE_ID = "reiparticlesruntime:test_display_entity_handler";
        private static FriendlyByteBuf lastDecodeBuffer;

        private static TestDisplayEntity decode(FriendlyByteBuf buf) {
            lastDecodeBuffer = buf;
            TestDisplayEntity entity = new TestDisplayEntity();
            DisplayEntity.decodeBase(entity, buf);
            return entity;
        }

        @Override
        public String typeId() {
            return TYPE_ID;
        }

        @Override
        public byte[] encodeToBytes() {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            DisplayEntity.encodeBase(this, buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.getBytes(0, data);
            return data;
        }
    }
}
