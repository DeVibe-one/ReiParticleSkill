// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display;

import com.reiasu.reiparticlesapi.display.DisplayEntityManager;

public final class SkillDisplayTypes {
    private SkillDisplayTypes() {
    }

    public static void register() {
        DisplayEntityManager.INSTANCE.registerType(BarrageItemDisplay.TYPE_ID, BarrageItemDisplay::decode);
        DisplayEntityManager.INSTANCE.registerType(SwordLightDisplay.TYPE_ID, SwordLightDisplay::decode);
        DisplayEntityManager.INSTANCE.registerType(LightFlashDisplay.TYPE_ID, LightFlashDisplay::decode);
    }
}

