// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.registry;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import com.reiasu.reiparticleskill.entities.BarrageItemEntity;
import com.reiasu.reiparticleskill.entities.SkillEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeSkillEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ReiParticleSkillConstants.MOD_ID);

    static {
        ENTITIES.register(SkillEntityTypes.BARRAGE_ITEM.path(), () ->
                EntityType.Builder.<BarrageItemEntity>of(BarrageItemEntity::new, MobCategory.MISC)
                        .sized(0.4F, 0.4F)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(SkillEntityTypes.BARRAGE_ITEM.id().toString()));
    }

    private ForgeSkillEntityTypes() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
