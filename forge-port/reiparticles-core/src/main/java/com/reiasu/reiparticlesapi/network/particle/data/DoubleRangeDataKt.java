// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

public final class DoubleRangeDataKt {
    private DoubleRangeDataKt() {
    }

    public static DoubleRangeData minRangeTo(double min, double max) {
        return new DoubleRangeData(min, max);
    }
}

