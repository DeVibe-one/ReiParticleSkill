// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.test.api;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public interface TestGroup {
    ServerPlayer getUser();

    TestGroup appendOption(Supplier<TestOption> supplier);

    void init();

    void start();

    default void stop() {
    }

    boolean isDone();

    TestOption skipCurrent();

    void doTick();

    void onOptionFailure(Throwable t, TestOption option);

    void onOptionSuccess(TestOption option);

    void onGroupFinished();

    String groupID();
}

