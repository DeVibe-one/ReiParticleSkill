// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command.port;

import com.reiasu.reiparticleskill.compat.interop.ReiparticlesInterop;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class APITestCommandPort {
    private APITestCommandPort() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("apitest")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("index", IntegerArgumentType.integer())
                                .executes(ctx -> run(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"))))
        );
    }

    private static int run(CommandSourceStack source, int index) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("apitest must be run by a player"));
            return 0;
        }
        if (!ReiparticlesInterop.isApiPresent()) {
            source.sendFailure(Component.literal("reiparticlesapi not present"));
            return 0;
        }

        ReiparticlesInterop.ApiTestResult result = ReiparticlesInterop.triggerApiTest(player, index);
        final String detail = result.detail();
        final var state = result.state();
        if (state == ReiparticlesInterop.ApiTestState.FAILED || state == ReiparticlesInterop.ApiTestState.UNAVAILABLE) {
            source.sendFailure(Component.literal("apitest index=" + index + " " + detail));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("apitest index=" + index + " " + detail), false);
        return 1;
    }
}
