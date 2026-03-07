// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.entity.projectile;

import com.reiasu.reiparticlesapi.event.events.entity.EntityEvent;
import net.minecraft.world.entity.projectile.Projectile;

public abstract class ProjectileEvent extends EntityEvent {
    protected ProjectileEvent(Projectile entity) {
        super(entity);
    }
}

