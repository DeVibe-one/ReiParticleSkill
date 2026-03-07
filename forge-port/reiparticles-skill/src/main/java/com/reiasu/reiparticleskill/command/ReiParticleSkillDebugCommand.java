// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command;

import com.reiasu.reiparticleskill.command.layout.DisplayIndexKind;
import com.reiasu.reiparticleskill.command.layout.DisplayIndexPlan;
import com.reiasu.reiparticleskill.command.layout.DisplayIndexRouting;
import com.reiasu.reiparticleskill.command.layout.DisplaySpawnProfile;
import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.group.layout.FormationLayerSpec;
import com.reiasu.reiparticleskill.display.group.layout.SimpleSwordFormationLayout;
import com.reiasu.reiparticleskill.display.group.layout.SwordFormationLayerPresets;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnPhase;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnSnapshot;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnStateBridge;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnWatcher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class ReiParticleSkillDebugCommand {
    private ReiParticleSkillDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, EndRespawnStateBridge bridge, Logger logger) {
        dispatcher.register(
                Commands.literal("reiparticleskill")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("display")
                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                        .executes(ctx -> runDisplayIndex(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "index")
                                        ))))
                        .then(Commands.literal("formation")
                                .executes(ctx -> runFormationPreview(ctx.getSource())))
                        .then(Commands.literal("barrage")
                                .then(Commands.literal("status")
                                        .executes(ctx -> runBarrageStatus(ctx.getSource())))
                                .then(Commands.literal("clear")
                                        .executes(ctx -> runBarrageClear(ctx.getSource()))))
                        .then(Commands.literal("respawn")
                                .then(Commands.literal("phase")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> runRespawnPhase(
                                                        ctx.getSource(),
                                                        bridge,
                                                        logger,
                                                        StringArgumentType.getString(ctx, "id")
                                                ))))
                                .then(Commands.literal("cancel")
                                        .executes(ctx -> runRespawnCancel(ctx.getSource(), bridge, logger)))
                                .then(Commands.literal("status")
                                        .executes(ctx -> runRespawnStatus(ctx.getSource(), bridge))))
        );
    }

    private static int runDisplayIndex(CommandSourceStack source, int index) {
        ServerLevel level = source.getLevel();
        var pos = source.getPosition();

        Optional<DisplayIndexPlan> planOpt = DisplayIndexRouting.plan(index);
        if (planOpt.isEmpty()) {
            source.sendFailure(Component.literal("index=" + index + " is not supported in migrated debug routing"));
            return 0;
        }

        DisplayIndexPlan plan = planOpt.get();

        if (plan.kind() == DisplayIndexKind.GROUP) {
            List<FormationLayerSpec> specs = SwordFormationLayerPresets.defaultLayerSpecs();
            int totalCount = specs.stream().mapToInt(FormationLayerSpec::count).sum();
            int emitted = DisplayDebugRuntime.previewByPlan(level, pos.x, pos.y, pos.z, plan);
            source.sendSuccess(() -> Component.literal("index=4 group layout ready, layers=" + specs.size()
                    + ", total points=" + totalCount
                    + ", preview particles=" + emitted), false);
            return 1;
        }

        Optional<DisplaySpawnProfile> profile = DisplayIndexRouting.spawnProfile(index);
        if (profile.isPresent()) {
            DisplaySpawnProfile p = profile.get();
            int emitted = DisplayDebugRuntime.previewByPlan(level, pos.x, pos.y, pos.z, plan);
            source.sendSuccess(() -> Component.literal("index=" + index
                    + " kind=" + plan.kind()
                    + " yaw=" + p.orientation().yawDegrees()
                    + " pitch=" + p.orientation().pitchDegrees()
                    + " scale=" + p.targetScale()
                    + " speed=" + p.scaledSpeed()
                    + " preview particles=" + emitted), false);
            return 1;
        }

        int emitted = DisplayDebugRuntime.previewByPlan(level, pos.x, pos.y, pos.z, plan);
        source.sendSuccess(() -> Component.literal("index=" + index + " kind=" + plan.kind() + " preview particles=" + emitted), false);
        return 1;
    }

    private static int runFormationPreview(CommandSourceStack source) {
        List<SimpleSwordFormationLayout> layouts =
                SwordFormationLayerPresets.createDefaultLayouts(new RelativeLocation(0.0, 0.0, 1.0));

        int totalPoints = 0;
        for (SimpleSwordFormationLayout layout : layouts) {
            totalPoints += layout.currentOffsets().size();
        }
        final int points = totalPoints;

        var pos = source.getPosition();
        int emitted = DisplayDebugRuntime.previewFormation(source.getLevel(), pos.x, pos.y, pos.z);

        source.sendSuccess(() -> Component.literal("formation preview layouts=" + layouts.size()
                + ", points=" + points
                + ", preview particles=" + emitted), false);
        return 1;
    }

    private static int runBarrageStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("barrage active=" + SkillBarrageManager.INSTANCE.activeCount()), false);
        return 1;
    }

    private static int runBarrageClear(CommandSourceStack source) {
        SkillBarrageManager.INSTANCE.clear();
        source.sendSuccess(() -> Component.literal("barrage cleared"), false);
        return 1;
    }

    private static int runRespawnPhase(CommandSourceStack source, EndRespawnStateBridge bridge, Logger logger, String id) {
        var phaseOpt = EndRespawnPhase.fromId(id);
        if (phaseOpt.isEmpty()) {
            source.sendFailure(Component.literal("unknown phase id=" + id));
            return 0;
        }

        var pos = source.getPosition();
        bridge.setup(source.getLevel(), pos);
        bridge.next(source.getLevel(), pos, phaseOpt.get(), logger);
        source.sendSuccess(() -> Component.literal("respawn phase -> " + phaseOpt.get().id()), false);
        return 1;
    }

    private static int runRespawnCancel(CommandSourceStack source, EndRespawnStateBridge bridge, Logger logger) {
        bridge.cancel(logger);
        source.sendSuccess(() -> Component.literal("respawn bridge canceled"), false);
        return 1;
    }

    private static int runRespawnStatus(CommandSourceStack source, EndRespawnStateBridge bridge) {
        EndRespawnSnapshot snapshot = bridge.current();
        String bridgeState;
        if (snapshot == null) {
            bridgeState = "inactive";
        } else {
            bridgeState = "level=" + snapshot.levelId()
                    + " phase=" + snapshot.phase().id()
                    + " phaseTick=" + snapshot.phaseTick()
                    + " center=" + snapshot.center();
        }

        String probeState = EndRespawnWatcher.probeServer(source.getServer())
                .map(probe -> "level=" + probe.levelId()
                        + " direct_phase=" + probe.directPhase()
                        + " crystals(fight/portal/resolved)="
                        + probe.fightCrystals() + "/" + probe.portalAreaCrystals() + "/" + probe.resolvedCrystals()
                        + " center=" + probe.center())
                .orElse("none");

        String directorState = bridge.directorDebugState();
        source.sendSuccess(() -> Component.literal("respawn status: bridge={" + bridgeState + "} probe={" + probeState
                + "} director={" + directorState + "}"), false);
        return 1;
    }
}

