/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.testutil;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UnsafeAllocator {
    private static final Unsafe UNSAFE = loadUnsafe();
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

    private UnsafeAllocator() {
    }

    public static <T> T allocate(Class<T> type) {
        bootstrapMinecraft();
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to allocate instance for " + type.getName(), e);
        }
    }

    private static void bootstrapMinecraft() {
        if (!BOOTSTRAPPED.compareAndSet(false, true)) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static Unsafe loadUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access Unsafe", e);
        }
    }
}
