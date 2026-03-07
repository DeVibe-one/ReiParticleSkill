// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command.port;

import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticleskill.command.DisplayDebugRuntime;
import com.reiasu.reiparticleskill.command.layout.DisplayIndexPlan;
import com.reiasu.reiparticleskill.command.layout.DisplayIndexRouting;
import com.reiasu.reiparticleskill.command.layout.DisplaySpawnProfile;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import com.reiasu.reiparticleskill.display.group.impl.SimpleSwordFormationGroupGroup;
import com.reiasu.reiparticleskill.entities.BarrageItemEntity;
import com.reiasu.reiparticleskill.particles.display.emitter.ParticleGroupEmitter;
import com.reiasu.reiparticleskill.particles.preview.display.ChangingComposition;
import com.reiasu.reiparticleskill.particles.preview.display.LargeMagicCircleStyle;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class DisplayCommandPort {
    private DisplayCommandPort() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("display")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("index", IntegerArgumentType.integer())
                                .executes(ctx -> run(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "index"),
                                        null
                                ))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> run(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "index"),
                                                Vec3Argument.getVec3(ctx, "pos")
                                        ))))
        );
    }

    private static int run(CommandSourceStack source, int index, net.minecraft.world.phys.Vec3 posArg) {
        Optional<DisplayIndexPlan> planOpt = DisplayIndexRouting.plan(index);
        if (planOpt.isEmpty()) {
            source.sendFailure(Component.literal("display index unsupported: " + index));
            return 0;
        }

        var level = source.getLevel();
        net.minecraft.world.phys.Vec3 pos = posArg != null ? posArg : source.getPosition();
        DisplayIndexPlan plan = planOpt.get();

        final int runtimeSpawned = spawnNative(plan, level, pos);
        final int previewFallback = runtimeSpawned <= 0
                ? DisplayDebugRuntime.previewByPlan(level, pos.x, pos.y, pos.z, plan) : 0;

        source.sendSuccess(() -> Component.literal("display index=" + index
                + " kind=" + plan.kind()
                + " runtime_spawned=" + runtimeSpawned
                + " preview_fallback=" + previewFallback), false);
        return 1;
    }

    private static int spawnNative(DisplayIndexPlan plan, ServerLevel level, Vec3 pos) {
        return switch (plan.kind()) {
            case EMITTER -> spawnEmitter(plan.index(), level, pos);
            case ENTITY -> spawnEntity(plan.index(), level, pos, plan.profile().orElse(DisplaySpawnProfile.defaultFacingUp()));
            case DISPLAY -> spawnDisplay(plan.index(), level, pos, plan.profile().orElse(DisplaySpawnProfile.defaultFacingUp()));
            case GROUP -> spawnGroup(level, pos);
            case STYLE -> spawnStyle(plan.index(), level, pos);
            case COMPOSITION -> spawnComposition(plan.index(), level, pos);
        };
    }

    private static int spawnEmitter(int index, ServerLevel level, Vec3 pos) {
        if (index != 0) {
            return 0;
        }
        ParticleEmittersManager.spawnEmitters(new ParticleGroupEmitter(pos, level));
        return 1;
    }

    private static int spawnStyle(int index, ServerLevel level, Vec3 pos) {
        if (index != 1) {
            return 0;
        }
        ParticleStyleManager.spawnStyle(level, pos, new LargeMagicCircleStyle());
        return 1;
    }

    private static int spawnEntity(int index, ServerLevel level, Vec3 pos, DisplaySpawnProfile profile) {
        if (index != 2) {
            return 0;
        }

        BarrageItemEntity entity = new BarrageItemEntity(level, pos, new ItemStack(Items.IRON_SWORD));
        entity.setYRot(profile.orientation().yawDegrees());
        entity.setXRot(profile.orientation().pitchDegrees());
        entity.setScale(Math.max(0.1F, profile.targetScale()));
        level.addFreshEntity(entity);
        return 1;
    }

    private static int spawnDisplay(int index, ServerLevel level, Vec3 pos, DisplaySpawnProfile profile) {
        if (index != 3 && index != 6) {
            return 0;
        }

        BarrageItemDisplay display = new BarrageItemDisplay(pos);
        display.setItem(new ItemStack(Items.IRON_SWORD));
        display.setTargetYaw(profile.orientation().yawDegrees());
        display.setTargetPitch(profile.orientation().pitchDegrees());
        display.setTargetScale(profile.targetScale());
        display.setScaledSpeed(profile.scaledSpeed());
        DisplayEntityManager.INSTANCE.spawn(display, level);
        return 1;
    }

    private static int spawnGroup(ServerLevel level, Vec3 pos) {
        SimpleSwordFormationGroupGroup group = new SimpleSwordFormationGroupGroup(pos, level, new Vec3(0.0, 0.0, 1.0));
        ServerDisplayGroupManager.INSTANCE.spawn(group);
        return Math.max(1, group.getDisplayers().size());
    }

    private static int spawnComposition(int index, ServerLevel level, Vec3 pos) {
        if (index != 5) {
            return 0;
        }
        ChangingComposition composition = new ChangingComposition(pos, level);
        composition.display();
        ParticleCompositionManager.INSTANCE.spawn(composition);
        return 1;
    }
}
