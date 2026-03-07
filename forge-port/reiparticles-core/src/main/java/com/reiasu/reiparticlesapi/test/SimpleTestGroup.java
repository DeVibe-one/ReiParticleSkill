// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.test;

import com.reiasu.reiparticlesapi.test.api.TestGroup;
import com.reiasu.reiparticlesapi.test.api.TestOption;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SimpleTestGroup implements TestGroup {
    private final String groupId;
    private final ServerPlayer user;
    private final List<Supplier<TestOption>> optionSuppliers = new ArrayList<>();
    private int currentIndex = -1;
    private TestOption currentOption;
    private boolean done;

    public SimpleTestGroup(String groupId, ServerPlayer user) {
        this.groupId = groupId;
        this.user = user;
    }

    @Override
    public ServerPlayer getUser() {
        return user;
    }

    @Override
    public TestGroup appendOption(Supplier<TestOption> supplier) {
        if (supplier != null) {
            optionSuppliers.add(supplier);
        }
        return this;
    }

    @Override
    public void init() {
        if (optionSuppliers.isEmpty()) {
            appendOption(() -> new SimpleTestOption("smoke"));
            appendOption(() -> new SimpleTestOption("lifecycle"));
        }
    }

    @Override
    public void start() {
        init();
        moveToNextOption();
    }

    @Override
    public void stop() {
        if (currentOption != null) {
            currentOption.stop();
            currentOption = null;
        }
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public TestOption skipCurrent() {
        TestOption old = currentOption;
        if (old != null) {
            old.stop();
            onOptionSuccess(old);
        }
        moveToNextOption();
        return old;
    }

    @Override
    public void doTick() {
        if (done || currentOption == null) {
            return;
        }
        try {
            currentOption.doTick();
            if (!currentOption.isValid()) {
                onOptionSuccess(currentOption);
                moveToNextOption();
            }
        } catch (RuntimeException | AssertionError e) {
            onOptionFailure(e, currentOption);
            moveToNextOption();
        }
    }

    @Override
    public void onOptionFailure(Throwable t, TestOption option) {
        option.onFailed();
    }

    @Override
    public void onOptionSuccess(TestOption option) {
        option.onSuccess();
    }

    @Override
    public void onGroupFinished() {
        done = true;
    }

    @Override
    public String groupID() {
        return groupId;
    }

    private void moveToNextOption() {
        currentIndex++;
        if (currentIndex >= optionSuppliers.size()) {
            currentOption = null;
            onGroupFinished();
            return;
        }
        currentOption = optionSuppliers.get(currentIndex).get();
        if (currentOption == null) {
            moveToNextOption();
            return;
        }
        currentOption.start();
    }
}
