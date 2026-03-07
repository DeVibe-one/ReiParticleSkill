// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

final class DragonRespawnCrystalResolver {
    private DragonRespawnCrystalResolver() {
    }

    static EndCrystal resolvePulseCrystal(ServerLevel level, Vec3 center, Vec3 anchor, UUID preferredCrystalId,
                                          double searchRadius) {
        if (preferredCrystalId != null) {
            Entity entity = level.getEntity(preferredCrystalId);
            if (entity instanceof EndCrystal preferred && preferred.isAlive()) {
                return preferred;
            }
        }

        List<EndCrystal> nearAnchor = level.getEntitiesOfClass(
                EndCrystal.class,
                new AABB(anchor, anchor).inflate(8.0),
                EndCrystal::isAlive
        );
        EndCrystal closestNearAnchor = nearestCrystalTo(anchor, nearAnchor);
        if (closestNearAnchor != null) {
            return closestNearAnchor;
        }

        List<EndCrystal> aroundPortal = level.getEntitiesOfClass(
                EndCrystal.class,
                new AABB(center, center).inflate(searchRadius),
                EndCrystal::isAlive
        );
        return nearestCrystalTo(anchor, aroundPortal);
    }

    private static EndCrystal nearestCrystalTo(Vec3 pos, List<EndCrystal> crystals) {
        EndCrystal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EndCrystal candidate : crystals) {
            double distance = candidate.position().distanceToSqr(pos);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
