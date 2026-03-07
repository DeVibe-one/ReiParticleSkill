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
package com.reiasu.reiparticleskill.compat.interop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReflectCompatTest {
    @Test
    void shouldInvokeStaticAndInstanceNoArgMethods() {
        ReflectCompatFixture.STATIC_CALLED = false;
        ReflectCompatFixture.INSTANCE.instanceCalled = false;

        String fixtureClass = ReflectCompatFixture.class.getName();

        assertTrue(ReflectCompat.invokeStaticNoArg(fixtureClass, "staticPing"));
        assertTrue(ReflectCompatFixture.STATIC_CALLED);

        assertTrue(ReflectCompat.invokeOnInstanceNoArg(fixtureClass, "INSTANCE", "instancePing"));
        assertTrue(ReflectCompatFixture.INSTANCE.instanceCalled);
    }

    @Test
    void shouldResolveCompatibleMethodByAssignableSignature() throws Exception {
        var method = ReflectCompat.findMethod(
                ReflectCompatFixture.class,
                "acceptObject",
                String.class
        );
        assertNotNull(method);
        method.invoke(ReflectCompatFixture.INSTANCE, "ok");
    }

    @Test
    void shouldTryCandidateMethodsUntilOneMatches() {
        String fixtureClass = ReflectCompatFixture.class.getName();
        assertTrue(ReflectCompat.invokeAnyStaticNoArg(fixtureClass, "missing", "staticPing"));
        assertTrue(ReflectCompat.invokeAnyOnInstanceNoArg(fixtureClass, "INSTANCE", "missing", "instancePing"));
    }
}
