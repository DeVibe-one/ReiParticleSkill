// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.client;

import com.reiasu.reiparticleskill.ReiParticleSkillForge;
import com.reiasu.reiparticleskill.entities.SkillEntityTypes;
import com.reiasu.reiparticleskill.entities.renderer.BarrageItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ReiParticleSkillForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SkillClientRegistries {
    private SkillClientRegistries() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SkillEntityTypes.BARRAGE_ITEM.get(), BarrageItemRenderer::new);
        event.registerEntityRenderer(EntityType.END_CRYSTAL, NoBeamEndCrystalRenderer::new);
    }
}
