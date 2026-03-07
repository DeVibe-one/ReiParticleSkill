// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command;

import com.reiasu.reiparticleskill.service.SkillKeyActionService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SkillActionCommand {
    private SkillActionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pskill")
                        .requires(source -> source.getEntity() instanceof Player)
                        .then(Commands.literal("formation1").executes(ctx -> run(ctx.getSource(), Action.FORMATION_1)))
                        .then(Commands.literal("formation2").executes(ctx -> run(ctx.getSource(), Action.FORMATION_2)))
                        .then(Commands.literal("shoot").executes(ctx -> run(ctx.getSource(), Action.SHOOT)))
        );
    }

    private static int run(CommandSourceStack source, Action action) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("player only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        boolean success;
        if (action == Action.FORMATION_1) {
            success = SkillKeyActionService.handleSkillKey(player, com.reiasu.reiparticleskill.keys.SkillKeys.FORMATION_1, stack);
        } else if (action == Action.FORMATION_2) {
            success = SkillKeyActionService.handleSkillKey(player, com.reiasu.reiparticleskill.keys.SkillKeys.FORMATION_2, stack);
        } else {
            success = SkillKeyActionService.triggerShoot(player, stack);
        }

        if (!success) {
            source.sendFailure(Component.literal("need sword_light enchanted sword and no cooldown"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("skill action: " + action.id), false);
        return 1;
    }

    private enum Action {
        FORMATION_1("formation1"),
        FORMATION_2("formation2"),
        SHOOT("shoot");

        private final String id;

        Action(String id) {
            this.id = id;
        }
    }
}
