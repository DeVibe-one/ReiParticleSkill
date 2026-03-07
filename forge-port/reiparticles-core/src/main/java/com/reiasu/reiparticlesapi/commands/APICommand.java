// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import net.minecraft.commands.CommandSourceStack;

/**
 * Registers the {@code /cleanapi} command tree for ReiParticlesAPI.
 * <p>
 * Currently provides:
 * <ul>
 *   <li>{@code /cleanapi display} - clears tracked display entities in this runtime</li>
 * </ul>
 */
public final class APICommand {
    public static final APICommand INSTANCE = new APICommand();

    private APICommand() {
    }

    /**
     * Register the {@code /cleanapi} command with the given dispatcher.
     *
     * @param dispatcher the server command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("cleanapi")
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("display")
                                .executes(ctx -> {
                                    DisplayEntityManager.INSTANCE.clear();
                                    return 1;
                                }))
        );
    }
}
