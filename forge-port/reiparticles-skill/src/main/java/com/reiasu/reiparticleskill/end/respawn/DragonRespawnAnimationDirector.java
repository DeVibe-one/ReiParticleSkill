// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.CollectEnderPowerEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.CollectPillarsEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.EndBeamExplosionEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.EndCrystalEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.EndCrystalStyleEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.PillarFourierBeamEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.RespawnEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.ShockwaveWallEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DragonRespawnAnimationDirector {
    private static final int PILLAR_EVENT_DELAY_TICKS = 39;
    private static final Vec3 ANCHOR_OFFSET = new Vec3(0.0, 130.5, 0.0);
    private static final double GRAVITY_RANGE = 32.0;
    private static final double CRYSTAL_SEARCH_RADIUS = 96.0;
    private static final double PILLAR_RING_RADIUS = 56.0;
    private static final double PILLAR_RING_Y_OFFSET = 80.0;

    private final ClientEmitterFactory clientEmitters = new ClientEmitterFactory();
    private final DragonGravityTracker gravityTracker = new DragonGravityTracker();
    private final List<RespawnEmitter> activeEmitters = new ArrayList<>();
    private final Set<RespawnEmitter> pulseScopedEmitters = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<PillarCrafter> activePillarCrafters = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private final DragonRespawnPulseTracker pulseTracker = new DragonRespawnPulseTracker(PILLAR_EVENT_DELAY_TICKS);
    private EndRespawnPhase activePhase;
    private ServerLevel activeLevel;
    private boolean active;
    private int pillarPulseIndex;

    public void setup(ServerLevel level, Vec3 center) {
        if (active) {
            return;
        }
        active = true;
        activeLevel = level;
        resetState();
    }

    public int next(ServerLevel level, Vec3 center, EndRespawnPhase phase, long phaseTick) {
        if (!active) {
            setup(level, center);
        }
        activeLevel = level;
        if (activePhase != phase) {
            if (!shouldPreserveMidChain(activePhase, phase)) {
                reconfigureEmittersForPhase(level, center, phase);
            }
            activePhase = phase;
            if (phase != EndRespawnPhase.SUMMON_PILLARS) {
                pulseTracker.clear();
            }
            if (phase == EndRespawnPhase.BEFORE_END_WAITING) {
                activeEmitters.removeIf(pulseScopedEmitters::contains);
                pulseScopedEmitters.clear();
            }
            if (phase == EndRespawnPhase.BEFORE_END_WAITING
                    || phase == EndRespawnPhase.END
                    || phase == EndRespawnPhase.START) {
                activePillarCrafters.clear();
            }
        }

        if (phase == EndRespawnPhase.SUMMON_PILLARS) {
            maybeTriggerPillarPulse(level, center, phaseTick);
        }
        if (phase == EndRespawnPhase.END && phaseTick == 0) {
            handleEnd(level, center);
        }
        gravityTracker.tick(level);
        return tickEmitters(level, center);
    }

    public void tick(ServerLevel level, Vec3 center) {
        if (!active) {
            return;
        }
        gravityTracker.tick(level);
        tickEmitters(level, center);
    }

    public void cancel() {
        if (activeLevel != null) {
            gravityTracker.restoreAll(activeLevel);
        }
        active = false;
        activeLevel = null;
        resetState();
    }

    private void resetState() {
        activePhase = null;
        clientEmitters.cancelAll();
        activeEmitters.clear();
        pulseScopedEmitters.clear();
        activePillarCrafters.clear();
        pillarPulseIndex = 0;
        pulseTracker.clear();
        gravityTracker.clear();
    }

    public String debugState() {
        String phaseId = activePhase == null ? "none" : activePhase.id();
        return "active=" + active
                + ", phase=" + phaseId
                + ", emitters=" + activeEmitters.size()
                + ", pulse_emitters=" + pulseScopedEmitters.size()
                + ", pillar_crafters=" + activePillarCrafters.size()
                + ", pending_pulses=" + pulseTracker.pendingPulseCount()
                + ", no_gravity_dragons=" + gravityTracker.trackedCount();
    }

    public void handleOncePillars(ServerLevel level, Vec3 center, Vec3 pillarCenter) {
        handleOncePillars(level, center, pillarCenter, null);
    }

    private void handleOncePillars(ServerLevel level, Vec3 center, Vec3 pillarCenter, UUID preferredCrystalId) {
        Vec3 anchor = pillarCenter == null ? center : pillarCenter;
        EndCrystal crystal = DragonRespawnCrystalResolver.resolvePulseCrystal(level, center, anchor, preferredCrystalId, CRYSTAL_SEARCH_RADIUS);
        Vec3 start = anchor;
        Vec3 target = Vec3.ZERO;
        if (crystal != null) {
            BlockPos beamTarget = crystal.getBeamTarget();
            if (beamTarget != null) {
                start = Vec3.atCenterOf(beamTarget);
            }
            target = crystal.position().add(0.0, 1.7, 0.0);
        }

        EndCrystalStyleEmitter styleEmitter = new EndCrystalStyleEmitter(start, target, 500)
                .setRotateSpeed(ClientEmitterFactory.ROT_FAST);
        activeEmitters.add(styleEmitter);
        pulseScopedEmitters.add(styleEmitter);

        activePillarCrafters.add(new PillarCrafter(start, target, crystal == null ? null : crystal.getUUID()));

        CollectPillarsEmitter burst = new CollectPillarsEmitter(500)
                .setAnchorOffset(anchor.subtract(center).add(0.0, -0.5, 0.0))
                .setDiscrete(2.0)
                .setRadiusMin(3.0)
                .setRadiusMax(8.0)
                .setCountMin(30)
                .setCountMax(50)
                .setVerticalMaxSpeedMultiplier(3.0)
                .setVerticalMinSpeedMultiplier(0.1)
                .setHorizontalMaxSpeedMultiplier(1.0)
                .setHorizontalMinSpeedMultiplier(0.08)
                .setParticleMinAge(20)
                .setParticleMaxAge(40)
                .setSizeMin(0.5f)
                .setSizeMax(1.2f)
                .setSpeed(1.5);
        activeEmitters.add(burst);
        pulseScopedEmitters.add(burst);
        pillarPulseIndex++;
    }

    public void handleEnd(ServerLevel level, Vec3 center) {
        activeEmitters.removeIf(emitter -> emitter instanceof EndBeamExplosionEmitter);
        EndBeamExplosionEmitter emitter = new EndBeamExplosionEmitter(160)
                .setAnchorOffset(ANCHOR_OFFSET)
                .setParticleMaxAge(130)
                .setParticleMinAge(50)
                .setCountMin(800)
                .setCountMax(1200)
                .setDiscrete(0.5)
                .setMinSpeed(0.8)
                .setMaxSpeed(6.0)
                .setSizeMin(0.8)
                .setSizeMax(1.8)
                .setDrag(0.96);
        activeEmitters.add(emitter);
        gravityTracker.setNearestNoGravity(level, center.add(ANCHOR_OFFSET), GRAVITY_RANGE, 20L);

        // Fourier beam eruption at each obsidian pillar
        List<EndCrystal> crystals = level.getEntitiesOfClass(
                EndCrystal.class,
                new AABB(center, center).inflate(CRYSTAL_SEARCH_RADIUS),
                EndCrystal::isAlive
        );
        for (EndCrystal crystal : crystals) {
            Vec3 base = crystal.position().add(0.0, -1.0, 0.0);
            activeEmitters.add(new PillarFourierBeamEmitter(base, 80));
        }
        // Also spawn one at the center portal
        activeEmitters.add(new PillarFourierBeamEmitter(center.add(0.0, 1.0, 0.0), 80));

        // Expanding shockwave wall from center
        activeEmitters.add(new ShockwaveWallEmitter(50));
    }

    private void reconfigureEmittersForPhase(ServerLevel level, Vec3 center, EndRespawnPhase phase) {
        clientEmitters.cancelAll();
        activeEmitters.clear();
        pulseScopedEmitters.clear();
        activePillarCrafters.clear();
        pulseTracker.clear();
        switch (phase) {
            case START -> {
                CollectEnderPowerEmitter emitter = new CollectEnderPowerEmitter(600)
                        .setR(60.0)
                        .setRadiusOffset(40.0)
                        .setCountMin(50)
                        .setCountMax(100)
                        .setSpeed(1.9)
                        .setOriginOffset(new Vec3(0.0, 4.0, 0.0))
                        .setTargetOffset(new Vec3(0.0, 4.0, 0.0));
                activeEmitters.add(emitter);
                clientEmitters.spawnForStart(level, center);
            }
            case SUMMON_PILLARS, SUMMONING_DRAGON, BEFORE_END_WAITING -> {
                CollectPillarsEmitter emitter = new CollectPillarsEmitter(500)
                        .setAnchorOffset(new Vec3(0.0, -1.0, 0.0))
                        .setDiscrete(2.0)
                        .setRadiusMin(16.0)
                        .setRadiusMax(18.0)
                        .setCountMin(40)
                        .setCountMax(70)
                        .setVerticalMaxSpeedMultiplier(4.5)
                        .setVerticalMinSpeedMultiplier(0.1)
                        .setHorizontalMaxSpeedMultiplier(1.5)
                        .setHorizontalMinSpeedMultiplier(0.04)
                        .setParticleMinAge(40)
                        .setParticleMaxAge(60)
                        .setSizeMin(0.6f)
                        .setSizeMax(1.5f)
                        .setSpeed(1.5);
                activeEmitters.add(emitter);
                clientEmitters.spawnForSummon(level, center);
            }
            case END -> handleEnd(level, center);
        }
    }

    private void maybeTriggerPillarPulse(ServerLevel level, Vec3 center, long phaseTick) {
        List<EndCrystal> crystals = level.getEntitiesOfClass(
                EndCrystal.class,
                new AABB(center, center).inflate(CRYSTAL_SEARCH_RADIUS),
                EndCrystal::isAlive
        );
        Set<UUID> seen = new HashSet<>();
        for (EndCrystal crystal : crystals) {
            UUID uuid = crystal.getUUID();
            seen.add(uuid);
            BlockPos beam = crystal.getBeamTarget();
            if (beam != null) {
                pulseTracker.observeCrystal(uuid, beam, phaseTick);
            }
        }
        pulseTracker.removeMissing(seen);

        List<DragonRespawnPulseTracker.PillarPulseTrigger> duePulses = pulseTracker.drainDuePulses(phaseTick);
        if (!duePulses.isEmpty()) {
            for (DragonRespawnPulseTracker.PillarPulseTrigger trigger : duePulses) {
                handleOncePillars(level, center, Vec3.atCenterOf(trigger.beamTarget()), trigger.crystalId());
            }
            return;
        }

        if (!pulseTracker.shouldFallback()) {
            return;
        }
        Vec3 fallback = chooseFallbackPillarTarget(crystals, center);
        handleOncePillars(level, center, fallback);
        pulseTracker.markPulseTriggered(phaseTick);
    }

    private Vec3 chooseFallbackPillarTarget(List<EndCrystal> crystals, Vec3 center) {
        if (!crystals.isEmpty()) {
            EndCrystal crystal = crystals.get(Math.floorMod(pillarPulseIndex, crystals.size()));
            BlockPos beam = crystal.getBeamTarget();
            if (beam != null) {
                return Vec3.atCenterOf(beam);
            }
            return crystal.position();
        }
        double angle = (Math.PI * 2.0 * (pillarPulseIndex % 10)) / 10.0;
        return center.add(Math.cos(angle) * PILLAR_RING_RADIUS, PILLAR_RING_Y_OFFSET, Math.sin(angle) * PILLAR_RING_RADIUS);
    }

    private int tickEmitters(ServerLevel level, Vec3 center) {
        int emitted = 0;
        emitted += tickPillarCrafters(level);
        Iterator<RespawnEmitter> iterator = activeEmitters.iterator();
        while (iterator.hasNext()) {
            RespawnEmitter emitter = iterator.next();
            emitted += emitter.tick(level, center);
            if (emitter.done()) {
                pulseScopedEmitters.remove(emitter);
                iterator.remove();
            }
        }
        return emitted;
    }

    private int tickPillarCrafters(ServerLevel level) {
        if (activePillarCrafters.isEmpty()) {
            return 0;
        }
        List<RespawnEmitter> crafted = new ArrayList<>();
        Iterator<PillarCrafter> iterator = activePillarCrafters.iterator();
        while (iterator.hasNext()) {
            PillarCrafter crafter = iterator.next();
            if (crafter.shouldCancel(level, activePhase)) {
                iterator.remove();
                continue;
            }
            if (crafter.shouldCraftNow()) {
                Vec3 target = crafter.resolveTarget(level);
                EndCrystalEmitter emitter = new EndCrystalEmitter(crafter.start(), target, 60)
                        .setRotationSpeed(Math.PI / 32.0)
                        .setMovementSpeed(randomBetween(1.5, 2.0))
                        .setCurrentRotation(randomBetween(-Math.PI, Math.PI))
                        .setCountMin(3)
                        .setCountMax(5)
                        .setMaxRadius(4.0)
                        .setParticleMinAge(10)
                        .setParticleMaxAge(20)
                        .setRefiner(6.0);
                crafted.add(emitter);
                pulseScopedEmitters.add(emitter);
            }
            crafter.advanceTick();
            if (crafter.expired()) {
                iterator.remove();
            }
        }
        activeEmitters.addAll(crafted);
        return 0;
    }

    private boolean shouldPreserveMidChain(EndRespawnPhase from, EndRespawnPhase to) {
        return isMidPhase(from) && isMidPhase(to);
    }

    private boolean isMidPhase(EndRespawnPhase phase) {
        return phase == EndRespawnPhase.SUMMON_PILLARS
                || phase == EndRespawnPhase.SUMMONING_DRAGON
                || phase == EndRespawnPhase.BEFORE_END_WAITING;
    }

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (Math.abs(hi - lo) < 1.0E-6) {
            return lo;
        }
        return lo + random.nextDouble() * (hi - lo);
    }

    private static final class PillarCrafter {
        private static final int CRAFT_INTERVAL = 15;
        private static final int MAX_TICKS = 500;

        private final Vec3 start;
        private Vec3 target;
        private final UUID crystalId;
        private int tick;

        private PillarCrafter(Vec3 start, Vec3 target, UUID crystalId) {
            this.start = start;
            this.target = target;
            this.crystalId = crystalId;
        }

        private Vec3 start() {
            return start;
        }

        private boolean shouldCraftNow() {
            return tick % CRAFT_INTERVAL == 0;
        }

        private void advanceTick() {
            tick++;
        }

        private boolean expired() {
            return tick > MAX_TICKS;
        }

        private boolean shouldCancel(ServerLevel level, EndRespawnPhase phase) {
            if (phase == EndRespawnPhase.BEFORE_END_WAITING || phase == EndRespawnPhase.END) {
                return true;
            }
            if (crystalId == null) {
                return true;
            }
            net.minecraft.world.entity.Entity entity = level.getEntity(crystalId);
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) {
                return true;
            }
            return crystal.getBeamTarget() == null;
        }

        private Vec3 resolveTarget(ServerLevel level) {
            if (crystalId == null) {
                return target;
            }
            net.minecraft.world.entity.Entity entity = level.getEntity(crystalId);
            if (entity instanceof EndCrystal crystal && crystal.isAlive()) {
                target = crystal.position().add(0.0, 1.7, 0.0);
            }
            return target;
        }
    }
}

