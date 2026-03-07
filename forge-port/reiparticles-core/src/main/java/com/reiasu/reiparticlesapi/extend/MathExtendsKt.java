// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.extend;

/**
 * Math extension utilities providing degree-to-radian conversion and a float PI constant.
 * Originally Kotlin extension functions, ported as static utility methods.
 */
public final class MathExtendsKt {

    /** Float-precision PI constant. */
    public static final float PIF = (float) Math.PI;

    private MathExtendsKt() {
    }

    /** Converts a float angle in degrees to radians (float result). */
    public static float radianF(float angle) {
        return angle * PIF / 180.0f;
    }

    /** Converts a double angle in degrees to radians (float result). */
    public static float radianF(double angle) {
        return (float) angle * PIF / 180.0f;
    }

    /** Converts a double angle in degrees to radians (double result). */
    public static double radianD(double angle) {
        return angle * Math.PI / 180.0;
    }

    /** Converts a float angle in degrees to radians (double result). */
    public static double radianD(float angle) {
        return (double) angle * Math.PI / 180.0;
    }
}
