// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.display;

import net.minecraft.client.renderer.RenderType;

/**
 * Provider interface for custom glow render types used by display entities.
 */
public interface ReiRenderTypesProvider {
    /**
     * Return the glow {@link RenderType} for rendering display entities with bloom/glow effects.
     *
     * @return a RenderType configured for glow rendering
     */
    RenderType glow();
}
