// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.registry;

import com.reiasu.reiparticleskill.ReiParticleSkillConstants;
import com.reiasu.reiparticleskill.sounds.SkillSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeSkillSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ReiParticleSkillConstants.MOD_ID);

    static {
        SOUND_EVENTS.register(SkillSoundEvents.SWORD_FORMATION.path(),
                () -> SoundEvent.createVariableRangeEvent(SkillSoundEvents.SWORD_FORMATION.id()));
    }

    private ForgeSkillSoundEvents() {
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
