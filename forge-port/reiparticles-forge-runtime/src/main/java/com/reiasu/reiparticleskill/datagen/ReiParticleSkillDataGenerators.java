// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.datagen;

import com.reiasu.reiparticleskill.ReiParticleSkillForge;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ReiParticleSkillForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ReiParticleSkillDataGenerators {
    private ReiParticleSkillDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        if (event.includeClient()) {
            event.getGenerator().addProvider(true, new SkillLanguageProvider(output, "en_us"));
            event.getGenerator().addProvider(true, new SkillLanguageProvider(output, "zh_cn"));
        }
    }
}
