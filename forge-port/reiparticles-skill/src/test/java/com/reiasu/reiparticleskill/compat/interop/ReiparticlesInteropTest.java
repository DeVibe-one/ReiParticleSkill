/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesSkill.
 *
 * ReiParticlesSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.compat.interop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReiparticlesInteropTest {
    @Test
    void shouldPreferApiTestGroupForZeroOrNegativeIndex() {
        assertEquals(List.of("api-test-group-builder", "skill-test-builder"),
                ReiparticlesInterop.preferredTestBuilderIds(0));
        assertEquals(List.of("api-test-group-builder", "skill-test-builder"),
                ReiparticlesInterop.preferredTestBuilderIds(-1));
    }

    @Test
    void shouldPreferSkillTestGroupForPositiveIndex() {
        assertEquals(List.of("skill-test-builder", "api-test-group-builder"),
                ReiparticlesInterop.preferredTestBuilderIds(1));
        assertEquals(List.of("skill-test-builder", "api-test-group-builder"),
                ReiparticlesInterop.preferredTestBuilderIds(2));
    }

    @Test
    void shouldExcludeCurrentGroupWhenSelectingReplacementOrder() {
        assertEquals(List.of("skill-test-builder"),
                ReiparticlesInterop.preferredReplacementTestBuilderIds("api-test-group-builder", 1));
        assertEquals(List.of("api-test-group-builder"),
                ReiparticlesInterop.preferredReplacementTestBuilderIds("skill-test-builder", 0));
    }
}

