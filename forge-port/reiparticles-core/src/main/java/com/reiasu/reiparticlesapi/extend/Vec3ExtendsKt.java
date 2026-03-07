// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.extend;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.concurrent.ThreadLocalRandom;

public final class Vec3ExtendsKt {
    private Vec3ExtendsKt() {
    }

    public static RelativeLocation plus(RelativeLocation a, RelativeLocation b) {
        return new RelativeLocation(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ());
    }

    public static RelativeLocation minus(RelativeLocation a, RelativeLocation b) {
        return new RelativeLocation(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    public static RelativeLocation times(RelativeLocation a, Number n) {
        double k = n.doubleValue();
        return new RelativeLocation(a.getX() * k, a.getY() * k, a.getZ() * k);
    }

    public static RelativeLocation unaryMinus(RelativeLocation a) {
        return new RelativeLocation(-a.getX(), -a.getY(), -a.getZ());
    }

    public static RelativeLocation asRelative(RelativeLocation a) {
        return new RelativeLocation(a.getX(), a.getY(), a.getZ());
    }

    public static RelativeLocation random(RelativeLocation range) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new RelativeLocation(
                (r.nextDouble() * 2.0 - 1.0) * range.getX(),
                (r.nextDouble() * 2.0 - 1.0) * range.getY(),
                (r.nextDouble() * 2.0 - 1.0) * range.getZ()
        );
    }

    public static RelativeLocation lengthCoerceAtMost(RelativeLocation value, double maxLen) {
        double len = Math.sqrt(value.getX() * value.getX() + value.getY() * value.getY() + value.getZ() * value.getZ());
        if (len <= maxLen || len == 0.0) {
            return value;
        }
        double k = maxLen / len;
        return new RelativeLocation(value.getX() * k, value.getY() * k, value.getZ() * k);
    }
}

