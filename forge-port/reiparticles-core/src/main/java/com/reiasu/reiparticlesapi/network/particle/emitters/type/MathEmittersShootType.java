// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.type;

import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.utils.math.ExpressionEvaluator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Emitter shoot type that evaluates mathematical expressions to
 * compute particle spawn positions and directions.
 * <p>
 * Position expressions may reference variables: {@code i} (particle index),
 * {@code c} (particle count), {@code t} (tick).
 * <p>
 * Direction expressions may reference: {@code t} (tick), {@code x/y/z} (particle pos),
 * {@code ox/oy/oz} (emitter origin).
 */
public final class MathEmittersShootType implements EmittersShootType {

    public static final String ID = "math";

    public static final BufferCodec<EmittersShootType> CODEC = BufferCodec.of(
            (buf, type) -> {
                MathEmittersShootType math = (MathEmittersShootType) type;
                buf.writeUtf(math.x);
                buf.writeUtf(math.y);
                buf.writeUtf(math.z);
                buf.writeUtf(math.dx);
                buf.writeUtf(math.dy);
                buf.writeUtf(math.dz);
            },
            buf -> {
                MathEmittersShootType math = new MathEmittersShootType();
                math.x = buf.readUtf();
                math.y = buf.readUtf();
                math.z = buf.readUtf();
                math.dx = buf.readUtf();
                math.dy = buf.readUtf();
                math.dz = buf.readUtf();
                math.setup();
                return math;
            }
    );

    private String x = "0";
    private String y = "0";
    private String z = "0";
    private String dx = "0";
    private String dy = "0";
    private String dz = "0";

    public MathEmittersShootType() {
    }

    // ---- Getters / Setters ----

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public String getZ() { return z; }
    public void setZ(String z) { this.z = z; }

    public String getDx() { return dx; }
    public void setDx(String dx) { this.dx = dx; }

    public String getDy() { return dy; }
    public void setDy(String dy) { this.dy = dy; }

    public String getDz() { return dz; }
    public void setDz(String dz) { this.dz = dz; }

    // ---- Core logic ----

    /**
     * @deprecated No longer needed — evaluators are now created fresh per call.
     */
    @Deprecated
    public void setup() {
        // No-op: kept for binary compatibility with existing callers (e.g. CODEC).
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public List<Vec3> getPositions(Vec3 origin, int tick, int count) {
        List<Vec3> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double px = evalPosition(x, tick, count, i);
            double py = evalPosition(y, tick, count, i);
            double pz = evalPosition(z, tick, count, i);
            result.add(origin.add(new Vec3(px, py, pz)));
        }
        return result;
    }

    @Override
    public Vec3 getDefaultDirection(Vec3 enter, int tick, Vec3 pos, Vec3 origin) {
        double ddx = evalDirection(dx, tick, pos, origin);
        double ddy = evalDirection(dy, tick, pos, origin);
        double ddz = evalDirection(dz, tick, pos, origin);
        return enter.add(ddx, ddy, ddz);
    }

    // ---- Helpers ----

    private static double evalPosition(String expr, int tick, int count, int index) {
        try {
            return new ExpressionEvaluator(expr)
                    .with("t", tick).with("c", count).with("i", index)
                    .evaluate();
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static double evalDirection(String expr, int tick, Vec3 pos, Vec3 origin) {
        try {
            return new ExpressionEvaluator(expr)
                    .with("t", tick)
                    .with("x", pos.x).with("y", pos.y).with("z", pos.z)
                    .with("ox", origin.x).with("oy", origin.y).with("oz", origin.z)
                    .evaluate();
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Encode this shoot type to a buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        CODEC.encode(buf, this);
    }

    /**
     * Decode a new instance from a buffer.
     */
    public static MathEmittersShootType decode(FriendlyByteBuf buf) {
        return (MathEmittersShootType) CODEC.decode(buf);
    }
}
