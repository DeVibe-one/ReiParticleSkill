// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.runtime;

import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnStateBridge;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnWatcher;
import org.slf4j.Logger;

public final class SkillRuntimeStateReset {
    private SkillRuntimeStateReset() {
    }

    public static void reset(EndRespawnStateBridge endRespawnBridge, Logger logger) {
        SkillBarrageManager.INSTANCE.clear();
        ServerDisplayGroupManager.INSTANCE.clear();
        EndRespawnWatcher.reset();
        if (endRespawnBridge != null && logger != null) {
            endRespawnBridge.cancel(logger);
        }
    }
}
