// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class ControllableParticleRenderHelper {
    private ControllableParticleRenderHelper() {
    }

    static void render(ControllableParticle particle, VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Quaternionf rotation = resolveQuaternion(
                particle.getFaceToCamera(),
                camera.rotation(),
                tickDelta,
                particle.previewPitchValue(),
                particle.currentPitchValue(),
                particle.previewYawValue(),
                particle.currentYawValue(),
                particle.previewRollValue(),
                particle.currentRollValue());
        Vector3f renderPos = resolveRenderPos(
                particle.getInterpolator(),
                particle.previousPos(),
                particle.currentPos(),
                camera.getPosition(),
                tickDelta);
        int light = particle.lightColorAt(tickDelta);

        if (particle.getFaceToCamera()) {
            renderQuad(particle, vertexConsumer, rotation, renderPos.x, renderPos.y, renderPos.z, tickDelta, light);
            return;
        }
        renderDoubleSidedQuad(particle, vertexConsumer, rotation, renderPos.x, renderPos.y, renderPos.z, tickDelta, light);
    }

    static Vector3f resolveRenderPos(ParticleLerpInterpolator interpolator,
                                     Vec3 previousPos,
                                     Vec3 currentPos,
                                     Vec3 cameraPos,
                                     float tickDelta) {
        Vec3 lerpedPos = interpolator.consume(previousPos, currentPos, tickDelta);
        return lerpedPos.subtract(cameraPos).toVector3f();
    }

    static Quaternionf resolveQuaternion(boolean faceToCamera,
                                         Quaternionf cameraRotation,
                                         float tickDelta,
                                         float previewPitch,
                                         float currentPitch,
                                         float previewYaw,
                                         float currentYaw,
                                         float previewRoll,
                                         float currentRoll) {
        Quaternionf rotation = new Quaternionf();
        if (faceToCamera) {
            rotation.set(cameraRotation);
            if (currentRoll != 0.0f) {
                rotation.rotateZ(Mth.lerp(tickDelta, previewRoll, currentRoll));
            }
            return rotation;
        }
        return rotation.rotateXYZ(
                Mth.lerp(tickDelta, previewPitch, currentPitch),
                Mth.lerp(tickDelta, previewYaw, currentYaw),
                Mth.lerp(tickDelta, previewRoll, currentRoll));
    }

    private static void renderQuad(ControllableParticle particle,
                                   VertexConsumer consumer,
                                   Quaternionf rotation,
                                   float x,
                                   float y,
                                   float z,
                                   float tickDelta,
                                   int light) {
        float size = particle.quadSizeAt(tickDelta);
        float u0 = particle.minU();
        float u1 = particle.maxU();
        float v0 = particle.minV();
        float v1 = particle.maxV();

        addVertex(particle, consumer, rotation, x, y, z, 1.0f, -1.0f, u1, v1, size, light);
        addVertex(particle, consumer, rotation, x, y, z, 1.0f, 1.0f, u1, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, -1.0f, 1.0f, u0, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, -1.0f, -1.0f, u0, v1, size, light);
    }

    private static void renderDoubleSidedQuad(ControllableParticle particle,
                                              VertexConsumer consumer,
                                              Quaternionf rotation,
                                              float x,
                                              float y,
                                              float z,
                                              float tickDelta,
                                              int light) {
        float size = particle.quadSizeAt(tickDelta);
        float u0 = particle.minU();
        float u1 = particle.maxU();
        float v0 = particle.minV();
        float v1 = particle.maxV();

        addVertex(particle, consumer, rotation, x, y, z, 1.0f, -1.0f, u1, v1, size, light);
        addVertex(particle, consumer, rotation, x, y, z, 1.0f, 1.0f, u1, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, -1.0f, 1.0f, u0, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, -1.0f, -1.0f, u0, v1, size, light);

        addVertex(particle, consumer, rotation, x, y, z, -1.0f, -1.0f, u0, v1, size, light);
        addVertex(particle, consumer, rotation, x, y, z, -1.0f, 1.0f, u0, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, 1.0f, 1.0f, u1, v0, size, light);
        addVertex(particle, consumer, rotation, x, y, z, 1.0f, -1.0f, u1, v1, size, light);
    }

    private static void addVertex(ControllableParticle particle,
                                  VertexConsumer consumer,
                                  Quaternionf rotation,
                                  float dx,
                                  float dy,
                                  float dz,
                                  float vx,
                                  float vy,
                                  float tu,
                                  float tv,
                                  float size,
                                  int light) {
        Vector3f pos = new Vector3f(vx, vy, 0.0f).rotate(rotation).mul(size).add(dx, dy, dz);
        consumer.vertex(pos.x, pos.y, pos.z)
                .uv(tu, tv)
                .color(
                        particle.redChannel(),
                        particle.greenChannel(),
                        particle.blueChannel(),
                        particle.alphaChannel())
                .uv2(light)
                .endVertex();
    }
}
