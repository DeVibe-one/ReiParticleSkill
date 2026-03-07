// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.enchantments;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class SkillEnchantments {
    public static final EnchantmentRef SWORD_LIGHT = enchantment("sword_light");

    private SkillEnchantments() {
    }

    private static EnchantmentRef enchantment(String path) {
        return new EnchantmentRef(new ResourceLocation(ReiParticleSkillConstants.MOD_ID, path));
    }

    public static int getSwordLightLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !SWORD_LIGHT.isPresent()) {
            return 0;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(SWORD_LIGHT.get(), stack);
    }

    public static final class EnchantmentRef {
        private final ResourceLocation id;

        private EnchantmentRef(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public String path() {
            return id.getPath();
        }

        public boolean isPresent() {
            return BuiltInRegistries.ENCHANTMENT.containsKey(id);
        }

        public Enchantment get() {
            return BuiltInRegistries.ENCHANTMENT.get(id);
        }
    }
}
