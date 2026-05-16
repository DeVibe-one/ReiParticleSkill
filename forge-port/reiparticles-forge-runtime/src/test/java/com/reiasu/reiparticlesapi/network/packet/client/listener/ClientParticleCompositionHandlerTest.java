// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientParticleCompositionHandlerTest {
    private static final String TYPE_ID = "test:composition_handler";

    @AfterEach
    void cleanup() {
        ParticleCompositionManager.INSTANCE.clearClient();
    }

    @Test
    void shouldSkipCreateWhenClientWorldIsMissing() {
        AtomicInteger decoderCalls = new AtomicInteger();
        ParticleCompositionManager.INSTANCE.registerType(TYPE_ID, buf -> {
            decoderCalls.incrementAndGet();
            return new TestComposition(Vec3.ZERO, null);
        });

        ClientParticleCompositionHandler.receive(new PacketParticleCompositionS2C(
                UUID.randomUUID(),
                TYPE_ID,
                new byte[0]
        ), null);

        assertEquals(0, decoderCalls.get());
        assertTrue(ParticleCompositionManager.INSTANCE.getClientView().isEmpty());
    }

    @Test
    void shouldBindDecodedCompositionToClientWorld() {
        UUID id = UUID.randomUUID();
        ParticleCompositionManager.INSTANCE.registerType(TYPE_ID, decodeTestComposition());
        Level clientWorld = allocateClientLevel();

        ClientParticleCompositionHandler.receive(new PacketParticleCompositionS2C(
                id,
                TYPE_ID,
                encodeTestComposition(id)
        ), clientWorld);

        ParticleComposition created = ParticleCompositionManager.INSTANCE.getClientView().get(id);
        assertNotNull(created);
        assertTrue(created.getClient());
        assertEquals(clientWorld, created.getWorld());
        assertFalse(created.getCanceled());
    }

    private static Function<FriendlyByteBuf, ParticleComposition> decodeTestComposition() {
        return buf -> {
            TestComposition composition = new TestComposition(Vec3.ZERO, null);
            ParticleComposition.decodeBase(composition, buf);
            return composition;
        };
    }

    private static byte[] encodeTestComposition(UUID id) {
        TestComposition composition = new TestComposition(Vec3.ZERO, null);
        composition.setControlUUID(id);
        return FriendlyByteBufs.encodeToByteArray(buf -> ParticleComposition.encodeBase(composition, buf));
    }

    private static ClientLevel allocateClientLevel() {
        ClientLevel clientLevel = UnsafeAllocator.allocate(ClientLevel.class);
        setBooleanField(Level.class, clientLevel, "isClientSide", true);
        return clientLevel;
    }

    private static void setBooleanField(Class<?> owner, Object target, String fieldName, boolean value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
        }
    }

    private static final class TestComposition extends ParticleComposition {
        TestComposition(Vec3 position, Level world) {
            super(position, world);
        }

        @Override
        public Map<CompositionData, RelativeLocation> getParticles() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }
    }
}
