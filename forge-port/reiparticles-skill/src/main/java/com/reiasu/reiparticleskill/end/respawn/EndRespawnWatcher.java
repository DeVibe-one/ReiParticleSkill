// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EndRespawnWatcher {
    private static final double PORTAL_RADIUS = 14.0;
    private static final Map<String, SyntheticRespawnTracker> SYNTHETIC_TRACKERS = new HashMap<>();

    private EndRespawnWatcher() {
    }

    public static void tickServer(MinecraftServer server, EndRespawnStateBridge bridge, Logger logger) {
        boolean foundRespawning = false;

        for (ServerLevel level : server.getAllLevels()) {
            if (!Level.END.equals(level.dimension())) {
                continue;
            }

            EndDragonFight fight = level.getDragonFight();
            if (fight == null) {
                continue;
            }

            Vec3 center = EndDragonFightHelper.portalCenter(fight);
            String levelId = level.dimension().location().toString();
            SyntheticRespawnTracker tracker = SYNTHETIC_TRACKERS.computeIfAbsent(levelId, ignored -> new SyntheticRespawnTracker());

            int crystalCount = resolveCrystalCount(fight, level, center);
            Optional<EndRespawnPhase> phase = EndDragonFightHelper.detectPhase(fight);
            if (phase.isPresent()) {
                tracker.observeDirectPhase(crystalCount);
            } else {
                phase = tracker.update(crystalCount);
            }
            if (phase.isEmpty()) {
                continue;
            }

            foundRespawning = true;
            bridge.setup(level, center);
            bridge.next(level, center, phase.get(), logger);
            break;
        }

        if (!foundRespawning && bridge.isActive()) {
            bridge.cancel(logger);
        }
    }

    public static Optional<RespawnProbe> probeServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (!Level.END.equals(level.dimension())) {
                continue;
            }

            EndDragonFight fight = level.getDragonFight();
            if (fight == null) {
                continue;
            }

            Vec3 center = EndDragonFightHelper.portalCenter(fight);
            int fightCrystals = Math.max(0, fight.getCrystalsAlive());
            int portalAreaCrystals = Math.max(0, countPortalCrystals(level, center));
            int resolvedCrystals = Math.max(fightCrystals, portalAreaCrystals);
            Optional<EndRespawnPhase> directPhase = EndDragonFightHelper.detectPhase(fight);
            return Optional.of(new RespawnProbe(
                    level.dimension().location().toString(),
                    center,
                    directPhase.map(EndRespawnPhase::id).orElse("none"),
                    fightCrystals,
                    portalAreaCrystals,
                    resolvedCrystals
            ));
        }
        return Optional.empty();
    }

    private static int countPortalCrystals(ServerLevel level, Vec3 center) {
        AABB box = new AABB(
                center.x - PORTAL_RADIUS,
                center.y - 24.0,
                center.z - PORTAL_RADIUS,
                center.x + PORTAL_RADIUS,
                center.y + 48.0,
                center.z + PORTAL_RADIUS
        );
        return level.getEntitiesOfClass(EndCrystal.class, box, crystal -> crystal != null && crystal.isAlive()).size();
    }

    private static int resolveCrystalCount(EndDragonFight fight, ServerLevel level, Vec3 center) {
        int fromFight = Math.max(0, fight.getCrystalsAlive());
        int fromPortalArea = Math.max(0, countPortalCrystals(level, center));
        return Math.max(fromFight, fromPortalArea);
    }

    // Synthetic phase tick thresholds — aligned with Fabric mixin timing.
    // Extracted to constants so they can be tuned or tested independently.
    static final long PHASE_START_END            = 150L;
    static final long PHASE_SUMMON_PILLARS_END   = 650L;
    static final long PHASE_SUMMONING_DRAGON_END = 750L;
    static final long PHASE_BEFORE_END_END       = 780L;
    private static final long TOLERANCE          = 40L;

    static EndRespawnPhase syntheticPhaseForTick(long tick) {
        return syntheticPhaseForTick(tick, -1);
    }

    static EndRespawnPhase syntheticPhaseForTick(long tick, int crystalCount) {
        // Crystal-count override: real-world signals correct tick drift
        // caused by server lag or mod conflicts.
        if (crystalCount >= 0) {
            if (crystalCount >= 4 && tick > PHASE_START_END + TOLERANCE) {
                if (tick < PHASE_SUMMON_PILLARS_END + TOLERANCE) {
                    return EndRespawnPhase.SUMMON_PILLARS;
                }
            }
            if (crystalCount == 0 && tick > PHASE_START_END) {
                return tick < PHASE_BEFORE_END_END + TOLERANCE
                        ? EndRespawnPhase.BEFORE_END_WAITING
                        : EndRespawnPhase.END;
            }
        }

        // Tick-based fallback
        if (tick < PHASE_START_END) {
            return EndRespawnPhase.START;
        }
        if (tick < PHASE_SUMMON_PILLARS_END) {
            return EndRespawnPhase.SUMMON_PILLARS;
        }
        if (tick < PHASE_SUMMONING_DRAGON_END) {
            return EndRespawnPhase.SUMMONING_DRAGON;
        }
        if (tick < PHASE_BEFORE_END_END) {
            return EndRespawnPhase.BEFORE_END_WAITING;
        }
        return EndRespawnPhase.END;
    }

    public record RespawnProbe(
            String levelId,
            Vec3 center,
            String directPhase,
            int fightCrystals,
            int portalAreaCrystals,
            int resolvedCrystals
    ) {
    }

    private static final class SyntheticRespawnTracker {
        private long tick;
        private int quietTicks;
        private boolean active;

        void observeDirectPhase(int crystalCount) {
            if (!active) {
                active = true;
                tick = 0L;
                quietTicks = 0;
                return;
            }

            tick++;
            if (crystalCount >= 4) {
                quietTicks = 0;
            }
        }

        Optional<EndRespawnPhase> update(int crystalCount) {
            if (crystalCount >= 4) {
                if (!active) {
                    active = true;
                    tick = 0L;
                    quietTicks = 0;
                } else {
                    tick++;
                }
                return Optional.of(syntheticPhaseForTick(tick, crystalCount));
            }

            if (!active) {
                return Optional.empty();
            }

            // Keep a short tail after crystals disappear so END effects can finish.
            quietTicks++;
            tick++;
            if (quietTicks <= 50) {
                return Optional.of(EndRespawnPhase.END);
            }

            reset();
            return Optional.empty();
        }

        void reset() {
            tick = 0L;
            quietTicks = 0;
            active = false;
        }

    }
}
