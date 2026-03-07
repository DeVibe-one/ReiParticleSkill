/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticleSkill.
 *
 * ReiParticleSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticleSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticleSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.keys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SkillKeysTest {
    @Test
    void formationIdsAreStable() {
        assertEquals("reiparticleskill", SkillKeys.FORMATION_1.getNamespace());
        assertEquals("formation1", SkillKeys.FORMATION_1.getPath());
        assertEquals("reiparticleskill", SkillKeys.FORMATION_2.getNamespace());
        assertEquals("formation2", SkillKeys.FORMATION_2.getPath());
    }

    @Test
    void helperBuildsModScopedId() {
        assertEquals("reiparticleskill:test_id", SkillKeys.id("test_id").toString());
    }
}
