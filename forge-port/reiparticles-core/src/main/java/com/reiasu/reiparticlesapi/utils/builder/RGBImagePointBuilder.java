// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.utils.ImageUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder that converts an image resource into a map of
 * {@link RelativeLocation} to ARGB colour int values.
 * <p>
 * Non-transparent pixels are mapped to points in the XZ plane,
 * centred around the origin.
 */
public final class RGBImagePointBuilder {

    private final ResourceLocation image;
    private double scale = 1.0;
    private double step = 0.01;

    public RGBImagePointBuilder(ResourceLocation image) {
        this.image = image;
    }

    public ResourceLocation getImage() {
        return image;
    }

    public RGBImagePointBuilder scale(double scale) {
        this.scale = scale;
        return this;
    }

    public RGBImagePointBuilder step(double step) {
        this.step = step;
        return this;
    }

    public Map<RelativeLocation, Integer> build() {
        BufferedImage bufferedImage = ImageUtil.INSTANCE.loadFromIdentifier(image);
        if (bufferedImage == null) {
            return new HashMap<>();
        }
        BufferedImage scaled = ImageUtil.INSTANCE.scale(this.scale, bufferedImage);
        double offsetX = (double) scaled.getWidth() * step / 2.0;
        double offsetZ = (double) scaled.getHeight() * step / 2.0;
        Map<RelativeLocation, Integer> points = ImageUtil.INSTANCE.toPointsWithRGBA(scaled, step);
        for (Map.Entry<RelativeLocation, Integer> entry : points.entrySet()) {
            RelativeLocation loc = entry.getKey();
            loc.setX(loc.getX() - offsetX);
            loc.setZ(loc.getZ() - offsetZ);
        }
        return points;
    }
}
