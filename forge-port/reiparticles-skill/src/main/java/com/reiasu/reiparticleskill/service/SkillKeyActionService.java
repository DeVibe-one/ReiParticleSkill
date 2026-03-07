// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.service;

import com.reiasu.reiparticleskill.enchantments.SkillEnchantments;
import com.reiasu.reiparticleskill.keys.SkillKeys;
import com.reiasu.reiparticleskill.util.SwordLightEnchantUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SkillKeyActionService {
    private static final int USE_COOLDOWN_TICKS = 16;

    private SkillKeyActionService() {
    }

    public static boolean handleSkillKey(Player player, ResourceLocation keyId, ItemStack stack) {
        if (player == null || keyId == null || !canUseSkill(player, stack)) {
            return false;
        }
        player.getCooldowns().addCooldown(stack.getItem(), USE_COOLDOWN_TICKS);
        if (SkillKeys.FORMATION_1.equals(keyId)) {
            SwordLightEnchantUtil.placeSwordFormation(player, stack);
            return true;
        }
        if (SkillKeys.FORMATION_2.equals(keyId)) {
            SwordLightEnchantUtil.placeSwordFormation2(player, stack);
            return true;
        }
        return false;
    }

    public static boolean triggerShoot(Player player, ItemStack stack) {
        if (!canUseSkill(player, stack)) {
            return false;
        }
        player.getCooldowns().addCooldown(stack.getItem(), USE_COOLDOWN_TICKS);
        SwordLightEnchantUtil.shoot(player, stack);
        return true;
    }

    private static boolean canUseSkill(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        return SkillEnchantments.getSwordLightLevel(stack) > 0;
    }
}
