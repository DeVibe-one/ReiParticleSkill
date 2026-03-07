// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.exceptions;

import net.minecraft.resources.ResourceLocation;

/**
 * Thrown when a render pipeline's pipe linker function has not been set.
 */
public final class RenderPipeLinkerNotSetException extends Exception {

    private ResourceLocation renderID;

    public RenderPipeLinkerNotSetException(ResourceLocation renderID) {
        super("Render " + renderID + "'s pipe linker not set; use manager.setLinkerFunc to set pipes link");
        this.renderID = renderID;
    }

    public ResourceLocation getRenderID() { return renderID; }
    public void setRenderID(ResourceLocation renderID) { this.renderID = renderID; }
}
