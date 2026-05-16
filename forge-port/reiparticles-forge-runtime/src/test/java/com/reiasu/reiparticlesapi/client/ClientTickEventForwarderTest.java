// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.client;

import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldChangeEvent;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientTickEventForwarderTest {
    @AfterEach
    void cleanup() {
        ClientTickEventForwarder.resetTrackedWorld();
        ReiEventBus.INSTANCE.clear();
    }

    @Test
    void shouldDispatchWorldChangeForJoinSwitchAndUnload() {
        Level firstWorld = allocateClientLevel();
        Level secondWorld = allocateClientLevel();
        CapturingListener listener = new CapturingListener();
        ReiEventBus.INSTANCE.registerListenerInstance("test", listener);

        ClientTickEventForwarder.forwardWorldChangeIfNeeded(null);
        ClientTickEventForwarder.forwardWorldChangeIfNeeded(firstWorld);
        ClientTickEventForwarder.forwardWorldChangeIfNeeded(firstWorld);
        ClientTickEventForwarder.forwardWorldChangeIfNeeded(secondWorld);
        ClientTickEventForwarder.forwardWorldChangeIfNeeded(null);

        assertEquals(List.of(firstWorld, secondWorld, secondWorld), listener.worlds);
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

    private static final class CapturingListener {
        private final List<Level> worlds = new ArrayList<>();

        @EventHandler
        public void onClientWorldChange(ClientWorldChangeEvent event) {
            worlds.add(event.getWorld());
        }
    }
}
