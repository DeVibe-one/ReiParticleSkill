/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PointsAndFourierBuilderTest {
    @Test
    void fourierBuilderKeepsRequestedCount() {
        FourierSeriesBuilder builder = new FourierSeriesBuilder()
                .count(314)
                .scale(0.2857142857142857)
                .addFourier(2.0, 4.0)
                .addFourier(-5.0, -3.0);
        assertEquals(314, builder.build().size());
    }

    @Test
    void discreteCircleProducesExpectedSamples() {
        PointsBuilder builder = new PointsBuilder()
                .addDiscreteCircleXZ(48.0, 200, 8.0);
        assertEquals(200, builder.create().size());
    }

    @Test
    void createWithStyleDataMapsEachPoint() {
        PointsBuilder builder = new PointsBuilder().addCircle(10.0, 32);
        Map<String, RelativeLocation> mapped = builder.createWithStyleData(point -> point.getX() > 0 ? "A" + point.getX() : "B" + point.getZ());
        assertFalse(mapped.isEmpty());
        assertTrue(mapped.values().stream().allMatch(point -> point != null));
    }
}
