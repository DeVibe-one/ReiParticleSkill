// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketParticleEmittersS2CTest {
    @Test
    void shouldRoundTripEmitterPacketUuidAndPayload() {
        UUID uuid = UUID.randomUUID();
        byte[] payload = new byte[]{1, 2, 3};
        PacketParticleEmittersS2C source = new PacketParticleEmittersS2C(
                new ResourceLocation("reiparticlesapi", "test_emitter"),
                uuid,
                payload,
                PacketParticleEmittersS2C.PacketType.CHANGE_OR_CREATE);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PacketParticleEmittersS2C.encode(source, buf);
        PacketParticleEmittersS2C decoded = PacketParticleEmittersS2C.decode(buf);

        assertEquals(source.emitterKey(), decoded.emitterKey());
        assertEquals(uuid, decoded.emitterUuid());
        assertArrayEquals(payload, decoded.emitterData());
        assertEquals(PacketParticleEmittersS2C.PacketType.CHANGE_OR_CREATE, decoded.type());
    }

    @Test
    void shouldEncodeRemovePacketWithoutEmitterPayload() {
        UUID uuid = UUID.randomUUID();
        PacketParticleEmittersS2C source = new PacketParticleEmittersS2C(
                new ResourceLocation("reiparticlesapi", "test_emitter"),
                uuid,
                new byte[0],
                PacketParticleEmittersS2C.PacketType.REMOVE);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PacketParticleEmittersS2C.encode(source, buf);
        PacketParticleEmittersS2C decoded = PacketParticleEmittersS2C.decode(buf);

        assertEquals(uuid, decoded.emitterUuid());
        assertArrayEquals(new byte[0], decoded.emitterData());
        assertEquals(PacketParticleEmittersS2C.PacketType.REMOVE, decoded.type());
    }
}
