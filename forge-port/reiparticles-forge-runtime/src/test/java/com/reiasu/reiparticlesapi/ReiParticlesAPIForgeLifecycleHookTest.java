// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi;

import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReiParticlesAPIForgeLifecycleHookTest {
    @Test
    void shouldRegisterServerLifecycleListenersAndRouteEventsToResetCallback() {
        CapturingEventBus bus = new CapturingEventBus();
        List<String> phases = new ArrayList<>();
        DedicatedServer server = UnsafeAllocator.allocate(DedicatedServer.class);

        ReiParticlesAPIForge.registerServerLifecycleResetHooks(bus.proxy(), phases::add);

        assertTrue(bus.hasListener(ServerStoppingEvent.class));
        assertTrue(bus.hasListener(ServerStoppedEvent.class));
        assertEquals(EventPriority.NORMAL, bus.priority(ServerStoppingEvent.class));
        assertEquals(EventPriority.NORMAL, bus.priority(ServerStoppedEvent.class));
        assertFalse(bus.receivesCancelled(ServerStoppingEvent.class));
        assertFalse(bus.receivesCancelled(ServerStoppedEvent.class));

        bus.dispatch(new ServerStoppingEvent(server));
        bus.dispatch(new ServerStoppedEvent(server));

        assertEquals(List.of("stopping", "stopped"), phases);
    }

    private static final class CapturingEventBus implements InvocationHandler {
        private final Map<Class<? extends Event>, ListenerRegistration<?>> listeners = new LinkedHashMap<>();

        IEventBus proxy() {
            return (IEventBus) Proxy.newProxyInstance(
                    IEventBus.class.getClassLoader(),
                    new Class<?>[]{IEventBus.class},
                    this);
        }

        boolean hasListener(Class<? extends Event> eventType) {
            return listeners.containsKey(eventType);
        }

        EventPriority priority(Class<? extends Event> eventType) {
            return listeners.get(eventType).priority();
        }

        boolean receivesCancelled(Class<? extends Event> eventType) {
            return listeners.get(eventType).receiveCancelled();
        }

        <T extends Event> void dispatch(T event) {
            ListenerRegistration<?> registration = listeners.get(event.getClass());
            if (registration == null) {
                throw new AssertionError("No listener registered for " + event.getClass().getName());
            }
            @SuppressWarnings("unchecked")
            Consumer<T> consumer = (Consumer<T>) registration.consumer();
            consumer.accept(event);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "addListener" -> captureListener(args);
                case "register", "unregister", "shutdown", "start" -> null;
                case "post" -> false;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "CapturingEventBus";
                default -> throw new UnsupportedOperationException("Unexpected IEventBus method: " + method.getName());
            };
        }

        private Object captureListener(Object[] args) {
            if (args == null || args.length != 4) {
                throw new UnsupportedOperationException("Unexpected addListener signature");
            }
            EventPriority priority = (EventPriority) args[0];
            boolean receiveCancelled = (Boolean) args[1];
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) args[2];
            @SuppressWarnings("unchecked")
            Consumer<? extends Event> consumer = (Consumer<? extends Event>) args[3];
            listeners.put(eventType, new ListenerRegistration<>(priority, receiveCancelled, consumer));
            return null;
        }
    }

    private record ListenerRegistration<T extends Event>(
            EventPriority priority,
            boolean receiveCancelled,
            Consumer<T> consumer) {
    }
}
