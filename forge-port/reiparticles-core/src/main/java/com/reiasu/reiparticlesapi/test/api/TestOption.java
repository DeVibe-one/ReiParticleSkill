// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.test.api;

public interface TestOption {
    void start();

    void stop();

    boolean isValid();

    void onFailed();

    void onSuccess();

    String optionID();

    void doTick();
}
