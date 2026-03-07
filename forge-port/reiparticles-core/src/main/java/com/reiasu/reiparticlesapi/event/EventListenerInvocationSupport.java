// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event;

import com.reiasu.reiparticlesapi.event.api.ReiEvent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class EventListenerInvocationSupport {
    private EventListenerInvocationSupport() {
    }

    static Object createListenerInstance(Class<?> listenerClass) {
        try {
            Field instanceField = listenerClass.getDeclaredField("INSTANCE");
            if (Modifier.isStatic(instanceField.getModifiers())) {
                instanceField.setAccessible(true);
                Object existing = instanceField.get(null);
                if (existing != null) {
                    return existing;
                }
            }
        } catch (NoSuchFieldException ignored) {
            // not a Kotlin object or no singleton instance field
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to resolve INSTANCE for " + listenerClass.getName(), e);
        }

        try {
            var ctor = listenerClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate listener " + listenerClass.getName(), e);
        }
    }

    static void invokeMethod(Object listener, Method method, ReiEvent event) {
        try {
            method.invoke(listener, event);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed invoking " + method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Listener threw checked exception in " + method.getName(), cause);
        }
    }
}
