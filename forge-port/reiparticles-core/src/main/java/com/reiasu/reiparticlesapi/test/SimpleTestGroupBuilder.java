// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.test;

import com.reiasu.reiparticlesapi.test.api.TestGroup;
import com.reiasu.reiparticlesapi.test.api.TestGroupBuilder;
import net.minecraft.server.level.ServerPlayer;

public final class SimpleTestGroupBuilder implements TestGroupBuilder {
    private final String id;
    private final ServerPlayer user;

    public SimpleTestGroupBuilder(String id, ServerPlayer user) {
        this.id = id;
        this.user = user;
    }

    @Override
    public String groupID() {
        return id;
    }

    @Override
    public TestGroup build() {
        return new SimpleTestGroup(id, user);
    }
}
