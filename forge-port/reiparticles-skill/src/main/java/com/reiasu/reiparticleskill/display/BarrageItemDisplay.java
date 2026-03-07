// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.entities.BarrageItemEntity;
import io.netty.buffer.Unpooled;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BarrageItemDisplay extends DisplayEntity implements ServerMovableDisplay {
    public static final String TYPE_ID = "reiparticleskill:barrage_item_display";

    private ItemStack item = ItemStack.EMPTY;
    private boolean block;
    private int sign;
    private Vec3 pos;
    private Vec3 prevPos;
    private Vec3 velocity = Vec3.ZERO;
    private float yaw;
    private float prevYaw;
    private float pitch;
    private float prevPitch;
    private float roll;
    private float prevRoll;
    private float scale = 1.0F;
    private float preScale = 1.0F;
    private float targetScale = 1.0F;
    private float scaledSpeed = 0.2F;
    private float targetYaw;
    private float targetPitch;
    private float rotateSpeed = 20.0F;
    private int blendCount = 1;
    private int age;
    private int displayTick;

    public BarrageItemDisplay(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
        this.prevPos = this.pos;
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public byte[] encodeToBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DisplayEntity.encodeBase(this, buf);
        buf.writeItem(item);
        buf.writeBoolean(block);
        buf.writeInt(sign);
        writeVec3(buf, prevPos);
        writeVec3(buf, velocity);
        buf.writeFloat(prevYaw);
        buf.writeFloat(prevPitch);
        buf.writeFloat(prevRoll);
        buf.writeFloat(preScale);
        buf.writeFloat(targetScale);
        buf.writeFloat(scaledSpeed);
        buf.writeFloat(targetYaw);
        buf.writeFloat(targetPitch);
        buf.writeFloat(rotateSpeed);
        buf.writeInt(blendCount);
        buf.writeInt(age);
        buf.writeInt(displayTick);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    public static BarrageItemDisplay decode(FriendlyByteBuf buf) {
        BarrageItemDisplay display = new BarrageItemDisplay(Vec3.ZERO);
        DisplayEntity.decodeBase(display, buf);
        display.setItem(buf.readItem());
        display.setBlock(buf.readBoolean());
        display.setSign(buf.readInt());
        display.setPrevPos(readVec3(buf));
        display.setVelocity(readVec3(buf));
        display.setPrevYaw(buf.readFloat());
        display.setPrevPitch(buf.readFloat());
        display.setPrevRoll(buf.readFloat());
        display.setPreScale(buf.readFloat());
        display.setTargetScale(buf.readFloat());
        display.setScaledSpeed(buf.readFloat());
        display.setTargetYaw(buf.readFloat());
        display.setTargetPitch(buf.readFloat());
        display.setRotateSpeed(buf.readFloat());
        display.setBlendCount(buf.readInt());
        display.setAge(buf.readInt());
        display.setDisplayTick(buf.readInt());
        return display;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item == null ? ItemStack.EMPTY : item.copy();
    }

    public boolean isBlock() {
        return block;
    }

    public void setBlock(boolean block) {
        this.block = block;
    }

    public int getSign() {
        return sign;
    }

    public void setSign(int sign) {
        this.sign = sign;
    }

    @Override
    public Vec3 getPos() {
        return pos;
    }

    @Override
    public void setPos(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    public Vec3 getPrevPos() {
        return prevPos;
    }

    public void setPrevPos(Vec3 prevPos) {
        this.prevPos = prevPos == null ? Vec3.ZERO : prevPos;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity == null ? Vec3.ZERO : velocity;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPrevYaw() {
        return prevYaw;
    }

    public void setPrevYaw(float prevYaw) {
        this.prevYaw = prevYaw;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPrevPitch() {
        return prevPitch;
    }

    public void setPrevPitch(float prevPitch) {
        this.prevPitch = prevPitch;
    }

    @Override
    public float getRoll() {
        return roll;
    }

    @Override
    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getPrevRoll() {
        return prevRoll;
    }

    public void setPrevRoll(float prevRoll) {
        this.prevRoll = prevRoll;
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getPreScale() {
        return preScale;
    }

    public void setPreScale(float preScale) {
        this.preScale = preScale;
    }

    public float getTargetScale() {
        return targetScale;
    }

    public void setTargetScale(float targetScale) {
        this.targetScale = targetScale;
    }

    public float getScaledSpeed() {
        return scaledSpeed;
    }

    public void setScaledSpeed(float scaledSpeed) {
        this.scaledSpeed = Math.max(0.001F, scaledSpeed);
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public void setTargetYaw(float targetYaw) {
        this.targetYaw = targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    public void setTargetPitch(float targetPitch) {
        this.targetPitch = targetPitch;
    }

    public float getRotateSpeed() {
        return rotateSpeed;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = Math.max(0.0F, rotateSpeed);
    }

    public int getBlendCount() {
        return blendCount;
    }

    public void setBlendCount(int blendCount) {
        this.blendCount = Math.max(0, blendCount);
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    public int getDisplayTick() {
        return displayTick;
    }

    public void setDisplayTick(int displayTick) {
        this.displayTick = Math.max(0, displayTick);
    }

    public void rotateToPoint(RelativeLocation to) {
        if (to == null) {
            return;
        }
        RelativeLocation copy = to.copy();
        if (copy.length() < 1.0E-8) {
            return;
        }
        copy.normalize();
        targetYaw = (float) Math.toDegrees(Math.atan2(-copy.getX(), copy.getZ()));
        targetPitch = (float) Math.toDegrees(Math.atan2(-copy.getY(), Math.sqrt(copy.getX() * copy.getX() + copy.getZ() * copy.getZ())));
    }

    public float yaw(float delta) {
        return Mth.lerp(delta, prevYaw, yaw);
    }

    public float pitch(float delta) {
        return Mth.lerp(delta, prevPitch, pitch);
    }

    public float roll(float delta) {
        return Mth.lerp(delta, prevRoll, roll);
    }

    @Override
    public void tick() {
        age++;

        if (velocity.lengthSqr() > 1.0E-8) {
            rotateToPoint(RelativeLocation.of(velocity));
        }

        prevPos = pos;
        pos = pos.add(velocity);

        prevYaw = yaw;
        prevPitch = pitch;
        prevRoll = roll;

        yaw = approachAngle(yaw, targetYaw, rotateSpeed);
        pitch = approachAngle(pitch, targetPitch, rotateSpeed);

        preScale = scale;
        float deltaScale = targetScale - scale;
        if (Math.abs(deltaScale) <= scaledSpeed) {
            scale = targetScale;
        } else {
            scale += Math.signum(deltaScale) * scaledSpeed;
        }

        emitFallbackParticles();
    }

    public BarrageItemEntity recoverToBarrage() {
        if (level() == null) {
            return null;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        BarrageItemEntity entity = new BarrageItemEntity(serverLevel, pos, item.copy());
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setRoll(roll);
        entity.setScale(scale);
        entity.setDeltaMovement(velocity);
        entity.setBlock(block);
        cancel();
        return entity;
    }

    @Override
    public void teleportTo(Vec3 pos) {
        Vec3 safe = pos == null ? Vec3.ZERO : pos;
        this.prevPos = this.pos;
        this.pos = safe;
    }

    public void remove() {
        cancel();
    }

    @Override
    public void update(DisplayEntity other) {
        super.update(other);
        if (!(other instanceof BarrageItemDisplay d)) {
            return;
        }
        this.item = d.item.copy();
        this.block = d.block;
        this.sign = d.sign;
        this.pos = d.pos;
        this.prevPos = d.prevPos;
        this.velocity = d.velocity;
        this.yaw = d.yaw;
        this.prevYaw = d.prevYaw;
        this.pitch = d.pitch;
        this.prevPitch = d.prevPitch;
        this.roll = d.roll;
        this.prevRoll = d.prevRoll;
        this.scale = d.scale;
        this.preScale = d.preScale;
        this.targetScale = d.targetScale;
        this.scaledSpeed = d.scaledSpeed;
        this.targetYaw = d.targetYaw;
        this.targetPitch = d.targetPitch;
        this.rotateSpeed = d.rotateSpeed;
        this.blendCount = d.blendCount;
        this.age = d.age;
        this.displayTick = d.displayTick;
    }

    private static float approachAngle(float current, float target, float maxStep) {
        float wrapped = Mth.wrapDegrees(target - current);
        if (Math.abs(wrapped) <= maxStep) {
            return target;
        }
        return current + Math.signum(wrapped) * maxStep;
    }

    private void emitFallbackParticles() {
        if (age < displayTick) {
            return;
        }
        Level level = level();
        if (level == null) {
            return;
        }

        int coreCount = Math.max(1, blendCount);
        spawn(level, ParticleTypes.END_ROD, pos.x, pos.y + 0.05, pos.z, coreCount, 0.0);

        if (velocity.lengthSqr() > 1.0E-6) {
            Vec3 path = pos.subtract(prevPos);
            int steps = Math.max(2, (int) (path.length() * 8.0));
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                Vec3 p = prevPos.add(path.scale(t));
                spawn(level, ParticleTypes.ENCHANT, p.x, p.y, p.z, 1, 0.0);
            }
        } else {
            double pulse = 0.06 + Math.max(0.0, scale) * 0.004;
            spawn(level, ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, coreCount + 1, pulse);
        }
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

