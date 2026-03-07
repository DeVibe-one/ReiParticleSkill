// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LightFlashDisplay extends DisplayEntity implements ServerMovableDisplay {
    public static final String TYPE_ID = "reiparticleskill:light_flash_display";
    private static final DustParticleOptions COLOR = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.35F), 1.0F);

    private Vec3 pos;
    private int bloomCount = 2;
    private int age;
    private int maxAge = 60;
    private float lengthMax = 100.0F;
    private float thicknessMax = 7.0F;

    public LightFlashDisplay(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public byte[] encodeToBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DisplayEntity.encodeBase(this, buf);
        buf.writeInt(bloomCount);
        buf.writeInt(age);
        buf.writeInt(maxAge);
        buf.writeFloat(lengthMax);
        buf.writeFloat(thicknessMax);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    public static LightFlashDisplay decode(FriendlyByteBuf buf) {
        LightFlashDisplay display = new LightFlashDisplay(Vec3.ZERO);
        DisplayEntity.decodeBase(display, buf);
        display.setBloomCount(buf.readInt());
        display.setAge(buf.readInt());
        display.setMaxAge(buf.readInt());
        display.setLengthMax(buf.readFloat());
        display.setThicknessMax(buf.readFloat());
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

    public int getBloomCount() {
        return bloomCount;
    }

    public void setBloomCount(int bloomCount) {
        this.bloomCount = Math.max(0, bloomCount);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = Math.max(1, maxAge);
    }

    public float getLengthMax() {
        return lengthMax;
    }

    public void setLengthMax(float lengthMax) {
        this.lengthMax = Math.max(1.0F, lengthMax);
    }

    public float getThicknessMax() {
        return thicknessMax;
    }

    public void setThicknessMax(float thicknessMax) {
        this.thicknessMax = Math.max(0.1F, thicknessMax);
    }

    @Override
    public void teleportTo(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    @Override
    public void tick() {
        age++;
        if (age > maxAge) {
            cancel();
            return;
        }

        Level level = level();
        if (level == null) {
            return;
        }

        float progress = Math.min(1.0F, age / (float) maxAge);
        float inv = 1.0F - progress;
        double halfLen = Math.max(1.0, lengthMax * progress * 0.15);
        double halfWide = Math.max(0.1, thicknessMax * inv * 0.15);

        int lineCount = Math.max(16, (int) (halfLen * 6.0));
        for (int i = -lineCount; i <= lineCount; i++) {
            double t = i / (double) lineCount;
            double dx = halfLen * t;
            spawn(level, pos.x + dx, pos.y, pos.z, halfWide);
            spawn(level, pos.x, pos.y + dx, pos.z, halfWide);
        }

        if (bloomCount > 0) {
            spawn(level, ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0);
        }
    }

    @Override
    public void update(DisplayEntity other) {
        super.update(other);
        if (!(other instanceof LightFlashDisplay d)) {
            return;
        }
        this.pos = d.pos;
        this.bloomCount = d.bloomCount;
        this.age = d.age;
        this.maxAge = d.maxAge;
        this.lengthMax = d.lengthMax;
        this.thicknessMax = d.thicknessMax;
    }

    private void spawn(Level level, double x, double y, double z, double spread) {
        spawn(level, COLOR, x, y, z, Math.max(1, bloomCount), spread);
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

