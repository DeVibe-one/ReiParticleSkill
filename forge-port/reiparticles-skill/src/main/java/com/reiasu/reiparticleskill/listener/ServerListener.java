// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.listener;

import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import net.minecraft.server.MinecraftServer;

public final class ServerListener {
    private ServerListener() {
    }

    public static void onServerPostTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        SkillBarrageManager.INSTANCE.tickAll();
        ServerDisplayGroupManager.INSTANCE.doTick();
    }
}
