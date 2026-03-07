// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

/**
 * Tick-based task scheduler. Supports one-shot, repeating (with interval),
 * and repeating-with-max-tick tasks. Thread-safe task queues are maintained
 * separately for server and client ticks.
 */
public final class ReiScheduler {

    public static final ReiScheduler INSTANCE = new ReiScheduler();

    private final ConcurrentLinkedQueue<TickRunnable> serverTicks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TickRunnable> serverTaskQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TickRunnable> clientTicks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TickRunnable> clientTaskQueue = new ConcurrentLinkedQueue<>();

    private ReiScheduler() {
    }

    /** Called once per server tick. Kept for compatibility with older callers. */
    public void doTick() {
        doServerTick();
    }

    public void doServerTick() {
        doTick(serverTicks, serverTaskQueue);
    }

    public void doClientTick() {
        doTick(clientTicks, clientTaskQueue);
    }

    private static void doTick(
            ConcurrentLinkedQueue<TickRunnable> activeTicks,
            ConcurrentLinkedQueue<TickRunnable> queuedTicks
    ) {
        TickRunnable queued;
        while ((queued = queuedTicks.poll()) != null) {
            activeTicks.add(queued);
        }
        activeTicks.removeIf(task -> {
            task.doTick();
            return task.isCancelled();
        });
    }

    /** Schedule a one-shot task after {@code delay} ticks on the server scheduler. */
    public TickRunnable runTask(int delay, Runnable action) {
        return enqueue(serverTaskQueue, createTask(delay, action, false, 0, 0));
    }

    /** Schedule a one-shot task after {@code delay} ticks on the client scheduler. */
    public TickRunnable runClientTask(int delay, Runnable action) {
        return enqueue(clientTaskQueue, createTask(delay, action, false, 0, 0));
    }

    /** Schedule a repeating task that runs every {@code delay} ticks on the server scheduler. */
    public TickRunnable runTaskTimer(int delay, Runnable action) {
        return enqueue(serverTaskQueue, createTask(delay, action, true, -1, 0));
    }

    /** Schedule a repeating task that runs every {@code delay} ticks on the client scheduler. */
    public TickRunnable runClientTaskTimer(int delay, Runnable action) {
        return enqueue(clientTaskQueue, createTask(delay, action, true, -1, 0));
    }

    /** Schedule a repeating task that runs every tick up to {@code maxLoopTick} total ticks. */
    public TickRunnable runTaskTimerMaxTick(int maxLoopTick, Runnable action) {
        return enqueue(serverTaskQueue, createTask(1, action, true, maxLoopTick, 0));
    }

    /** Schedule a repeating task that runs every tick up to {@code maxLoopTick} total ticks on the client scheduler. */
    public TickRunnable runClientTaskTimerMaxTick(int maxLoopTick, Runnable action) {
        return enqueue(clientTaskQueue, createTask(1, action, true, maxLoopTick, 0));
    }

    /** Schedule a repeating task with a pre-delay, then runs every tick up to {@code maxLoopTick}. */
    public TickRunnable runTaskTimerMaxTick(int preDelay, int maxLoopTick, Runnable action) {
        return enqueue(serverTaskQueue, createTask(1, action, true, maxLoopTick, preDelay));
    }

    /** Schedule a repeating task with a pre-delay on the client scheduler, then runs every tick up to {@code maxLoopTick}. */
    public TickRunnable runClientTaskTimerMaxTick(int preDelay, int maxLoopTick, Runnable action) {
        return enqueue(clientTaskQueue, createTask(1, action, true, maxLoopTick, preDelay));
    }

    public void clear() {
        cancelAndClear(serverTicks);
        cancelAndClear(serverTaskQueue);
        cancelAndClear(clientTicks);
        cancelAndClear(clientTaskQueue);
    }

    private static TickRunnable enqueue(ConcurrentLinkedQueue<TickRunnable> queue, TickRunnable task) {
        queue.add(task);
        return task;
    }

    private static TickRunnable createTask(int delay, Runnable action, boolean repeating, int maxTick, int preDelay) {
        TickRunnable task = new TickRunnable(delay, action, repeating, maxTick);
        task.setPreDelay(Math.max(preDelay, 0));
        return task;
    }

    private static void cancelAndClear(ConcurrentLinkedQueue<TickRunnable> queue) {
        TickRunnable task;
        while ((task = queue.poll()) != null) {
            task.cancel();
        }
    }

    /**
     * A scheduled tick-driven task with support for one-shot, repeating,
     * and repeating-with-limit modes.
     */
    public static final class TickRunnable {
        private final int delay;
        private final Runnable action;
        private final boolean repeating;
        private final int maxTick; // -1 = infinite
        private int preDelay;
        private int currentTick;
        private int loopCount;
        private boolean cancelled;
        private Predicate<TickRunnable> cancelPredicate;
        private Runnable finishCallback;

        TickRunnable(int delay, Runnable action, boolean repeating, int maxTick) {
            this.delay = Math.max(delay, 1);
            this.action = action;
            this.repeating = repeating;
            this.maxTick = maxTick;
        }

        public void cancel() {
            if (cancelled) {
                return;
            }
            this.cancelled = true;
            if (finishCallback != null) {
                finishCallback.run();
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public void setCancelPredicate(Predicate<TickRunnable> predicate) {
            this.cancelPredicate = predicate;
        }

        public void setFinishCallback(Runnable callback) {
            this.finishCallback = callback;
        }

        void setPreDelay(int preDelay) {
            this.preDelay = preDelay;
        }

        public int getLoopCount() {
            return loopCount;
        }

        void doTick() {
            if (cancelled) {
                return;
            }

            if (preDelay > 0) {
                preDelay--;
                return;
            }

            currentTick++;

            if (cancelPredicate != null && cancelPredicate.test(this)) {
                cancel();
                return;
            }

            if (!repeating) {
                if (currentTick >= delay) {
                    action.run();
                    cancel();
                }
                return;
            }

            if (currentTick >= delay) {
                action.run();
                currentTick = 0;
                loopCount++;
                if (maxTick > 0 && loopCount >= maxTick) {
                    cancel();
                }
            }
        }
    }
}
