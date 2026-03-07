// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

/**
 * 3D rotation matrix constructed from an axis-angle representation.
 * Uses Rodrigues' rotation formula.
 */
public final class RotationMatrix {
    private final double[][] matrix;

    private RotationMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    /**
     * Apply the rotation to a clone of the given point (does not modify the original).
     */
    public RelativeLocation applyToClone(RelativeLocation point) {
        return applyTo(point.copy());
    }

    /**
     * Apply the rotation in-place and return the modified point.
     */
    public RelativeLocation applyTo(RelativeLocation point) {
        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        point.setX(matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z);
        point.setY(matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z);
        point.setZ(matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z);
        return point;
    }

    /**
     * Create a rotation matrix from an axis and angle (radians).
     *
     * @param axis  the rotation axis (will be normalized internally)
     * @param angle the rotation angle in radians
     */
    public static RotationMatrix fromAxisAngle(RelativeLocation axis, double angle) {
        RelativeLocation u = axis.copy().normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double omc = 1.0 - cos;
        double ux = u.getX(), uy = u.getY(), uz = u.getZ();
        double[][] m = {
                {cos + ux * ux * omc, ux * uy * omc - uz * sin, ux * uz * omc + uy * sin},
                {uy * ux * omc + uz * sin, cos + uy * uy * omc, uy * uz * omc - ux * sin},
                {uz * ux * omc - uy * sin, uz * uy * omc + ux * sin, cos + uz * uz * omc}
        };
        return new RotationMatrix(m);
    }
}
