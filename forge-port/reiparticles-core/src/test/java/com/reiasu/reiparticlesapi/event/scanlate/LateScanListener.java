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
package com.reiasu.reiparticlesapi.event.scanlate;

import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.annotations.events.EventListener;

import java.util.concurrent.atomic.AtomicInteger;

@EventListener(modId = "late-test")
public final class LateScanListener {
    public static final AtomicInteger CALLS = new AtomicInteger();

    @EventHandler
    public void onLateEvent(LateScanEvent event) {
        CALLS.incrementAndGet();
    }
}
