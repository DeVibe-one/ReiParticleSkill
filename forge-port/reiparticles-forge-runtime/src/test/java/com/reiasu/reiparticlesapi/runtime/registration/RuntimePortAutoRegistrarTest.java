// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.runtime.registration;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.EmitterRegistry;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleProvider;
import com.reiasu.reiparticlesapi.network.particle.style.StyleRegistry;
import com.reiasu.reiparticlesapi.reflect.ReiAPIScanner;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import com.reiasu.reiparticlesapi.renderer.client.ClientRenderEntityManager;
import com.reiasu.reiparticlesapi.testutil.RecordingLogger;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimePortAutoRegistrarTest {
    @AfterEach
    void cleanup() {
        ReiAPIScanner.INSTANCE.clear();
        ClientRenderEntityManager.INSTANCE.clear();
        SuccessRenderEntity.lastDecodeBuffer = null;
    }

    @Test
    void shouldRegisterAnnotatedRuntimePorts() {
        RecordingLogger recorder = new RecordingLogger();

        RuntimePortAutoRegistrar.registerAll(recorder.logger(), "com.reiasu.reiparticlesapi.runtime.registration");

        assertNotNull(ParticleEmittersManager.getCodecFromID(SuccessEmitter.CODEC_ID));
        assertNotNull(ParticleStyleManager.getProvider(SuccessStyle.REGISTRY_KEY));
        assertNotNull(ClientRenderEntityManager.INSTANCE.getCodecFromID(SuccessRenderEntity.ID));
        ClientRenderEntityManager.INSTANCE.getCodecFromID(SuccessRenderEntity.ID).apply(new byte[]{1, 2, 3});
        assertNotNull(SuccessRenderEntity.lastDecodeBuffer);
        assertEquals(0, SuccessRenderEntity.lastDecodeBuffer.refCnt());
        assertTrue(recorder.hasEvent("info", "Auto-registered"));
    }

    @Test
    void shouldWarnWhenProviderInstantiationFails() {
        RecordingLogger recorder = new RecordingLogger();

        RuntimePortAutoRegistrar.registerAll(recorder.logger(), "com.reiasu.reiparticlesapi.runtime.registration");

        assertTrue(recorder.hasEvent("warn", "Failed to instantiate Provider"));
        assertTrue(recorder.hasEvent("warn", "no usable Provider inner class"));
    }

    @Test
    void shouldRegisterEmittersAndStylesInStableNameOrder() {
        RecordingLogger recorder = new RecordingLogger();

        RuntimePortAutoRegistrar.registerDiscoveredClasses(recorder.logger(), List.of(
                OrderStyleZulu.class,
                OrderEmitterZulu.class,
                OrderStyleAlpha.class,
                OrderEmitterAlpha.class
        ));

        assertTrue(EmitterRegistry.INSTANCE.getId(OrderEmitterAlpha.CODEC_ID)
                < EmitterRegistry.INSTANCE.getId(OrderEmitterZulu.CODEC_ID));
        assertTrue(StyleRegistry.INSTANCE.getId(OrderStyleAlpha.REGISTRY_KEY)
                < StyleRegistry.INSTANCE.getId(OrderStyleZulu.REGISTRY_KEY));
    }

    @ReiAutoRegister
    public static final class SuccessEmitter extends ParticleEmitters {
        public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticlesruntime", "success_emitter_test");

        public static SuccessEmitter decode(FriendlyByteBuf buf) {
            SuccessEmitter emitter = new SuccessEmitter();
            emitter.decodeFromBuffer(buf);
            return emitter;
        }
    }

    @ReiAutoRegister
    public static final class SuccessStyle extends ParticleGroupStyle {
        public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticlesruntime", "success_style_test");

        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        public static final class Provider implements ParticleStyleProvider<SuccessStyle> {
            @Override
            public SuccessStyle create() {
                return new SuccessStyle();
            }
        }
    }

    @ReiAutoRegister
    public static final class SuccessRenderEntity extends RenderEntity {
        public static final ResourceLocation ID = new ResourceLocation("reiparticlesruntime", "success_render_entity_test");
        private static FriendlyByteBuf lastDecodeBuffer;

        public static SuccessRenderEntity decode(FriendlyByteBuf buf) {
            lastDecodeBuffer = buf;
            return new SuccessRenderEntity();
        }

        @Override
        public void clientTick() {
        }

        @Override
        public void serverTick() {
        }

        @Override
        public ResourceLocation getRenderID() {
            return ID;
        }
    }

    @ReiAutoRegister
    public static final class BrokenStyle extends ParticleGroupStyle {
        public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticlesruntime", "broken_style_test");

        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        public static final class Provider implements ParticleStyleProvider<BrokenStyle> {
            public Provider() {
                throw new IllegalStateException("boom");
            }

            @Override
            public BrokenStyle create() {
                return new BrokenStyle();
            }
        }
    }

    @ReiAutoRegister
    public static final class OrderEmitterAlpha extends ParticleEmitters {
        public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticlesruntime", "order_emitter_alpha");

        public static OrderEmitterAlpha decode(FriendlyByteBuf buf) {
            return new OrderEmitterAlpha();
        }
    }

    @ReiAutoRegister
    public static final class OrderEmitterZulu extends ParticleEmitters {
        public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticlesruntime", "order_emitter_zulu");

        public static OrderEmitterZulu decode(FriendlyByteBuf buf) {
            return new OrderEmitterZulu();
        }
    }

    @ReiAutoRegister
    public static final class OrderStyleAlpha extends ParticleGroupStyle {
        public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticlesruntime", "order_style_alpha");

        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        public static final class Provider implements ParticleStyleProvider<OrderStyleAlpha> {
            @Override
            public OrderStyleAlpha create() {
                return new OrderStyleAlpha();
            }
        }
    }

    @ReiAutoRegister
    public static final class OrderStyleZulu extends ParticleGroupStyle {
        public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticlesruntime", "order_style_zulu");

        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        public static final class Provider implements ParticleStyleProvider<OrderStyleZulu> {
            @Override
            public OrderStyleZulu create() {
                return new OrderStyleZulu();
            }
        }
    }
}
