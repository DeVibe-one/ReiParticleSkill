// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.listener;

import com.reiasu.reiparticlesapi.annotations.events.EventHandler;
import com.reiasu.reiparticlesapi.annotations.events.EventListener;
import com.reiasu.reiparticlesapi.event.events.key.KeyActionEvent;
import com.reiasu.reiparticlesapi.event.events.key.KeyActionType;
import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import com.reiasu.reiparticleskill.service.SkillKeyActionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@EventListener(modId = ReiParticleSkillConstants.MOD_ID)
@Mod.EventBusSubscriber(modid = ReiParticleSkillConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KeyListener {
    @EventHandler
    public void onKeyEvent(KeyActionEvent event) {
        if (event == null || event.getAction() != KeyActionType.SINGLE_CLICK || !event.getServerSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        SkillKeyActionService.handleSkillKey(player, event.getKeyId(), event.getPlayer().getMainHandItem());
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event == null || event.getEntity().level().isClientSide() || event.getEntity().isSpectator()) {
            return;
        }
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        SkillKeyActionService.triggerShoot(player, stack);
    }
}
