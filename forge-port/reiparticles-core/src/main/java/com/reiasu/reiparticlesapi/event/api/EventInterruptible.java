// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.api;

public interface EventInterruptible {
    boolean isInterrupted();

    void setInterrupted(boolean interrupted);
}

