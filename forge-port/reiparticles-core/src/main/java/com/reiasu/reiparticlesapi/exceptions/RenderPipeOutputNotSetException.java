// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.exceptions;

import net.minecraft.resources.ResourceLocation;

/**
 * Thrown when a render pipeline output has not been configured.
 */
public final class RenderPipeOutputNotSetException extends Exception {

    public RenderPipeOutputNotSetException(ResourceLocation pipeID) {
        super(pipeID + " has no output pipeline set");
    }
}
