// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.sounds;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SkillSoundEvents {
    public static final SoundEventRef SWORD_FORMATION = sound("sword_formation");

    private SkillSoundEvents() {
    }

    private static SoundEventRef sound(String path) {
        return new SoundEventRef(new ResourceLocation(ReiParticleSkillConstants.MOD_ID, path));
    }

    public static final class SoundEventRef {
        private final ResourceLocation id;

        private SoundEventRef(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public String path() {
            return id.getPath();
        }

        public boolean isPresent() {
            return BuiltInRegistries.SOUND_EVENT.containsKey(id);
        }

        public SoundEvent get() {
            return BuiltInRegistries.SOUND_EVENT.get(id);
        }
    }
}
