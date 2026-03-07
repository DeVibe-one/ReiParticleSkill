// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public final class APIConfigSpec {
    public static final ForgeConfigSpec SPEC;
    private static final APIConfigSpec INSTANCE;

    static {
        Pair<APIConfigSpec, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(APIConfigSpec::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
        INSTANCE.applyValues();
    }

    private final ForgeConfigSpec.BooleanValue enabledParticleCountInject;
    private final ForgeConfigSpec.BooleanValue enabledParticleAsync;
    private final ForgeConfigSpec.IntValue particleCountLimit;
    private final ForgeConfigSpec.IntValue calculateThreadCount;
    private final ForgeConfigSpec.IntValue packetsPerTickLimit;
    private final ForgeConfigSpec.IntValue maxEmitterVisibleRange;

    private APIConfigSpec(ForgeConfigSpec.Builder builder) {
        builder.push("particles");

        enabledParticleCountInject = builder.comment("Enable particle count injection (overrides vanilla limits)")
                .define("enabledParticleCountInject", true);
        enabledParticleAsync = builder.comment("Enable async particle calculations")
                .define("enabledParticleAsync", true);
        particleCountLimit = builder.comment("Maximum number of particles allowed in the world")
                .defineInRange("particleCountLimit", 131072, 1, 1_000_000);
        calculateThreadCount = builder.comment("Number of threads for particle calculations")
                .defineInRange("calculateThreadCount", 4, 1, 64);
        packetsPerTickLimit = builder.comment("Maximum emitter sync packets sent per server tick (global)")
                .defineInRange("packetsPerTickLimit", 512, 16, 4096);
        maxEmitterVisibleRange = builder.comment("Maximum visible range (blocks) for emitter sync packets")
                .defineInRange("maxEmitterVisibleRange", 256, 32, 1024);

        builder.pop();
    }

    public static boolean owns(ModConfig config) {
        return config != null && config.getSpec() == SPEC;
    }

    public static void apply() {
        INSTANCE.applyValues();
    }

    private void applyValues() {
        APIConfig.INSTANCE.setEnabledParticleCountInject(enabledParticleCountInject.get());
        APIConfig.INSTANCE.setEnabledParticleAsync(enabledParticleAsync.get());
        APIConfig.INSTANCE.setParticleCountLimit(particleCountLimit.get());
        APIConfig.INSTANCE.setCalculateThreadCount(calculateThreadCount.get());
        APIConfig.INSTANCE.setPacketsPerTickLimit(packetsPerTickLimit.get());
        APIConfig.INSTANCE.setMaxEmitterVisibleRange(maxEmitterVisibleRange.get());
    }
}
