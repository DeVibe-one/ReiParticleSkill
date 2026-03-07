// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class EndRespawnStateBridge {
    private static final long START_MIN_TICKS = 150L;
    private static final long SUMMONING_WAIT_LIMIT = 130L;
    private static final long BEFORE_END_WAITING_MIN = 30L;

    private EndRespawnSnapshot snapshot;
    private boolean beforeEndWaitingEmitted;
    private boolean directorFailedLogged;
    private final DragonRespawnAnimationDirector director = new DragonRespawnAnimationDirector();

    public void setup(ServerLevel level, Vec3 center) {
        if (snapshot == null) {
            snapshot = new EndRespawnSnapshot(level.dimension().location().toString(), center, EndRespawnPhase.START, 0L);
            beforeEndWaitingEmitted = false;
            director.setup(level, center);
        }
    }

    public void cancel(Logger logger) {
        if (snapshot != null) {
            logger.info("End respawn bridge canceled at phase {}", snapshot.phase().id());
        }
        snapshot = null;
        beforeEndWaitingEmitted = false;
        directorFailedLogged = false;
        director.cancel();
    }

    public boolean isActive() {
        return snapshot != null;
    }

    public EndRespawnSnapshot current() {
        return snapshot;
    }

    public String directorDebugState() {
        return director.debugState();
    }

    public boolean next(ServerLevel level, Vec3 center, EndRespawnPhase phase, Logger logger) {
        EndRespawnPhase effectivePhase = normalizeIncomingPhase(phase);
        if (snapshot == null) {
            snapshot = new EndRespawnSnapshot(level.dimension().location().toString(), center, effectivePhase, 0L);
            emitWithFallback(level, center, effectivePhase, 0L, logger);
            logger.info("End respawn phase -> {}", effectivePhase.id());
            if (effectivePhase != EndRespawnPhase.SUMMONING_DRAGON) {
                beforeEndWaitingEmitted = false;
            }
            return true;
        }

        if (snapshot.phase() == effectivePhase) {
            long tick = snapshot.phaseTick() + 1;
            snapshot = new EndRespawnSnapshot(snapshot.levelId(), center, effectivePhase, tick);
            emitWithFallback(level, center, effectivePhase, tick, logger);
            maybeEmitBeforeEndWaiting(level, center, logger);
            return false;
        }

        snapshot = new EndRespawnSnapshot(snapshot.levelId(), center, effectivePhase, 0L);
        emitWithFallback(level, center, effectivePhase, 0L, logger);
        logger.info("End respawn phase -> {}", effectivePhase.id());
        if (effectivePhase != EndRespawnPhase.SUMMONING_DRAGON) {
            beforeEndWaitingEmitted = false;
        }
        return true;
    }

    private void maybeEmitBeforeEndWaiting(ServerLevel level, Vec3 center, Logger logger) {
        if (snapshot == null || snapshot.phase() != EndRespawnPhase.SUMMONING_DRAGON) {
            return;
        }
        long tick = snapshot.phaseTick();
        if (tick < 100L || tick > SUMMONING_WAIT_LIMIT) {
            return;
        }

        // Mirror Fabric mixin behaviour: while summoning tries to end too early,
        // force a waiting-phase overlay window.
        long waitingTick = tick - 100L;
        emitWithFallback(level, center, EndRespawnPhase.BEFORE_END_WAITING, waitingTick, logger);
        if (!beforeEndWaitingEmitted) {
            beforeEndWaitingEmitted = true;
            logger.info("End respawn phase -> {}", EndRespawnPhase.BEFORE_END_WAITING.id());
        }
    }

    private EndRespawnPhase normalizeIncomingPhase(EndRespawnPhase phase) {
        if (snapshot == null) {
            return phase;
        }
        EndRespawnPhase currentPhase = snapshot.phase();
        long currentTick = snapshot.phaseTick();

        if (currentPhase == EndRespawnPhase.START
                && phase != EndRespawnPhase.START
                && currentTick < START_MIN_TICKS) {
            return EndRespawnPhase.START;
        }

        if (currentPhase == EndRespawnPhase.SUMMONING_DRAGON
                && phase == EndRespawnPhase.END
                && currentTick <= SUMMONING_WAIT_LIMIT) {
            return EndRespawnPhase.BEFORE_END_WAITING;
        }

        if (currentPhase == EndRespawnPhase.BEFORE_END_WAITING
                && phase == EndRespawnPhase.END
                && currentTick < BEFORE_END_WAITING_MIN) {
            return EndRespawnPhase.BEFORE_END_WAITING;
        }

        return phase;
    }

    private void emitWithFallback(ServerLevel level, Vec3 center, EndRespawnPhase phase, long tick, Logger logger) {
        int emitted = 0;
        boolean failed = false;
        try {
            emitted = director.next(level, center, phase, tick);
        } catch (RuntimeException e) {
            failed = true;
            if (!directorFailedLogged) {
                directorFailedLogged = true;
                logger.warn("Respawn director failed once, falling back to preview path", e);
            }
        }
        if (emitted > 0) {
            if (tick == 0L) {
                logger.info("Respawn director phase {} started at center={} (emitted={})", phase.id(), center, emitted);
            }
            return;
        }
        if (!failed) {
            if (tick == 0L) {
                logger.warn("Respawn director emitted 0 particles at phase {}, skipping preview fallback", phase.id());
            }
            return;
        }
        if (tick == 0L) {
            logger.warn("Respawn director emitted 0 particles at phase {}, using preview fallback", phase.id());
        }
        EndRespawnEffectPreview.emit(level, center, phase, tick);
    }
}
