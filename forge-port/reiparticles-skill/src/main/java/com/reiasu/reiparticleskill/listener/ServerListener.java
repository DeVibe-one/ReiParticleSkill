// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.listener;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class ServerListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerListener() {
    }

    public static void onServerPostTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        safeTick("SkillBarrageManager", () -> SkillBarrageManager.INSTANCE.tickAll());
        safeTick("ServerDisplayGroupManager", () -> ServerDisplayGroupManager.INSTANCE.doTick());
    }

    private static void safeTick(String name, Runnable tick) {
        try {
            tick.run();
        } catch (RuntimeException e) {
            LOGGER.warn("Skill server tick handler '{}' failed", name, e);
        }
    }
}
