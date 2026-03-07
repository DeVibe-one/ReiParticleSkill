// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.registry;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import com.reiasu.reiparticleskill.enchantments.SkillEnchantments;
import com.reiasu.reiparticleskill.enchantments.SwordLightEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeSkillEnchantments {
    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ReiParticleSkillConstants.MOD_ID);

    static {
        ENCHANTMENTS.register(SkillEnchantments.SWORD_LIGHT.path(), SwordLightEnchantment::new);
    }

    private ForgeSkillEnchantments() {
    }

    public static void register(IEventBus bus) {
        ENCHANTMENTS.register(bus);
    }
}
