// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation.timeline;

/**
 * Easing function interface for timeline animations.
 * Takes a progress value t in [0,1] and returns the eased value.
 */
@FunctionalInterface
public interface Ease {
    double cal(double t);
}
