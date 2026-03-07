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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class ReflectCompat {
    private ReflectCompat() {
    }

    static boolean invokeStaticNoArg(String className, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Method method = type.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(null);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    static boolean invokeOnInstanceNoArg(String className, String fieldName, String methodName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object instance = field.get(null);
            Method method = type.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(instance);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    static boolean invokeAnyStaticNoArg(String className, String... methodNames) {
        for (String methodName : methodNames) {
            if (invokeStaticNoArg(className, methodName)) {
                return true;
            }
        }
        return false;
    }

    static boolean invokeAnyOnInstanceNoArg(String className, String fieldName, String... methodNames) {
        for (String methodName : methodNames) {
            if (invokeOnInstanceNoArg(className, fieldName, methodName)) {
                return true;
            }
        }
        return false;
    }

    static Method findMethod(Class<?> type, String methodName, Class<?>... argumentTypes) {
        for (Method method : type.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != argumentTypes.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!parameterTypes[i].isAssignableFrom(argumentTypes[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
