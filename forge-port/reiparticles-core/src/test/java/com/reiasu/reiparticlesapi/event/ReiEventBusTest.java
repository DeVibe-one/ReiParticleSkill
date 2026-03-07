/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.event;

import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.event.api.EventInterruptible;
import com.reiasu.reiparticlesapi.event.api.EventPriority;
import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import com.reiasu.reiparticlesapi.event.scanlate.LateScanEvent;
import com.reiasu.reiparticlesapi.event.scanlate.LateScanListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReiEventBusTest {
    @AfterEach
    void clean() {
        ReiEventBus.INSTANCE.clear();
    }

    @Test
    void shouldDispatchHandlersByPriorityOrder() {
        StringBuilder order = new StringBuilder();
        ReiEventBus.INSTANCE.registerListenerInstance("test", new PriorityListener(order));

        ReiEventBus.call(new BaseEvent());

        assertEquals("HIGHEST,HIGH,NORMAL,LOW,LOWEST", order.toString());
    }

    @Test
    void shouldStopWhenEventIsInterrupted() {
        AtomicInteger calls = new AtomicInteger();
        ReiEventBus.INSTANCE.registerListenerInstance("test", new InterruptingListener(calls));

        ReiEventBus.call(new InterruptEvent());

        assertEquals(1, calls.get());
    }

    @Test
    void shouldDispatchChildThenParentEventHandlers() {
        StringBuilder order = new StringBuilder();
        ReiEventBus.INSTANCE.registerListenerInstance("test", new HierarchyListener(order));

        ReiEventBus.call(new ChildEvent());

        assertEquals("child,parent", order.toString());
    }

    @Test
    void shouldContinueAfterRuntimeListenerFailure() {
        AtomicInteger calls = new AtomicInteger();
        ReiEventBus.INSTANCE.registerListenerInstance("test", new FailingListener());
        ReiEventBus.INSTANCE.registerListenerInstance("test", new InterruptingListener(calls));

        ReiEventBus.call(new InterruptEvent());

        assertEquals(1, calls.get());
    }

    @Test
    void shouldDiscoverAnnotatedListenersRegisteredAfterInitialInit() {
        LateScanListener.CALLS.set(0);

        ReiEventBus.INSTANCE.initListeners();
        ReiEventBus.INSTANCE.appendListenerTarget("late-test", LateScanListener.class.getPackageName());

        ReiEventBus.call(new LateScanEvent());

        assertEquals(1, LateScanListener.CALLS.get());
    }

    private static class BaseEvent extends ReiEvent {
    }

    private static final class ChildEvent extends BaseEvent {
    }

    private static final class InterruptEvent extends ReiEvent implements EventInterruptible {
        private boolean interrupted;

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }

        @Override
        public void setInterrupted(boolean interrupted) {
            this.interrupted = interrupted;
        }
    }

    private static final class PriorityListener {
        private final StringBuilder order;

        private PriorityListener(StringBuilder order) {
            this.order = order;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void highest(BaseEvent event) {
            append("HIGHEST");
        }

        @EventHandler(priority = EventPriority.HIGH)
        public void high(BaseEvent event) {
            append("HIGH");
        }

        @EventHandler(priority = EventPriority.NORMAL)
        public void normal(BaseEvent event) {
            append("NORMAL");
        }

        @EventHandler(priority = EventPriority.LOW)
        public void low(BaseEvent event) {
            append("LOW");
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void lowest(BaseEvent event) {
            append("LOWEST");
        }

        private void append(String value) {
            if (order.length() > 0) {
                order.append(',');
            }
            order.append(value);
        }
    }

    private static final class FailingListener {
        @EventHandler(priority = EventPriority.HIGHEST)
        public void fail(InterruptEvent event) {
            throw new IllegalStateException("boom");
        }
    }

    private static final class InterruptingListener {
        private final AtomicInteger calls;

        private InterruptingListener(AtomicInteger calls) {
            this.calls = calls;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void first(InterruptEvent event) {
            calls.incrementAndGet();
            event.setInterrupted(true);
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void second(InterruptEvent event) {
            calls.incrementAndGet();
        }
    }

    private static final class HierarchyListener {
        private final StringBuilder order;

        private HierarchyListener(StringBuilder order) {
            this.order = order;
        }

        @EventHandler
        public void onChild(ChildEvent event) {
            append("child");
        }

        @EventHandler
        public void onParent(BaseEvent event) {
            append("parent");
        }

        private void append(String value) {
            if (order.length() > 0) {
                order.append(',');
            }
            order.append(value);
        }
    }
}
