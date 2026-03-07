// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.helper.impl.GroupBezierValueScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.GroupProgressSequencedHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.GroupScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.ParticleAlphaHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleAlphaHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleBezierValueScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleProgressSequencedHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleScaleHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.StyleStatusHelper;
import com.reiasu.reiparticlesapi.utils.helper.impl.composition.CompositionBezierScaleHelper;

public final class HelperUtil {
    public static final HelperUtil INSTANCE = new HelperUtil();

    private HelperUtil() {
    }

    public StyleProgressSequencedHelper sequencedStyle(int maxCount, int progressMaxTick) {
        return new StyleProgressSequencedHelper(maxCount, progressMaxTick);
    }

    public GroupProgressSequencedHelper sequencedGroup(int maxCount, int progressMaxTick) {
        return new GroupProgressSequencedHelper(maxCount, progressMaxTick);
    }

    public StyleScaleHelper scaleStyle(double minScale, double maxScale, int scaleTick) {
        return new StyleScaleHelper(minScale, maxScale, scaleTick);
    }

    public BezierValueScaleHelper bezierValueScaleStyle(
            double minScale,
            double maxScale,
            int scaleTick,
            RelativeLocation c1,
            RelativeLocation c2
    ) {
        return new StyleBezierValueScaleHelper(scaleTick, minScale, maxScale, c1, c2);
    }

    public CompositionBezierScaleHelper bezierValueScaleComposition(
            double minScale,
            double maxScale,
            int scaleTick,
            RelativeLocation c1,
            RelativeLocation c2
    ) {
        return new CompositionBezierScaleHelper(scaleTick, minScale, maxScale, c1, c2);
    }

    public GroupBezierValueScaleHelper bezierValueScaleGroup(
            double minScale,
            double maxScale,
            int scaleTick,
            RelativeLocation c1,
            RelativeLocation c2
    ) {
        return new GroupBezierValueScaleHelper(scaleTick, minScale, maxScale, c1, c2);
    }

    public GroupScaleHelper scaleGroup(double minScale, double maxScale, int scaleTick) {
        return new GroupScaleHelper(minScale, maxScale, scaleTick);
    }

    public StyleStatusHelper styleStatus(int closedInterval) {
        StyleStatusHelper helper = new StyleStatusHelper();
        helper.setClosedInternal(closedInterval);
        return helper;
    }

    public StyleAlphaHelper alphaStyle(double minAlpha, double maxAlpha, int alphaTick) {
        return new StyleAlphaHelper(minAlpha, maxAlpha, alphaTick);
    }

    public ParticleAlphaHelper particleAlpha(double minAlpha, double maxAlpha, int alphaTick) {
        return new ParticleAlphaHelper(minAlpha, maxAlpha, alphaTick);
    }
}
