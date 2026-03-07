// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import com.reiasu.reiparticlesapi.test.SimpleTestGroupBuilder;
import com.reiasu.reiparticlesapi.test.TestManager;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Main entry point for the ReiParticles API.
 *
 * Provides lifecycle management (initialization, scanner loading, hook registration),
 * a {@link ReiScheduler} for deferred tasks, and convenience methods for the
 * {@link ReiEventBus} event system.
 */
public final class ReiParticlesAPI {
    public static final ReiParticlesAPI INSTANCE = new ReiParticlesAPI();
    /** @deprecated Use {@link #reiScheduler()} for new code. */
    @Deprecated
    public static final Scheduler scheduler = new Scheduler(true);
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;
    private static boolean scannersLoaded;
    private static boolean testHooksRegistered;

    private ReiParticlesAPI() {
    }

    /** Initializes the API. Safe to call multiple times; only the first call takes effect. */
    public static void init() {
        if (initialized) {
            LOGGER.debug("init() called again; already initialized, skipping");
            return;
        }
        initialized = true;
        ParticleEmittersManager.registerBuiltinCodecs();
        LOGGER.info("ReiParticlesAPI init completed");
    }

    /** Returns {@code true} if {@link #init()} has been called. */
    public static boolean isInitialized() {
        return initialized;
    }

    /** Scans and initializes event listener packages. Call once after all listener targets are registered. */
    public void loadScannerPackages() {
        if (scannersLoaded) {
            LOGGER.debug("loadScannerPackages() called again; already loaded, skipping");
            return;
        }
        scannersLoaded = true;
        if (!initialized) {
            LOGGER.warn("loadScannerPackages() called before init(); call init() first");
        }
        LOGGER.info("ReiParticlesAPI scanner packages loaded");
        ReiEventBus.INSTANCE.initListeners();
    }

    public boolean scannersLoaded() {
        return scannersLoaded;
    }

    public void registerTest() {
        if (testHooksRegistered) {
            return;
        }
        testHooksRegistered = true;
        TestManager.INSTANCE.register("api-test-group-builder", user -> buildSmokeTestGroup(user));
        LOGGER.info("ReiParticlesAPI test hooks registered");
    }

    public boolean testHooksRegistered() {
        return testHooksRegistered;
    }

    /**
     * Registers a package for {@link com.reiasu.reiparticlesapi.annotations.events.EventListener @EventListener}
     * class scanning.
     */
    public void appendEventListenerTarget(String modId, String packageName) {
        ReiEventBus.INSTANCE.appendListenerTarget(modId, packageName);
    }

    /** Initializes all registered event listeners. Call after all targets have been appended. */
    public void initEventListeners() {
        if (!scannersLoaded) {
            LOGGER.debug("initEventListeners() called before loadScannerPackages(); listeners must be registered explicitly");
        }
        ReiEventBus.INSTANCE.initListeners();
    }

    /** Manually registers a single event listener instance. */
    public void registerEventListener(String modId, Object listener) {
        ReiEventBus.INSTANCE.registerListenerInstance(modId, listener);
    }

    /** Fires an event through the {@link ReiEventBus}. */
    public <T extends ReiEvent> T callEvent(T event) {
        return ReiEventBus.call(event);
    }

    /**
     * Returns the primary shared scheduler.
     * Server-side tasks use {@code runTask(...)} and related methods by default.
     * Client-only work should use the explicit client scheduling methods on {@link ReiScheduler}.
     */
    public static ReiScheduler reiScheduler() {
        return ReiScheduler.INSTANCE;
    }

    /**
     * Legacy scheduler compatibility layer.
     *
     * The shared {@link #scheduler} instance still delegates to {@link ReiScheduler} so runtime callers keep
     * their current server-side behavior. Ad-hoc instances created in tests keep a local queue and advance on
     * {@link #tick()}.
     *
     * @deprecated Use {@link #reiScheduler()} instead.
     */
    @Deprecated
    public static final class Scheduler {
        private final boolean delegateToGlobal;
        private final List<ScheduledTask> localTasks = new ArrayList<>();
        private int currentTick;

        public Scheduler() {
            this(false);
        }

        private Scheduler(boolean delegateToGlobal) {
            this.delegateToGlobal = delegateToGlobal;
        }

        /** Schedules a one-shot task after the given number of ticks. */
        public void runTask(int ticks, Runnable task) {
            int delay = Math.max(1, ticks);
            if (delegateToGlobal) {
                ReiScheduler.INSTANCE.runTask(delay, task);
                return;
            }
            localTasks.add(new ScheduledTask(currentTick + delay, task));
        }

        /** Advances legacy local tasks or delegates to the global server scheduler. */
        public void tick() {
            if (delegateToGlobal) {
                ReiScheduler.INSTANCE.doServerTick();
                return;
            }
            currentTick++;
            Iterator<ScheduledTask> iterator = localTasks.iterator();
            while (iterator.hasNext()) {
                ScheduledTask task = iterator.next();
                if (task.executionTick <= currentTick) {
                    task.action.run();
                    iterator.remove();
                }
            }
        }

        /** Clears local legacy tasks. The global scheduler lifecycle stays managed elsewhere. */
        public void shutdown() {
            if (!delegateToGlobal) {
                localTasks.clear();
            }
        }

        private record ScheduledTask(int executionTick, Runnable action) {
        }
    }

    private static SimpleTestGroupBuilder buildSmokeTestGroup(ServerPlayer user) {
        return new SimpleTestGroupBuilder("api-test-group-builder", user);
    }
}
