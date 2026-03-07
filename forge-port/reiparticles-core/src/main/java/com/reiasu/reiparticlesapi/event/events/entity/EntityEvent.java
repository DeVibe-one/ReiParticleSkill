// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity;

import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import net.minecraft.world.entity.Entity;

public abstract class EntityEvent extends ReiEvent {
    private final Entity entity;

    protected EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public final Entity getEntity() {
        return entity;
    }
}

