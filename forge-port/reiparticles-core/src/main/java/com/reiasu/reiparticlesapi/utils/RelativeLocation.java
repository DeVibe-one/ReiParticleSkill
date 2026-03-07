// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import net.minecraft.world.phys.Vec3;

public class RelativeLocation {
    private double x;
    private double y;
    private double z;

    public RelativeLocation() {
        this(0.0, 0.0, 0.0);
    }

    public RelativeLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public RelativeLocation copy() {
        return new RelativeLocation(x, y, z);
    }

    public Vec3 toVector() {
        return new Vec3(x, y, z);
    }

    public static RelativeLocation of(Vec3 vec) {
        return new RelativeLocation(vec.x, vec.y, vec.z);
    }

    public RelativeLocation add(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        return this;
    }

    public RelativeLocation add(RelativeLocation other) {
        return add(other.x, other.y, other.z);
    }

    public RelativeLocation subtract(RelativeLocation other) {
        return add(-other.x, -other.y, -other.z);
    }

    public RelativeLocation scale(double factor) {
        this.x *= factor;
        this.y *= factor;
        this.z *= factor;
        return this;
    }

    public RelativeLocation unaryMinus() {
        return new RelativeLocation(-x, -y, -z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public RelativeLocation normalize() {
        double len = length();
        if (len == 0.0) {
            return this;
        }
        return scale(1.0 / len);
    }

    public double distance(RelativeLocation other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public RelativeLocation plus(RelativeLocation other) {
        return new RelativeLocation(x + other.x, y + other.y, z + other.z);
    }

    public RelativeLocation minus(RelativeLocation other) {
        return new RelativeLocation(x - other.x, y - other.y, z - other.z);
    }

    @Override
    public RelativeLocation clone() {
        return new RelativeLocation(x, y, z);
    }

    public RelativeLocation multiply(double factor) {
        return scale(factor);
    }

    public RelativeLocation multiplyClone(double factor) {
        return new RelativeLocation(x * factor, y * factor, z * factor);
    }

    public static class Companion {
        public static RelativeLocation of(Vec3 vec) {
            return new RelativeLocation(vec.x, vec.y, vec.z);
        }

        public static RelativeLocation yAxis() {
            return new RelativeLocation(0.0, 1.0, 0.0);
        }

        public static RelativeLocation xAxis() {
            return new RelativeLocation(1.0, 0.0, 0.0);
        }

        public static RelativeLocation zAxis() {
            return new RelativeLocation(0.0, 0.0, 1.0);
        }
    }
}
