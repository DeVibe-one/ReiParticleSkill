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
        KeySkillAction action = resolveKeyAction(keyId);
        if (action == null) {
            return false;
        }
        player.getCooldowns().addCooldown(stack.getItem(), USE_COOLDOWN_TICKS);
        action.run(player, stack);
        return true;
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

    static KeySkillAction resolveKeyAction(ResourceLocation keyId) {
        if (SkillKeys.FORMATION_1.equals(keyId)) {
            return KeySkillAction.FORMATION_1;
        }
        if (SkillKeys.FORMATION_2.equals(keyId)) {
            return KeySkillAction.FORMATION_2;
        }
        return null;
    }

    enum KeySkillAction {
        FORMATION_1 {
            @Override
            void run(Player player, ItemStack stack) {
                SwordLightEnchantUtil.placeSwordFormation(player, stack);
            }
        },
        FORMATION_2 {
            @Override
            void run(Player player, ItemStack stack) {
                SwordLightEnchantUtil.placeSwordFormation2(player, stack);
            }
        };

        abstract void run(Player player, ItemStack stack);
    }
}
