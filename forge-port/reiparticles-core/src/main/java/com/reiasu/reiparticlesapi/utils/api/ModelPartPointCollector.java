// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Collects sample points from a {@link ModelPart} hierarchy for particle placement.
 * <p>
 * Implementations walk the model-part tree and emit world-space positions
 * that can be used to spawn particles along the model's surface.
 */
public interface ModelPartPointCollector {
    /**
     * Collect sample points from the given model-part tree.
     *
     * @param root       the root model part
     * @param poseStack  the current pose stack (transformations applied to the model)
     * @param density    how many sample points to generate (higher = denser)
     * @param pixelToUnit whether to convert pixel coordinates to unit (1/16) scale
     * @return a list of sample positions in model-local space
     */
    List<Vec3> collectSamplePoints(ModelPart root, PoseStack poseStack, int density, boolean pixelToUnit);

    /**
     * Convenience overload with {@code pixelToUnit} defaulting to {@code true}.
     */
    default List<Vec3> collectSamplePoints(ModelPart root, PoseStack poseStack, int density) {
        return collectSamplePoints(root, poseStack, density, true);
    }
}
