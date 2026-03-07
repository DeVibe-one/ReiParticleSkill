// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SwordLightDisplay extends DisplayEntity implements ServerMovableDisplay {
    public static final String TYPE_ID = "reiparticleskill:sword_light_display";

    private Vec3 pos;
    private Vec3 end;
    private int maxAge = 20;
    private int bloomCount = 2;
    private float thinness = 0.125F;
    private int age;
    private boolean removed;

    public SwordLightDisplay(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
        this.end = this.pos;
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public byte[] encodeToBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DisplayEntity.encodeBase(this, buf);
        writeVec3(buf, end);
        buf.writeInt(maxAge);
        buf.writeInt(bloomCount);
        buf.writeFloat(thinness);
        buf.writeInt(age);
        buf.writeBoolean(removed);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    public static SwordLightDisplay decode(FriendlyByteBuf buf) {
        SwordLightDisplay display = new SwordLightDisplay(Vec3.ZERO);
        DisplayEntity.decodeBase(display, buf);
        display.setEnd(readVec3(buf));
        display.setMaxAge(buf.readInt());
        display.setBloomCount(buf.readInt());
        display.setThinness(buf.readFloat());
        display.age = buf.readInt();
        display.removed = buf.readBoolean();
        return display;
    }

    @Override
    public Vec3 getPos() {
        return pos;
    }

    @Override
    public void setPos(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    public Vec3 getEnd() {
        return end;
    }

    public void setEnd(Vec3 end) {
        this.end = end == null ? pos : end;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = Math.max(1, maxAge);
    }

    public int getBloomCount() {
        return bloomCount;
    }

    public void setBloomCount(int bloomCount) {
        this.bloomCount = Math.max(0, bloomCount);
    }

    public float getThinness() {
        return thinness;
    }

    public void setThinness(float thinness) {
        this.thinness = Math.max(0.01F, thinness);
    }

    public int getAge() {
        return age;
    }

    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void teleportTo(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    @Override
    public void tick() {
        age++;
        if (age > maxAge) {
            if (!removed) {
                removed = true;
                age = 0;
            } else {
                cancel();
                return;
            }
        }

        Level level = level();
        if (level == null) {
            return;
        }

        Vec3 diff = end.subtract(pos);
        int samples = Math.max(6, (int) (diff.length() * 6.0));
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            Vec3 p = pos.add(diff.scale(t));
            spawn(level, ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0);
            if (i % 4 == 0 && bloomCount > 0) {
                spawn(level, ParticleTypes.ENCHANT, p.x, p.y, p.z, bloomCount, thinness);
            }
        }
    }

    @Override
    public void update(DisplayEntity other) {
        super.update(other);
        if (!(other instanceof SwordLightDisplay d)) {
            return;
        }
        this.pos = d.pos;
        this.end = d.end;
        this.maxAge = d.maxAge;
        this.bloomCount = d.bloomCount;
        this.thinness = d.thinness;
        this.age = d.age;
        this.removed = d.removed;
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 value) {
        Vec3 safe = value == null ? Vec3.ZERO : value;
        buf.writeDouble(safe.x);
        buf.writeDouble(safe.y);
        buf.writeDouble(safe.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void spawn(Level level, net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z, int count, double spread) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count, spread, spread, spread, 0.0);
            return;
        }
        int safe = Math.max(1, count);
        for (int i = 0; i < safe; i++) {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}

