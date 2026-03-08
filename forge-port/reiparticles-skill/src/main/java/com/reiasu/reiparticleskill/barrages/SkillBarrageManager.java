// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.barrages;

import com.reiasu.reiparticlesapi.barrages.Barrage;
import com.reiasu.reiparticlesapi.barrages.BarrageManager;

import java.util.List;

public enum SkillBarrageManager {
    INSTANCE;

    public void spawn(Barrage barrage) {
        BarrageManager.INSTANCE.spawn(barrage);
    }

    public void tickAll() {
        BarrageManager.INSTANCE.doTick();
    }

    public void clear() {
        BarrageManager.INSTANCE.clear();
    }

    public int activeCount() {
        return BarrageManager.INSTANCE.activeCount();
    }

    public List<Barrage> snapshot() {
        return BarrageManager.INSTANCE.snapshot();
    }
}
