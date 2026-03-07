// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.PhysicsUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Core client-side particle class that can be remotely controlled via a {@link ParticleController}.
 * <p>
 * Extends {@link TextureSheetParticle} (Forge 1.20.1) and provides:
 * <ul>
 *   <li>UUID-based control via {@link com.reiasu.reiparticlesapi.particles.control.ControlParticleManager}</li>
 *   <li>Custom rotation (pitch/yaw/roll) with interpolation</li>
 *   <li>Teleportation and physics-aware movement</li>
 *   <li>Color, alpha, light, size control</li>
 *   <li>Camera-facing or fixed-orientation rendering</li>
 *   <li>Double-sided quad rendering (front and back faces)</li>
 * </ul>
 */
public abstract class ControllableParticle extends TextureSheetParticle {

    /**
     * Default linear interpolator using {@link GraphMathHelper#lerp(float, Vec3, Vec3)}.
     */
    public static final ParticleLerpInterpolator LINEAR_INTERPOLATOR =
            (prev, current, delta) -> GraphMathHelper.lerp(delta, prev, current);

    private final UUID controlUUID;
    private boolean faceToCamera;
    private ParticleLerpInterpolator interpolator;
    private final ControllableParticleControllerBridge controllerBridge;
    private boolean crossLiquid;
    private int light;
    private ParticleRenderType textureSheet;
    private boolean minecraftTick;
    private final ControllableParticleState runtimeState;

    public ControllableParticle(ClientLevel world, Vec3 pos, Vec3 velocity, UUID controlUUID, boolean faceToCamera) {
        super(world, pos.x, pos.y, pos.z, velocity.x, velocity.y, velocity.z);
        this.controlUUID = controlUUID;
        this.faceToCamera = faceToCamera;
        this.interpolator = LINEAR_INTERPOLATOR;
        this.runtimeState = new ControllableParticleState(pos);
        this.light = 15;
        this.textureSheet = ParticleRenderType.PARTICLE_SHEET_LIT;
        this.controllerBridge = ControllableParticleControllerBridge.attach(this.controlUUID, this);
        this.runtimeState.setPendingRotation(new Vector3f(
                this.runtimeState.getPreviewPitch(),
                this.runtimeState.getPreviewYaw(),
                this.runtimeState.getPreviewRoll()));
    }

    public ControllableParticle(ClientLevel world, Vec3 pos, Vec3 velocity, UUID controlUUID) {
        this(world, pos, velocity, controlUUID, true);
    }

    public UUID getControlUUID() {
        return controlUUID;
    }

    public boolean getFaceToCamera() {
        return faceToCamera;
    }

    public void setFaceToCamera(boolean faceToCamera) {
        this.faceToCamera = faceToCamera;
    }

    public ParticleLerpInterpolator getInterpolator() {
        return interpolator;
    }

    public ParticleController getController() {
        return controllerBridge.controller();
    }

    public boolean getCrossLiquid() {
        return crossLiquid;
    }

    public void setCrossLiquid(boolean crossLiquid) {
        this.crossLiquid = crossLiquid;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int value) {
        if (value == -1) {
            this.light = value;
            return;
        }
        this.light = Mth.clamp(value, 0, 15);
    }

    public ParticleRenderType getTextureSheet() {
        return textureSheet;
    }

    public void setTextureSheet(ParticleRenderType textureSheet) {
        this.textureSheet = textureSheet;
    }

    public boolean getMinecraftTick() {
        return minecraftTick;
    }

    public void setMinecraftTick(boolean minecraftTick) {
        this.minecraftTick = minecraftTick;
    }

    public ClientLevel getClientWorld() {
        return (ClientLevel) this.level;
    }

    public Vec3 getLoc() {
        return new Vec3(this.x, this.y, this.z);
    }

    public void setLoc(Vec3 value) {
        this.x = value.x;
        this.y = value.y;
        this.z = value.z;
    }

    public float getSize() {
        return this.quadSize;
    }

    public void setSize(float value) {
        this.quadSize = value;
        this.setSize(0.2f * this.quadSize, 0.2f * this.quadSize);
    }

    public Vec3 getPrevPos() {
        return new Vec3(this.xo, this.yo, this.zo);
    }

    public void setPrevPos(Vec3 value) {
        this.xo = value.x;
        this.yo = value.y;
        this.zo = value.z;
    }

    public Vec3 getVelocity() {
        return new Vec3(this.xd, this.yd, this.zd);
    }

    public void setVelocity(Vec3 value) {
        this.xd = value.x;
        this.yd = value.y;
        this.zd = value.z;
    }

    public AABB getBounding() {
        return this.getBoundingBox();
    }

    public void setBounding(AABB value) {
        this.setBoundingBox(value);
    }

    public boolean getOnTheGround() {
        return this.onGround;
    }

    public void setOnTheGround(boolean value) {
        this.onGround = value;
    }

    public boolean getCollidesWithTheWorld() {
        return this.hasPhysics;
    }

    public void setCollidesWithTheWorld(boolean value) {
        this.hasPhysics = value;
    }

    public boolean getDeath() {
        return this.removed;
    }

    public void setDeath(boolean value) {
        if (value) {
            this.remove();
        } else {
            this.removed = false;
        }
    }

    public Vector2f getSpacing() {
        return new Vector2f(this.bbWidth, this.bbHeight);
    }

    public void setSpacing(Vector2f value) {
        this.bbWidth = value.x;
        this.bbHeight = value.y;
    }

    public int getCurrentAge() {
        return this.age;
    }

    public void setCurrentAge(int value) {
        this.age = value;
    }

    public float getGravityStrength() {
        return this.gravity;
    }

    public void setGravityStrength(float value) {
        this.gravity = value;
    }

    public Vector3f getColor() {
        return new Vector3f(this.rCol, this.gCol, this.bCol);
    }

    public void setColor(Vector3f value) {
        this.rCol = value.x;
        this.gCol = value.y;
        this.bCol = value.z;
    }

    public float getParticleAlpha() {
        return this.alpha;
    }

    public void setParticleAlpha(float value) {
        this.alpha = Mth.clamp(value, 0.0f, 1.0f);
    }

    public float getPreviewPitch() {
        return runtimeState.getPreviewPitch();
    }

    public void setPreviewPitch(float value) {
        this.runtimeState.setPreviewPitch(value);
    }

    public float getCurrentPitch() {
        return runtimeState.getCurrentPitch();
    }

    public void setCurrentPitch(float value) {
        this.runtimeState.setCurrentPitch(value);
    }

    public float getPreviewYaw() {
        return runtimeState.getPreviewYaw();
    }

    public void setPreviewYaw(float value) {
        this.runtimeState.setPreviewYaw(value);
    }

    public float getCurrentYaw() {
        return runtimeState.getCurrentYaw();
    }

    public void setCurrentYaw(float value) {
        this.runtimeState.setCurrentYaw(value);
    }

    public float getPreviewRoll() {
        return runtimeState.getPreviewRoll();
    }

    public void setPreviewRoll(float value) {
        this.oRoll = value;
        this.runtimeState.setPreviewRoll(value);
    }

    public float getCurrentRoll() {
        return runtimeState.getCurrentRoll();
    }

    public void setCurrentRoll(float value) {
        this.roll = value;
        this.runtimeState.setCurrentRoll(value);
    }

    public float getVelocityMulti() {
        return this.friction;
    }

    public void setVelocityMulti(float value) {
        this.friction = value;
    }

    public boolean getCanAscending() {
        return this.speedUpWhenYMotionIsBlocked;
    }

    public void setCanAscending(boolean value) {
        this.speedUpWhenYMotionIsBlocked = value;
    }

    public Vector3f getLastRotate() {
        return runtimeState.getPendingRotation();
    }

    public void setLastRotate(Vector3f lastRotate) {
        this.runtimeState.setPendingRotation(lastRotate);
    }

    public boolean getUpdateRotate() {
        return runtimeState.isRotationPending();
    }

    public void setUpdateRotate(boolean updateRotate) {
        this.runtimeState.setRotationPending(updateRotate);
    }

    public void teleportTo(Vec3 pos) {
        this.runtimeState.scheduleTeleport(pos);
    }

    public void teleportTo(double x, double y, double z) {
        this.runtimeState.scheduleTeleport(new Vec3(x, y, z));
    }

    public void rotateParticleTo(RelativeLocation target) {
        rotateParticleTo(new Vector3f((float) target.getX(), (float) target.getY(), (float) target.getZ()));
    }

    public void rotateParticleTo(Vec3 target) {
        rotateParticleTo(target.toVector3f());
    }

    public void rotateParticleTo(Vector3f target) {
        float[] angles = Math3DUtil.calculateEulerAnglesToPointArray(target);
        this.runtimeState.scheduleRotation(new Vector3f(angles[0], angles[1], angles[2]));
    }

    public void colorOfRGB(int r, int g, int b) {
        setColor(Math3DUtil.colorOf(
                Mth.clamp(r, 0, 255),
                Mth.clamp(g, 0, 255),
                Mth.clamp(b, 0, 255)));
    }

    public void colorOfRGBA(int rgba) {
        int a = (rgba >> 24) & 0xFF;
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = rgba & 0xFF;
        colorOfRGBA(r, g, b, (float) a / 255.0f);
    }

    public void colorOfRGBA(int r, int g, int b, float alpha) {
        setColor(Math3DUtil.colorOf(
                Mth.clamp(r, 0, 255),
                Mth.clamp(g, 0, 255),
                Mth.clamp(b, 0, 255)));
        this.alpha = Mth.clamp(alpha, 0.0f, 1.0f);
    }

    public void moveToWithPhysics(Vec3 pos) {
        Vec3 rel = pos.subtract(getLoc());
        BlockHitResult res = PhysicsUtil.collide(getLoc(), rel, (Level) getClientWorld());
        Vec3 actualPos = res.getType() != HitResult.Type.MISS
                ? PhysicsUtil.fixBeforeCollidePosition(res) : pos;
        teleportTo(actualPos);
    }

    public void moveToWithPhysics(Vec3 pos, BlockHitResult collideResult) {
        Vec3 actualPos = collideResult.getType() != HitResult.Type.MISS
                ? PhysicsUtil.fixBeforeCollidePosition(collideResult) : pos;
        teleportTo(actualPos);
    }

    public void moveToWithPhysics(double x, double y, double z, BlockHitResult collideResult) {
        moveToWithPhysics(new Vec3(x, y, z), collideResult);
    }

    public void moveToWithPhysics(double x, double y, double z) {
        moveToWithPhysics(new Vec3(x, y, z));
    }

    public ControllableParticle setInterpolator(ParticleLerpInterpolator newInterpolator) {
        this.interpolator = newInterpolator;
        return this;
    }

    @Override
    public void tick() {
        if (this.age > this.lifetime) {
            this.age = this.lifetime;
        }
        if (this.minecraftTick) {
            super.tick();
        }
        this.controllerBridge.tick();

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        Vec3 pendingTeleport = this.runtimeState.consumePendingTeleport();
        if (pendingTeleport != null) {
            if (!this.minecraftTick) {
                AABB bb = getBoundingBox();
                setBoundingBox(AABB.ofSize(getLoc(),
                        bb.maxX - bb.minX,
                        bb.maxY - bb.minY,
                        bb.maxZ - bb.minZ));
            }
            setLoc(pendingTeleport);
        }

        this.runtimeState.setCurrentRoll(this.roll);
        this.runtimeState.capturePreviousRotation();
        this.oRoll = this.runtimeState.getPreviewRoll();
        if (this.runtimeState.applyPendingRotation()) {
            this.roll = this.runtimeState.getCurrentRoll();
        }
    }

    @Override
    public void remove() {
        super.remove();
        this.controllerBridge.release();
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        ControllableParticleRenderHelper.render(this, vertexConsumer, camera, tickDelta);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return this.textureSheet;
    }

    @Override
    protected int getLightColor(float partialTick) {
        if (this.light == -1) {
            return LevelRenderer.getLightColor(this.level, BlockPos.containing(getLoc()));
        }
        return LightTexture.pack(this.light, this.light);
    }

    Vec3 previousPos() {
        return new Vec3(this.xo, this.yo, this.zo);
    }

    Vec3 currentPos() {
        return getLoc();
    }

    float previewPitchValue() {
        return runtimeState.getPreviewPitch();
    }

    float currentPitchValue() {
        return runtimeState.getCurrentPitch();
    }

    float previewYawValue() {
        return runtimeState.getPreviewYaw();
    }

    float currentYawValue() {
        return runtimeState.getCurrentYaw();
    }

    float previewRollValue() {
        return runtimeState.getPreviewRoll();
    }

    float currentRollValue() {
        return runtimeState.getCurrentRoll();
    }

    float quadSizeAt(float tickDelta) {
        return getQuadSize(tickDelta);
    }

    float minU() {
        return getU0();
    }

    float maxU() {
        return getU1();
    }

    float minV() {
        return getV0();
    }

    float maxV() {
        return getV1();
    }

    float redChannel() {
        return this.rCol;
    }

    float greenChannel() {
        return this.gCol;
    }

    float blueChannel() {
        return this.bCol;
    }

    float alphaChannel() {
        return this.alpha;
    }

    int lightColorAt(float tickDelta) {
        return getLightColor(tickDelta);
    }
}
