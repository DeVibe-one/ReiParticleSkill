// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.barrages;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.barrages.Barrage;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public enum SkillBarrageManager {
    INSTANCE;

    private static final Logger LOGGER = LogUtils.getLogger();

    private final CopyOnWriteArrayList<Barrage> activeBarrages = new CopyOnWriteArrayList<>();

    public void spawn(Barrage barrage) {
        if (barrage == null || !barrage.getValid()) {
            return;
        }
        activeBarrages.add(barrage);
    }

    public void tickAll() {
        if (activeBarrages.isEmpty()) {
            return;
        }
        activeBarrages.removeIf(barrage -> {
            if (barrage == null || !barrage.getValid()) {
                return true;
            }
            barrage.tick();
            return !barrage.getValid();
        });
    }

    public void clear() {
        for (Barrage barrage : activeBarrages) {
            if (barrage == null) {
                continue;
            }
            try {
                barrage.getBindControl().cancel();
            } catch (RuntimeException e) {
                LOGGER.debug("Failed to cancel barrage control during cleanup", e);
            }
        }
        activeBarrages.clear();
    }

    public int activeCount() {
        return activeBarrages.size();
    }

    public List<Barrage> snapshot() {
        return List.copyOf(activeBarrages);
    }
}
