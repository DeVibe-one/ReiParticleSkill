// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages all active {@link Barrage} instances on the server,
 * handling spawning, ticking, and collision queries.
 */
public final class BarrageManager {
    public static final BarrageManager INSTANCE = new BarrageManager();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ConcurrentLinkedDeque<Barrage> barrages = new ConcurrentLinkedDeque<>();

    private BarrageManager() {
    }

    public List<Barrage> collectClipBarrages(ServerLevel world, AABB box) {
        List<Barrage> result = new ArrayList<>();
        for (Barrage barrage : barrages) {
            if (!barrage.getValid()) {
                continue;
            }
            if (!world.equals(barrage.getWorld())) {
                continue;
            }
            if (barrage.noclip()) {
                continue;
            }
            if (box.contains(barrage.getLoc()) || box.intersects(barrage.getHitBox().ofBox(barrage.getLoc()))) {
                result.add(barrage);
            }
        }
        return result;
    }

    public void spawn(Barrage barrage) {
        if (barrage == null || !barrage.getValid()) {
            return;
        }
        try {
            spawnOnWorld(barrage);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to spawn barrage {} ({})", barrage.getUuid(), barrage.getClass().getName(), e);
            return;
        }
        barrages.add(barrage);
    }

    public void doTick() {
        if (barrages.isEmpty()) {
            return;
        }
        barrages.removeIf(this::tickAndShouldDiscard);
    }

    public void clear() {
        for (Barrage barrage : barrages) {
            if (barrage == null) {
                continue;
            }
            try {
                barrage.getBindControl().cancel();
            } catch (RuntimeException e) {
                LOGGER.debug("Failed to cancel barrage control during cleanup", e);
            }
        }
        barrages.clear();
    }

    public int activeCount() {
        return barrages.size();
    }

    public List<Barrage> snapshot() {
        return List.copyOf(barrages);
    }

    private boolean tickAndShouldDiscard(Barrage barrage) {
        if (barrage == null || !barrage.getValid()) {
            return true;
        }
        try {
            barrage.tick();
        } catch (RuntimeException e) {
            LOGGER.warn("Barrage {} ({}) failed during server tick; removing barrage",
                    barrage.getUuid(), barrage.getClass().getName(), e);
            try {
                barrage.getBindControl().cancel();
            } catch (RuntimeException cancelError) {
                LOGGER.debug("Failed to cancel barrage control after tick failure", cancelError);
            }
            return true;
        }
        return !barrage.getValid();
    }

    private void spawnOnWorld(Barrage barrage) {
        ServerLevel world = barrage.getWorld();
        Vec3 loc = barrage.getLoc();
        barrage.getBindControl().spawnInWorld(world, loc);
        barrage.setLaunch(true);
    }
}
