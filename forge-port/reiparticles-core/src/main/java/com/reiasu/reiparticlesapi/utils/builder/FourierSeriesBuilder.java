// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.ArrayList;
import java.util.List;

public final class FourierSeriesBuilder {
    private int count = 360;
    private double scale = 1.0;
    private final List<Fourier> fouriers = new ArrayList<>();

    public FourierSeriesBuilder addFourier(double r, double w) {
        return addFourier(r, w, 0.0);
    }

    public FourierSeriesBuilder addFourier(double r, double w, double startAngleDeg) {
        fouriers.add(new Fourier(w, r, startAngleDeg));
        return this;
    }

    public FourierSeriesBuilder scale(double scale) {
        this.scale = scale;
        return this;
    }

    public FourierSeriesBuilder count(int count) {
        this.count = Math.max(1, count);
        return this;
    }

    public List<RelativeLocation> build() {
        List<RelativeLocation> result = new ArrayList<>();
        if (fouriers.isEmpty()) {
            return result;
        }
        double precision = Math.PI * 2.0 / (double) count;
        for (int i = 0; i < count; i++) {
            double t = i * precision;
            double x = 0.0;
            double z = 0.0;
            for (Fourier fourier : fouriers) {
                double angle = Math.toRadians(fourier.startAngleDeg) + fourier.w * t;
                x += fourier.r * Math.cos(angle) * scale;
                z += fourier.r * Math.sin(angle) * scale;
            }
            result.add(new RelativeLocation(x, 0.0, z));
        }
        return result;
    }

    public record Fourier(double w, double r, double startAngleDeg) {
    }
}
