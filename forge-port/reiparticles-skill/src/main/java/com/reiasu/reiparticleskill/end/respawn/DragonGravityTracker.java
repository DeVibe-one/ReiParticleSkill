// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks ender dragons whose gravity has been temporarily disabled
 * and restores it after a configured number of ticks.
 */
final class DragonGravityTracker {

    private final Map<UUID, Long> restoreTicks = new HashMap<>();

    void setNearestNoGravity(ServerLevel level, Vec3 origin, double radius, long restoreAfterTicks) {
        List<EnderDragon> dragons = level.getEntitiesOfClass(
                EnderDragon.class,
                new AABB(origin, origin).inflate(radius),
                EnderDragon::isAlive
        );
        EnderDragon nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (EnderDragon dragon : dragons) {
            double d = dragon.position().distanceToSqr(origin);
            if (d < bestDistance) {
                bestDistance = d;
                nearest = dragon;
            }
        }
        if (nearest == null) {
            return;
        }
        nearest.setNoGravity(true);
        restoreTicks.put(nearest.getUUID(), level.getGameTime() + Math.max(1L, restoreAfterTicks));
    }

    void tick(ServerLevel level) {
        if (restoreTicks.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Long>> iterator = restoreTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (now < entry.getValue()) {
                continue;
            }
            net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof EnderDragon dragon && dragon.isAlive()) {
                dragon.setNoGravity(false);
            }
            iterator.remove();
        }
    }

    void restoreAll(ServerLevel level) {
        if (restoreTicks.isEmpty()) {
            return;
        }
        for (UUID uuid : restoreTicks.keySet()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
            if (entity instanceof EnderDragon dragon && dragon.isAlive()) {
                dragon.setNoGravity(false);
            }
        }
    }

    void clear() {
        restoreTicks.clear();
    }

    int trackedCount() {
        return restoreTicks.size();
    }
}
