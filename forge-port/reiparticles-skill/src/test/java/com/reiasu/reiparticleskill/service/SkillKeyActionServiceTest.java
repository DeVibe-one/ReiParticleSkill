// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.service;

import com.reiasu.reiparticleskill.keys.SkillKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillKeyActionServiceTest {
    @Test
    void shouldResolveOnlyKnownSkillKeys() {
        assertEquals(SkillKeyActionService.KeySkillAction.FORMATION_1,
                SkillKeyActionService.resolveKeyAction(SkillKeys.FORMATION_1));
        assertEquals(SkillKeyActionService.KeySkillAction.FORMATION_2,
                SkillKeyActionService.resolveKeyAction(SkillKeys.FORMATION_2));
        assertNull(SkillKeyActionService.resolveKeyAction(new ResourceLocation("reiparticleskill", "unknown")));
    }
}
