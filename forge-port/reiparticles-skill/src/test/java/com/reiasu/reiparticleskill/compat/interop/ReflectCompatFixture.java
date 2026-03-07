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

public final class ReflectCompatFixture {
    public static final ReflectCompatFixture INSTANCE = new ReflectCompatFixture();
    public static boolean STATIC_CALLED = false;
    public boolean instanceCalled = false;

    private ReflectCompatFixture() {
    }

    public static void staticPing() {
        STATIC_CALLED = true;
    }

    public void instancePing() {
        instanceCalled = true;
    }

    public void acceptObject(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value");
        }
    }
}
