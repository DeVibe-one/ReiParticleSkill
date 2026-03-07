// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility singleton for loading images from mod resources and converting
 * them to particle point clouds.
 * <p>
 * Points are generated in the XZ plane (Y=0) with configurable step spacing.
 * Only non-transparent pixels are included.
 */
public final class ImageUtil {

    public static final ImageUtil INSTANCE = new ImageUtil();

    private ImageUtil() {
    }

    /**
     * Loads a {@link BufferedImage} from the mod's resource path.
     *
     * @param id the resource location (namespace:path)
     * @return the loaded image, or null if not found
     */
    @Nullable
    public BufferedImage loadFromIdentifier(ResourceLocation id) {
        String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return null;
            return ImageIO.read(stream);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Scales an image by the given percentage (e.g. 0.5 = 50%).
     */
    public BufferedImage scale(double scalePercent, BufferedImage image) {
        int newWidth = (int) Math.round(image.getWidth() * scalePercent);
        int newHeight = (int) Math.round(image.getHeight() * scalePercent);
        BufferedImage result = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return result;
    }

    /**
     * Converts non-transparent pixels to a list of {@link RelativeLocation} points
     * in the XZ plane.
     *
     * @param image the source image
     * @param step  the spacing between pixel positions in world units
     * @return list of points corresponding to non-transparent pixels
     */
    public List<RelativeLocation> toPoints(BufferedImage image, double step) {
        List<RelativeLocation> result = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (getAlpha(image, x, y) != 0) {
                    result.add(new RelativeLocation(x * step, 0.0, y * step));
                }
            }
        }
        return result;
    }

    /**
     * Converts non-transparent pixels to a map of point -> alpha value.
     *
     * @param image the source image
     * @param step  the spacing in world units
     * @return map from point to alpha (0-255)
     */
    public Map<RelativeLocation, Integer> toPointsWithAlpha(BufferedImage image, double step) {
        Map<RelativeLocation, Integer> result = new HashMap<>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int alpha = getAlpha(image, x, y);
                if (alpha != 0) {
                    result.put(new RelativeLocation(x * step, 0.0, y * step), alpha);
                }
            }
        }
        return result;
    }

    /**
     * Converts non-transparent pixels to a map of point -> full RGBA color int.
     *
     * @param image the source image
     * @param step  the spacing in world units
     * @return map from point to ARGB color value
     */
    public Map<RelativeLocation, Integer> toPointsWithRGBA(BufferedImage image, double step) {
        Map<RelativeLocation, Integer> result = new HashMap<>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int alpha = getAlpha(image, x, y);
                if (alpha != 0) {
                    result.put(new RelativeLocation(x * step, 0.0, y * step), image.getRGB(x, y));
                }
            }
        }
        return result;
    }

    private int getAlpha(BufferedImage image, int x, int y) {
        if (!image.getColorModel().hasAlpha()) {
            return 255;
        }
        int pixel = image.getRGB(x, y);
        return (pixel >> 24) & 0xFF;
    }
}
