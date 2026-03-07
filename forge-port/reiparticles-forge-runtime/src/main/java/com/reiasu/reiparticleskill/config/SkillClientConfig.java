// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class SkillClientConfig {

    public static final ForgeConfigSpec SPEC;
    public static final SkillClientConfig INSTANCE;

    static {
        Pair<SkillClientConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(SkillClientConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final ForgeConfigSpec.BooleanValue suppressCrystalBeam;

    private SkillClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("rendering");

        suppressCrystalBeam = builder
                .comment("Suppress the vanilla End Crystal beam so custom particle effects are visible.",
                         "Set to false to restore the vanilla beam (for modpack compatibility).")
                .define("suppressCrystalBeam", true);

        builder.pop();
    }

    public boolean isSuppressCrystalBeam() {
        try {
            return suppressCrystalBeam.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }
}
