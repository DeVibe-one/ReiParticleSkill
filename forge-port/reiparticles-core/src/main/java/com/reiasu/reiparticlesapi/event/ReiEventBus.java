// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.annotations.events.EventListener;
import com.reiasu.reiparticlesapi.event.api.EventExecutor;
import com.reiasu.reiparticlesapi.event.api.EventInterruptible;
import com.reiasu.reiparticlesapi.event.api.EventPriority;
import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import com.reiasu.reiparticlesapi.reflect.ReiAPIScanner;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReiEventBus {
    public static final ReiEventBus INSTANCE = new ReiEventBus();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, Set<String>> pendingPackagesByMod = new ConcurrentHashMap<>();
    private final Map<Class<? extends ReiEvent>, EnumMap<EventPriority, CopyOnWriteArrayList<EventExecutor>>> handlerLists =
            new ConcurrentHashMap<>();
    private final Set<Class<?>> discoveredAnnotatedListeners = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private ReiEventBus() {
    }

    public void appendListenerTarget(String modId, String packageName) {
        if (modId == null || modId.isBlank() || packageName == null || packageName.isBlank()) {
            return;
        }
        pendingPackagesByMod.computeIfAbsent(modId, ignored -> ConcurrentHashMap.newKeySet()).add(packageName);
        ReiAPIScanner.registerPackage(packageName);
        if (initialized.get()) {
            initListeners();
        }
    }

    public void initListeners() {
        boolean firstInitialization = initialized.compareAndSet(false, true);
        ReiAPIScanner scanner = ReiAPIScanner.INSTANCE;
        if (scanner.getScannedPackageCount() == 0) {
            if (firstInitialization) {
                LOGGER.info("initListeners(): no packages registered for scanning. Use registerAnnotatedClass() or registerListenerInstance() for explicit registration.");
            }
            return;
        }

        scanner.scan();
        int count = 0;
        for (Class<?> clazz : scanner.getClassesWithAnnotation(EventListener.class)) {
            if (!discoveredAnnotatedListeners.add(clazz)) {
                continue;
            }
            try {
                registerAnnotatedClass(clazz);
                count++;
            } catch (RuntimeException e) {
                discoveredAnnotatedListeners.remove(clazz);
                LOGGER.error("initListeners(): failed to register {}", clazz.getName(), e);
            }
        }

        if (count > 0 || firstInitialization) {
            LOGGER.info("initListeners(): discovered and registered {} new @EventListener classes", count);
        }
    }

    public void registerAnnotatedClass(Class<?> listenerClass) {
        EventListener anno = listenerClass.getAnnotation(EventListener.class);
        if (anno == null) {
            registerListenerClass("reiparticlesapi", listenerClass);
            return;
        }
        registerListenerClass(anno.modId(), listenerClass);
    }

    public void registerListenerClass(String modId, Class<?> listenerClass) {
        registerListenerInstance(modId, EventListenerInvocationSupport.createListenerInstance(listenerClass));
    }

    public void registerListenerInstance(String modId, Object listener) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(listener, "listener");

        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            if (handler == null || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!ReiEvent.class.isAssignableFrom(paramType)) {
                continue;
            }
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<? extends ReiEvent> eventType = (Class<? extends ReiEvent>) paramType;
            registerHandler(modId, listener, method, eventType, handler.priority());
        }
    }

    public int handlerCount(Class<? extends ReiEvent> eventType) {
        EnumMap<EventPriority, CopyOnWriteArrayList<EventExecutor>> bucket = handlerLists.get(eventType);
        if (bucket == null) {
            return 0;
        }
        int sum = 0;
        for (CopyOnWriteArrayList<EventExecutor> handlers : bucket.values()) {
            sum += handlers.size();
        }
        return sum;
    }

    public void clear() {
        handlerLists.clear();
        pendingPackagesByMod.clear();
        discoveredAnnotatedListeners.clear();
        ReiAPIScanner.INSTANCE.clear();
        initialized.set(false);
    }

    public static <T extends ReiEvent> T call(T event) {
        return INSTANCE.callEvent(event);
    }

    public <T extends ReiEvent> T callEvent(T event) {
        Objects.requireNonNull(event, "event");
        Class<?> currentEventType = event.getClass();

        while (currentEventType != null && ReiEvent.class.isAssignableFrom(currentEventType)) {
            @SuppressWarnings("unchecked")
            Class<? extends ReiEvent> eventClass = (Class<? extends ReiEvent>) currentEventType;
            EnumMap<EventPriority, CopyOnWriteArrayList<EventExecutor>> byPriority = handlerLists.get(eventClass);
            if (byPriority != null) {
                for (EventPriority priority : EventPriority.values()) {
                    CopyOnWriteArrayList<EventExecutor> executors = byPriority.get(priority);
                    if (executors == null || executors.isEmpty()) {
                        continue;
                    }
                    for (EventExecutor executor : executors) {
                        try {
                            executor.getExecutor().accept(event);
                        } catch (RuntimeException e) {
                            LOGGER.error("Failed handling event {} in mod {}", event.getClass().getName(), executor.getModId(), e);
                        }
                        if (event instanceof EventInterruptible interruptible && interruptible.isInterrupted()) {
                            return event;
                        }
                    }
                }
            }
            currentEventType = currentEventType.getSuperclass();
        }
        return event;
    }

    private void registerHandler(
            String modId,
            Object listener,
            Method method,
            Class<? extends ReiEvent> eventType,
            EventPriority priority
    ) {
        EnumMap<EventPriority, CopyOnWriteArrayList<EventExecutor>> byPriority =
                handlerLists.computeIfAbsent(eventType, ignored -> new EnumMap<>(EventPriority.class));
        CopyOnWriteArrayList<EventExecutor> executors =
                byPriority.computeIfAbsent(priority, ignored -> new CopyOnWriteArrayList<>());
        executors.add(new EventExecutor(modId, event -> EventListenerInvocationSupport.invokeMethod(listener, method, event)));
    }
}
