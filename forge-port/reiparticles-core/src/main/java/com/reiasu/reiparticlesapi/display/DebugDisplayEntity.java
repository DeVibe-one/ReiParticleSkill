// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.display;

import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class DebugDisplayEntity extends DisplayEntity {
    public static final String TYPE_ID = "reiparticlesapi:debug_display";
    private Vec3 pos;
    private final String kind;
    private final int maxTicks;
    private int tick;
    private static final DustParticleOptions COLOR =
            new DustParticleOptions(new Vector3f(0.95f, 0.75f, 0.25f), 1.0f);

    public DebugDisplayEntity(double x, double y, double z, String kind) {
        this(null, x, y, z, kind, 80);
    }

    public DebugDisplayEntity(ServerLevel level, double x, double y, double z, String kind) {
        this(level, x, y, z, kind, 80);
    }

    public DebugDisplayEntity(ServerLevel level, double x, double y, double z, String kind, int maxTicks) {
        bindLevel(level);
        this.pos = new Vec3(x, y, z);
        this.kind = kind;
        this.maxTicks = Math.max(1, maxTicks);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public byte[] encodeToBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DisplayEntity.encodeBase(this, buf);
        buf.writeUtf(kind == null ? "" : kind);
        buf.writeInt(maxTicks);
        buf.writeInt(tick);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    public static DebugDisplayEntity decode(FriendlyByteBuf buf) {
        DebugDisplayEntity display = new DebugDisplayEntity(null, 0.0, 0.0, 0.0, "group", 1);
        DisplayEntity.decodeBase(display, buf);
        String kind = buf.readUtf();
        int maxTicks = buf.readInt();
        int tick = buf.readInt();
        DebugDisplayEntity decoded = new DebugDisplayEntity(null, display.getPos().x, display.getPos().y, display.getPos().z, kind, maxTicks);
        decoded.setYaw(display.getYaw());
        decoded.setPitch(display.getPitch());
        decoded.setRoll(display.getRoll());
        decoded.setScale(display.getScale());
        decoded.setValid(display.getValid());
        decoded.setControlUUID(display.getControlUUID());
        decoded.tick = tick;
        return decoded;
    }

    public double getX() {
        return pos.x;
    }

    public double getY() {
        return pos.y;
    }

    public double getZ() {
        return pos.z;
    }

    public String getKind() {
        return kind;
    }

    public int getMaxTicks() {
        return maxTicks;
    }

    public int getTickCount() {
        return tick;
    }

    @Override
    public Vec3 getPos() {
        return pos;
    }

    @Override
    public void setPos(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    @Override
    public void tick() {
        if (!getValid()) {
            return;
        }
        Level level = level();
        if (level != null) {
            double ringRadius = 0.4 + tick * 0.02;
            int points = "group".equalsIgnoreCase(kind) ? 8 : 5;
            for (int i = 0; i < points; i++) {
                double t = (Math.PI * 2.0 * i) / points + tick * 0.12;
                double px = pos.x + Math.cos(t) * ringRadius;
                double py = pos.y + 0.1 + Math.sin(t * 2.0) * 0.04;
                double pz = pos.z + Math.sin(t) * ringRadius;
                spawn(level, COLOR, px, py, pz, 1);
            }
            if ("group".equalsIgnoreCase(kind)) {
                spawn(level, ParticleTypes.END_ROD, pos.x, pos.y + 0.2, pos.z, 2);
            }
        }
        tick++;
        if (tick >= maxTicks) {
            cancel();
        }
    }

    private static void spawn(Level level, net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z, int count) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        int safe = Math.max(1, count);
        for (int i = 0; i < safe; i++) {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}

