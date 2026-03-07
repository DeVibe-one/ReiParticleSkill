// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.util;

import com.reiasu.reiparticlesapi.ReiParticlesAPI;
import com.reiasu.reiparticlesapi.animation.Animate;
import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.animation.AnimateNode;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.utils.ServerCameraUtil;
import com.reiasu.reiparticleskill.particles.core.emitters.p1.CollectEnderPowerEmitter;
import com.reiasu.reiparticleskill.particles.core.emitters.p1.CollectPillarsEmitters;
import com.reiasu.reiparticleskill.particles.core.emitters.p1.EndBeamExplosionEmitter;
import com.reiasu.reiparticleskill.particles.core.emitters.p1.EndCrystalEmitters;
import com.reiasu.reiparticleskill.particles.preview.styles.EndCrystalStyle;
import com.reiasu.reiparticleskill.particles.preview.styles.EndDustStyle;
import com.reiasu.reiparticleskill.particles.preview.styles.EnderRespawnCenterStyle;
import com.reiasu.reiparticleskill.particles.preview.styles.EnderRespawnWaveCloudStyle;
import com.reiasu.reiparticleskill.particles.preview.styles.EnderRespawnWaveEnchantStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility singleton that orchestrates dragon respawn animation sequences.
 * Manages state transitions (setup ??pillars ??crystal converge ??end explosion)
 * using the Animate system and particle emitters/styles.
 * Server-side port of the Fabric original.
 */
public final class DragonRespawnStateUtil {
    public static final DragonRespawnStateUtil INSTANCE = new DragonRespawnStateUtil();

    private String current = "";
    private Animate currentAnimate;
    private ServerLevel world;
    private BlockPos pos;
    private EndDragonFight fight;
    private List<? extends EndCrystal> crystals = new ArrayList<>();

    private final Set<Animate> pillarsAnimates = new HashSet<>();
    private final Set<Animate> tickingAnimates = new HashSet<>();

    private DragonRespawnStateUtil() {
    }

    // ---- Accessors ----

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current == null ? "" : current;
    }

    public Animate getCurrentAnimate() {
        return currentAnimate;
    }

    public void setCurrentAnimate(Animate animate) {
        this.currentAnimate = animate;
    }

    public ServerLevel getWorld() {
        return world;
    }

    public void setWorld(ServerLevel world) {
        this.world = world;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public EndDragonFight getFight() {
        return fight;
    }

    public void setFight(EndDragonFight fight) {
        this.fight = fight;
    }

    public List<? extends EndCrystal> getCrystals() {
        return crystals;
    }

    public void setCrystals(List<? extends EndCrystal> crystals) {
        this.crystals = crystals == null ? new ArrayList<>() : crystals;
    }

    // ---- Public API ----

    /**
     * Initialize the respawn state with world context, position, fight instance, and crystals.
     */
    public void setup(ServerLevel world, BlockPos pos, EndDragonFight fight,
                      List<? extends EndCrystal> crystals) {
        this.world = world;
        this.pos = pos;
        this.fight = fight;
        this.crystals = crystals != null ? crystals : new ArrayList<>();
        this.current = "";
        this.currentAnimate = null;
        this.pillarsAnimates.clear();
        this.tickingAnimates.clear();
    }

    /**
     * Advance to the next respawn phase by id.
     * Returns true if the phase transition was recognized and handled.
     */
    public boolean next(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        // Cancel previous animate
        if (currentAnimate != null && !currentAnimate.getDone()) {
            currentAnimate.cancel();
        }

        current = id;

        switch (id) {
            case "pillars" -> handlePillarsPhase();
            case "crystal" -> handleCrystalPhase();
            case "end" -> handleEnd();
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Handle the once-pillars event (camera shake + pillar emitters).
     */
    public void handleOncePillars(BlockPos pillarPos) {
        if (world == null || pillarPos == null) return;

        Vec3 center = Vec3.atCenterOf(pillarPos);
        ServerCameraUtil.sendShake(world, center, 128.0, 3.0, 20);

        // Spawn pillar collection emitter
        CollectPillarsEmitters emitter = new CollectPillarsEmitters(center, world);
        emitter.setMaxTick(60);
        emitter.setRadiusRange(2.0, 8.0);
        emitter.setCountRange(20, 40);
        spawnManagedEmitter(emitter);

        // Track pillar animate
        Animate pillarAnimate = new Animate();
        pillarAnimate.addNode(createTickingNode(60));
        pillarAnimate.start();
        pillarsAnimates.add(pillarAnimate);
    }

    /**
     * Handle the end phase ??summon explosion and dragon invulnerability toggle.
     */
    public void handleEnd() {
        if (world == null || pos == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        // Camera shake
        ServerCameraUtil.sendShake(world, center, 128.0, 3.0, 20);

        // End beam explosion
        EndBeamExplosionEmitter explosion = new EndBeamExplosionEmitter(center, world);
        explosion.setSpeedRange(2.0, 18.0);
        explosion.setCountRange(80, 120);
        explosion.setDrag(0.97);
        spawnManagedEmitter(explosion);

        // Find and toggle nearest dragon
        Vec3 summonPos = Vec3.atCenterOf(pos);
        List<EnderDragon> dragons = world.getEntitiesOfClass(
                EnderDragon.class,
                AABB.ofSize(summonPos, 64.0, 64.0, 64.0),
                EnderDragon::isAlive
        );
        dragons.stream()
                .min(Comparator.comparingDouble(d -> d.position().distanceTo(summonPos)))
                .ifPresent(dragon -> {
                    dragon.setInvulnerable(true);
                    ReiParticlesAPI.scheduler.runTask(20, () -> dragon.setInvulnerable(false));
                });
    }

    /**
     * Get or create the current animate sequence for the active respawn phase.
     */
    public Animate getAnimateFrom() {
        if (currentAnimate != null && !currentAnimate.getDone()) {
            return currentAnimate;
        }

        Animate animate = new Animate();
        animate.addCancelPredicate(a -> "end".equals(current) || current.isEmpty());

        // Add camera shake node
        animate.addNode(new AnimateNode().addAction(new AnimateAction() {
            @Override
            public boolean checkDone() {
                return getTickCount() >= 40;
            }

            @Override
            public void tick() {
                if (world != null && pos != null) {
                    Vec3 center = Vec3.atCenterOf(pos);
                    ServerCameraUtil.sendShake(world, center, 128.0, 2.0, 30);
                }
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onDone() {
            }
        }));

        currentAnimate = animate;
        return animate;
    }

    /**
     * Tick all active animates.
     */
    public void tick() {
        // Tick pillar animates
        pillarsAnimates.removeIf(a -> {
            a.tick();
            return a.getDone();
        });

        // Tick main ticking animates
        tickingAnimates.removeIf(a -> {
            a.tick();
            return a.getDone();
        });

        // Tick current animate
        if (currentAnimate != null) {
            currentAnimate.tick();
            if (currentAnimate.getDone()) {
                currentAnimate = null;
            }
        }
    }

    /**
     * Cancel all active animates and reset state.
     */
    public void cancel() {
        for (Animate a : pillarsAnimates) {
            if (!a.getDone()) a.cancel();
        }
        pillarsAnimates.clear();

        for (Animate a : tickingAnimates) {
            if (!a.getDone()) a.cancel();
        }
        tickingAnimates.clear();

        if (currentAnimate != null && !currentAnimate.getDone()) {
            currentAnimate.cancel();
        }
        currentAnimate = null;
        current = "";
    }

    // ---- Internal phases ----

    private void handlePillarsPhase() {
        if (world == null || pos == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        // Spawn center style
        EnderRespawnCenterStyle centerStyle = new EnderRespawnCenterStyle();
        centerStyle.display(center, world);

        // Spawn wave styles
        for (int i = 0; i < 3; i++) {
            double yOff = i * 2.0;
            EnderRespawnWaveCloudStyle cloudStyle = new EnderRespawnWaveCloudStyle();
            cloudStyle.setYOffset(yOff);
            cloudStyle.setRadius(15.0 + i * 5.0);
            cloudStyle.display(center, world);

            EnderRespawnWaveEnchantStyle enchantStyle = new EnderRespawnWaveEnchantStyle();
            enchantStyle.setYOffset(yOff + 1.0);
            enchantStyle.setRadius(12.0 + i * 4.0);
            enchantStyle.display(center, world);
        }

        // Spawn dust
        EndDustStyle dustStyle = new EndDustStyle();
        dustStyle.setMaxRadius(8.0);
        dustStyle.setCount(60);
        dustStyle.display(center, world);

        // Start animate
        currentAnimate = getAnimateFrom();
        currentAnimate.start();
    }

    private void handleCrystalPhase() {
        if (world == null || pos == null) return;

        Vec3 target = Vec3.atCenterOf(pos);

        // Spawn crystal emitters for each crystal
        for (EndCrystal crystal : crystals) {
            if (crystal == null || !crystal.isAlive()) continue;

            Vec3 crystalPos = crystal.position();
            EndCrystalEmitters emitter = new EndCrystalEmitters(crystalPos, world);
            emitter.setTarget(target);
            emitter.setSummonPos(crystalPos);
            emitter.setMovementSpeed(3.0);
            emitter.setMaxRadius(6.0);
            emitter.setMaxTick(400);
            spawnManagedEmitter(emitter);

            // Crystal style at crystal position
            EndCrystalStyle style = new EndCrystalStyle();
            style.display(crystalPos, world);
        }

        // Collect ender power toward center
        CollectEnderPowerEmitter powerEmitter = new CollectEnderPowerEmitter(
                target.add(0, 10, 0), world);
        powerEmitter.setTargetPos(target);
        powerEmitter.setSpeed(0.8);
        powerEmitter.setSpawnRadius(12.0);
        powerEmitter.setMaxTick(200);
        spawnManagedEmitter(powerEmitter);

        // Start animate
        currentAnimate = getAnimateFrom();
        currentAnimate.start();
    }

    static <T extends ParticleEmitters> T spawnManagedEmitter(T emitter) {
        if (emitter == null) {
            return null;
        }
        ParticleEmittersManager.spawnEmitters(emitter);
        emitter.tick();
        return emitter;
    }

    private AnimateNode createTickingNode(int duration) {
        return new AnimateNode().addAction(new AnimateAction() {
            @Override
            public boolean checkDone() {
                return getTickCount() >= duration;
            }

            @Override
            public void tick() {
                // No-op ticking; just wait for duration
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onDone() {
            }
        });
    }
}




