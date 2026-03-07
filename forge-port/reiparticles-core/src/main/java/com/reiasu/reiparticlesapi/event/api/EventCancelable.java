// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.api;

public interface EventCancelable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}

