// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.exceptions;

import net.minecraft.resources.ResourceLocation;

/**
 * Thrown when a render entity's bound pipe manager cannot be found.
 */
public final class RenderPipeNotFoundException extends Exception {

    private ResourceLocation renderID;

    public RenderPipeNotFoundException(ResourceLocation renderID) {
        super("Render " + renderID + "'s bound pipe manager not found; "
                + "use ClientRenderEntityManager.bindEntityRenderPipe(YourRenderEntity.ID, pipeID) to bind an output pipe manager");
        this.renderID = renderID;
    }

    public ResourceLocation getRenderID() { return renderID; }
    public void setRenderID(ResourceLocation renderID) { this.renderID = renderID; }
}
