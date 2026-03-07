// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.api;

import java.util.Objects;
import java.util.function.Consumer;

public final class EventExecutor {
    private final String modId;
    private final Consumer<ReiEvent> executor;

    public EventExecutor(String modId, Consumer<ReiEvent> executor) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public String getModId() {
        return modId;
    }

    public Consumer<ReiEvent> getExecutor() {
        return executor;
    }
}

