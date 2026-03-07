// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.entities;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class SkillEntityTypes {
    public static final EntityTypeRef<BarrageItemEntity> BARRAGE_ITEM = entityType("barrage_item");

    private SkillEntityTypes() {
    }

    private static <T extends Entity> EntityTypeRef<T> entityType(String path) {
        return new EntityTypeRef<>(new ResourceLocation(ReiParticleSkillConstants.MOD_ID, path));
    }

    public static final class EntityTypeRef<T extends Entity> {
        private final ResourceLocation id;

        private EntityTypeRef(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public String path() {
            return id.getPath();
        }

        public boolean isPresent() {
            return BuiltInRegistries.ENTITY_TYPE.containsKey(id);
        }

        @SuppressWarnings("unchecked")
        public EntityType<T> get() {
            return (EntityType<T>) BuiltInRegistries.ENTITY_TYPE.get(id);
        }
    }
}
