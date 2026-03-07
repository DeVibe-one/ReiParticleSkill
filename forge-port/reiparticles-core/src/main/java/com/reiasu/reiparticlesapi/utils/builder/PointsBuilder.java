// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.network.particle.composition.CompositionData;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;

public class PointsBuilder {
    private final List<RelativeLocation> points = new ArrayList<>();
    private RelativeLocation axis = new RelativeLocation(0.0, 0.0, 1.0);

    public PointsBuilder axis(RelativeLocation axis) {
        this.axis = axis.copy();
        return this;
    }

    public PointsBuilder addLine(RelativeLocation from, RelativeLocation to, int steps) {
        if (steps <= 1) {
            points.add(from.copy());
            points.add(to.copy());
            return this;
        }

        int n = Math.max(2, steps);
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            points.add(new RelativeLocation(
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t
            ));
        }
        return this;
    }

    public PointsBuilder addCircle(double radius, int samples) {
        if (samples <= 0) {
            return this;
        }
        int n = samples;
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2.0 * i / n;
            points.add(new RelativeLocation(Math.cos(t) * radius, 0.0, Math.sin(t) * radius));
        }
        return this;
    }

    public PointsBuilder addDiscreteCircleXZ(double radius, int samples, double discrete) {
        if (samples <= 0) {
            return this;
        }
        int n = Math.max(1, samples);
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * 2.0 * i / n;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            if (discrete > 0.0) {
                double randomR = ThreadLocalRandom.current().nextDouble(0.0, discrete);
                double rx = ThreadLocalRandom.current().nextDouble(-Math.PI, Math.PI);
                double ry = ThreadLocalRandom.current().nextDouble(-Math.PI, Math.PI);
                x += randomR * Math.cos(rx) * Math.cos(ry);
                z += randomR * Math.sin(ry) * Math.cos(rx);
                double y = randomR * Math.sin(rx);
                points.add(new RelativeLocation(x, y, z));
                continue;
            }
            points.add(new RelativeLocation(x, 0.0, z));
        }
        return this;
    }

    public PointsBuilder addBall(double radius, int samples) {
        if (samples <= 0 || radius <= 0.0) {
            return this;
        }
        int n = Math.max(1, samples);
        for (int i = 0; i < n; i++) {
            RelativeLocation random = randomUnitVector();
            double r = ThreadLocalRandom.current().nextDouble(0.0, radius);
            points.add(random.scale(r));
        }
        return this;
    }

    public PointsBuilder addFourierSeries(FourierSeriesBuilder builder) {
        if (builder != null) {
            points.addAll(builder.build());
        }
        return this;
    }

    public PointsBuilder addPolygonInCircle(int sides, int pointsPerEdge, double radius) {
        List<RelativeLocation> vertices = Math3DUtil.getPolygonInCircleVertices(sides, radius);
        if (vertices.size() < 3) {
            return this;
        }
        for (int i = 0; i < vertices.size(); i++) {
            addLine(vertices.get(i), vertices.get((i + 1) % vertices.size()), pointsPerEdge);
        }
        return this;
    }

    public PointsBuilder pointsOnEach(Consumer<RelativeLocation> consumer) {
        for (RelativeLocation p : points) {
            consumer.accept(p);
        }
        return this;
    }

    /**
     * @deprecated Math3DUtil is now all-static. Use {@code addAll(generator.apply(null))} or call Math3DUtil methods directly.
     */
    @Deprecated
    public PointsBuilder addWith(Function<Math3DUtil, Collection<RelativeLocation>> generator) {
        Collection<RelativeLocation> generated = generator.apply(null);
        if (generated != null) {
            points.addAll(generated);
        }
        return this;
    }

    public PointsBuilder rotateAsAxis(double radians) {
        return rotateAsAxis(radians, axis);
    }

    /**
     * Rotates the collected points around the world Y axis.
     */
    public PointsBuilder rotateAroundY(double radians) {
        return rotateAsAxis(radians, new RelativeLocation(0.0, 1.0, 0.0));
    }

    public PointsBuilder rotateAsAxis(double radians, RelativeLocation axis) {
        RelativeLocation normalizedAxis = axis == null ? new RelativeLocation(0.0, 0.0, 1.0) : axis.copy().normalize();
        if (normalizedAxis.length() == 0.0) {
            return this;
        }
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (RelativeLocation point : points) {
            double x = point.getX();
            double y = point.getY();
            double z = point.getZ();
            double ux = normalizedAxis.getX();
            double uy = normalizedAxis.getY();
            double uz = normalizedAxis.getZ();
            // Rodrigues' rotation formula.
            double dot = ux * x + uy * y + uz * z;
            double rx = x * cos + (uy * z - uz * y) * sin + ux * dot * (1.0 - cos);
            double ry = y * cos + (uz * x - ux * z) * sin + uy * dot * (1.0 - cos);
            double rz = z * cos + (ux * y - uy * x) * sin + uz * dot * (1.0 - cos);
            point.setX(rx);
            point.setY(ry);
            point.setZ(rz);
        }
        return this;
    }

    public PointsBuilder rotateTo(RelativeLocation direction) {
        RelativeLocation dir = direction.copy().normalize();
        if (dir.length() == 0.0) {
            return this;
        }
        double yaw = Math3DUtil.getYawFromLocation(dir);
        rotateAsAxis(yaw);
        return this;
    }

    public PointsBuilder addPoint(RelativeLocation point) {
        points.add(point.copy());
        return this;
    }

    public PointsBuilder withBuilder(PointsBuilder other) {
        points.addAll(other.createWithoutClone());
        return this;
    }

    public <T> Map<T, RelativeLocation> createWithStyleData(Function<RelativeLocation, T> styleFactory) {
        Map<T, RelativeLocation> mapped = new LinkedHashMap<>();
        for (RelativeLocation point : points) {
            RelativeLocation copy = point.copy();
            mapped.put(styleFactory.apply(copy), copy);
        }
        return mapped;
    }

    /**
     * Convenience method that delegates to {@link #createWithStyleData(Function)}.
     * Equivalent to the Kotlin extension used by SequencedParticleShapeComposition.
     */
    public Map<CompositionData, RelativeLocation> createWithCompositionData(
            Function<RelativeLocation, CompositionData> dataFactory) {
        return createWithStyleData(dataFactory);
    }

    public List<RelativeLocation> create() {
        return new ArrayList<>(points);
    }

    public List<RelativeLocation> createWithoutClone() {
        return Collections.unmodifiableList(points);
    }

    public RelativeLocation getAxis() {
        return axis.copy();
    }

    private RelativeLocation randomUnitVector() {
        while (true) {
            double x = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double y = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double z = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
            double lengthSqr = x * x + y * y + z * z;
            if (lengthSqr < 1.0E-6 || lengthSqr > 1.0) {
                continue;
            }
            double inv = 1.0 / Math.sqrt(lengthSqr);
            return new RelativeLocation(x * inv, y * inv, z * inv);
        }
    }
}

