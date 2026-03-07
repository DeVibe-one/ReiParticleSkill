// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command.port;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.particle.emitters.DebugRailgunEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class RailgunCommandPort {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RailgunCommandPort() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("railgun")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .executes(ctx -> run(ctx.getSource(), Vec3Argument.getVec3(ctx, "pos"))))
        );
    }

    private static int run(CommandSourceStack source, net.minecraft.world.phys.Vec3 target) {
        var level = source.getLevel();
        net.minecraft.world.phys.Vec3 from = source.getPosition();
        boolean spawned;
        try {
            ParticleEmittersManager.spawnEmitters(new DebugRailgunEmitters(level, from, target));
            spawned = true;
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to spawn railgun emitters, using fallback particles", e);
            spawned = false;
        }

        int emitted = 0;
        if (!spawned) {
            net.minecraft.world.phys.Vec3 diff = target.subtract(from);
            int steps = Math.max(12, (int) (diff.length() * 2.0));
            DustParticleOptions color = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.62f), 1.2f);
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                double x = from.x + diff.x * t;
                double y = from.y + diff.y * t;
                double z = from.z + diff.z * t;
                level.sendParticles(color, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                emitted++;
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, target.x, target.y, target.z, 6, 0.2, 0.2, 0.2, 0.01);
            emitted += 6;
        }

        final int rs = spawned ? 1 : 0;
        final int fb = emitted;
        source.sendSuccess(() -> Component.literal("railgun runtime_spawned=" + rs
                + " preview_fallback=" + fb), false);
        return 1;
    }
}
