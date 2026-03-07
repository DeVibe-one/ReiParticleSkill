// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.keys;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import net.minecraft.resources.ResourceLocation;

public final class SkillKeys {
    public static final ResourceLocation FORMATION_1 = id("formation1");
    public static final ResourceLocation FORMATION_2 = id("formation2");

    private SkillKeys() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ReiParticleSkillConstants.MOD_ID, path);
    }
}
