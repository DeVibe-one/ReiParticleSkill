// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

/**
 * Bit manipulation utility for compact status storage in int/long containers.
 */
public final class MathDataUtil {
    private MathDataUtil() {
    }

    public static int setStatusInt(int container, int bit, boolean status) {
        if (bit > 32 || bit <= 0) {
            return container;
        }
        int move = bit - 1;
        return status ? container | (1 << move) : (container ^ (1 << move)) & container;
    }

    public static int getStatusInt(int container, int bit) {
        if (bit < 1 || bit > 32) {
            return -1;
        }
        int move = bit - 1;
        return (container & (1 << move)) >>> move;
    }

    public static long setStatusLong(long container, int bit, boolean status) {
        if (bit < 1 || bit > 64) {
            return container;
        }
        int move = bit - 1;
        return status ? container | (1L << move) : (container ^ (1L << move)) & container;
    }

    public static int getStatusLong(long container, int bit) {
        if (bit > 64 || bit <= 0) {
            return -1;
        }
        int move = bit - 1;
        return (int) ((container & (1L << move)) >>> move);
    }

    public static int getStoragePageInt(int index) {
        return index / 32;
    }

    public static int getStorageWithBitInt(int index) {
        return index - getStoragePageInt(index) * 32;
    }

    public static int getStoragePageLong(int index) {
        return index / 64;
    }

    public static int getStorageWithBitLong(int index) {
        return index - getStoragePageLong(index) * 64;
    }
}
