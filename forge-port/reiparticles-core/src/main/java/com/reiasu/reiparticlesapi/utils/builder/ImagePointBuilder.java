// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.utils.ImageUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder that converts an image resource into a list of
 * {@link RelativeLocation} points in the XZ plane.
 * <p>
 * Non-transparent pixels are mapped to points, centred around the origin.
 */
public final class ImagePointBuilder {

    private final ResourceLocation image;
    private double scale = 1.0;
    private double step = 0.01;

    public ImagePointBuilder(ResourceLocation image) {
        this.image = image;
    }

    public ResourceLocation getImage() {
        return image;
    }

    public ImagePointBuilder scale(double scale) {
        this.scale = scale;
        return this;
    }

    public ImagePointBuilder step(double step) {
        this.step = step;
        return this;
    }

    public List<RelativeLocation> build() {
        BufferedImage bufferedImage = ImageUtil.INSTANCE.loadFromIdentifier(image);
        if (bufferedImage == null) {
            return new ArrayList<>();
        }
        BufferedImage scaled = ImageUtil.INSTANCE.scale(this.scale, bufferedImage);
        double offsetX = (double) scaled.getWidth() * step / 2.0;
        double offsetZ = (double) scaled.getHeight() * step / 2.0;
        List<RelativeLocation> points = ImageUtil.INSTANCE.toPoints(scaled, step);
        for (RelativeLocation point : points) {
            point.setX(point.getX() - offsetX);
            point.setZ(point.getZ() - offsetZ);
        }
        return points;
    }
}
