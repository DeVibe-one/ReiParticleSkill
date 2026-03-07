// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.client;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.event.events.key.KeyActionType;
import com.reiasu.reiparticlesapi.network.ForgeReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.packet.PacketKeyActionC2S;
import com.reiasu.reiparticleskill.ReiParticleSkillForge;
import com.reiasu.reiparticleskill.keys.SkillKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = ReiParticleSkillForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SkillKeyInputHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SkillKeyInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (SkillKeyMappings.FORMATION_1.consumeClick()) {
            sendKeyAction(SkillKeys.FORMATION_1);
        }
        while (SkillKeyMappings.FORMATION_2.consumeClick()) {
            sendKeyAction(SkillKeys.FORMATION_2);
        }
    }

    private static void sendKeyAction(ResourceLocation keyId) {
        try {
            ForgeReiParticlesNetwork.CHANNEL.sendToServer(new PacketKeyActionC2S(keyId, KeyActionType.SINGLE_CLICK, 0, false));
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to send key action packet: {}", keyId, e);
        }
    }
}
