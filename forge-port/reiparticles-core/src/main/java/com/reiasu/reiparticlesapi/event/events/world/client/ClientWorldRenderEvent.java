// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.world.client;

import com.reiasu.reiparticlesapi.event.events.world.WorldEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Consumer;

public final class ClientWorldRenderEvent extends WorldEvent {
    public enum RenderStage {
        AFTER_ENTITY
    }

    private final RenderStage stage;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectMatrix;
    private final PoseStack poseStack;
    private final MultiBufferSource buffer;
    private final LevelRenderer worldRenderer;
    private final Camera camera;
    private final float partialTick;

    public ClientWorldRenderEvent(
            ClientLevel world,
            RenderStage stage,
            Matrix4f viewMatrix,
            Matrix4f projectMatrix,
            PoseStack poseStack,
            MultiBufferSource buffer,
            LevelRenderer worldRenderer,
            Camera camera,
            float partialTick
    ) {
        super(world);
        this.stage = stage;
        this.viewMatrix = viewMatrix;
        this.projectMatrix = projectMatrix;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.worldRenderer = worldRenderer;
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public RenderStage getStage() {
        return stage;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjectMatrix() {
        return projectMatrix;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getBuffer() {
        return buffer;
    }

    public LevelRenderer getWorldRenderer() {
        return worldRenderer;
    }

    public Camera getCamera() {
        return camera;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public void transformTo(Vec3 pos, Consumer<PoseStack> invoker) {
        Vec3 cameraPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
        try {
            invoker.accept(poseStack);
        } finally {
            poseStack.popPose();
        }
    }
}

