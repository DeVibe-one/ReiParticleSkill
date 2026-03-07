// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.client;

import com.reiasu.reiparticleskill.ReiParticleSkillForge;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ReiParticleSkillForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SkillKeyMappings {
    public static final KeyMapping FORMATION_1 = new KeyMapping(
            "key.reiparticleskill.formation1",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.reiparticleskill.category"
    );

    public static final KeyMapping FORMATION_2 = new KeyMapping(
            "key.reiparticleskill.formation2",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.reiparticleskill.category"
    );

    private SkillKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(FORMATION_1);
        event.register(FORMATION_2);
    }
}
