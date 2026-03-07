// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class DragonRespawnPulseTracker {
    private final int pulseDelayTicks;
    private final Map<UUID, BlockPos> crystalBeamTargets = new HashMap<>();
    private final Map<BlockPos, PendingPillarPulse> pendingPillarPulseTicks = new HashMap<>();
    private long lastPillarPulseTick = Long.MIN_VALUE;

    DragonRespawnPulseTracker(int pulseDelayTicks) {
        this.pulseDelayTicks = pulseDelayTicks;
    }

    void observeCrystal(UUID crystalId, BlockPos beamTarget, long phaseTick) {
        BlockPos target = beamTarget.immutable();
        BlockPos previous = crystalBeamTargets.put(crystalId, target);
        if (previous == null || !previous.equals(target)) {
            pendingPillarPulseTicks.put(target, new PendingPillarPulse(phaseTick + pulseDelayTicks, crystalId));
        }
    }

    void removeMissing(Set<UUID> seenCrystals) {
        crystalBeamTargets.keySet().removeIf(uuid -> !seenCrystals.contains(uuid));
    }

    List<PillarPulseTrigger> drainDuePulses(long phaseTick) {
        List<PillarPulseTrigger> triggers = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, PendingPillarPulse>> iterator = pendingPillarPulseTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, PendingPillarPulse> pending = iterator.next();
            PendingPillarPulse pulse = pending.getValue();
            if (pulse.triggerTick() > phaseTick) {
                continue;
            }
            triggers.add(new PillarPulseTrigger(pending.getKey(), pulse.crystalId()));
            iterator.remove();
        }
        if (!triggers.isEmpty()) {
            lastPillarPulseTick = phaseTick;
        }
        return triggers;
    }

    boolean shouldFallback() {
        return pendingPillarPulseTicks.isEmpty()
                && crystalBeamTargets.isEmpty()
                && lastPillarPulseTick == Long.MIN_VALUE;
    }

    void markPulseTriggered(long phaseTick) {
        lastPillarPulseTick = phaseTick;
    }

    int pendingPulseCount() {
        return pendingPillarPulseTicks.size();
    }

    void clear() {
        crystalBeamTargets.clear();
        pendingPillarPulseTicks.clear();
        lastPillarPulseTick = Long.MIN_VALUE;
    }

    record PillarPulseTrigger(BlockPos beamTarget, UUID crystalId) {
    }

    private record PendingPillarPulse(long triggerTick, UUID crystalId) {
    }
}
