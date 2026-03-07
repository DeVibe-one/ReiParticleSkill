// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages all active {@link Barrage} instances on the server,
 * handling spawning, ticking, and collision queries.
 */
public final class BarrageManager {
    public static final BarrageManager INSTANCE = new BarrageManager();

    private final ConcurrentLinkedDeque<Barrage> barrages = new ConcurrentLinkedDeque<>();

    private BarrageManager() {}

    public List<Barrage> collectClipBarrages(ServerLevel world, AABB box) {
        List<Barrage> result = new ArrayList<>();
        for (Barrage b : barrages) {
            if (!b.getValid()) continue;
            if (!world.equals(b.getWorld())) continue;
            if (b.noclip()) continue;
            if (box.contains(b.getLoc()) || box.intersects(b.getHitBox().ofBox(b.getLoc()))) {
                result.add(b);
            }
        }
        return result;
    }

    public void spawn(Barrage barrage) {
        spawnOnWorld(barrage);
        barrages.add(barrage);
    }

    public void doTick() {
        barrages.removeIf(b -> {
            b.tick();
            return !b.getValid();
        });
    }

    private void spawnOnWorld(Barrage barrage) {
        ServerLevel world = barrage.getWorld();
        Vec3 loc = barrage.getLoc();
        barrage.getBindControl().spawnInWorld(world, loc);
        barrage.setLaunch(true);
    }
}
