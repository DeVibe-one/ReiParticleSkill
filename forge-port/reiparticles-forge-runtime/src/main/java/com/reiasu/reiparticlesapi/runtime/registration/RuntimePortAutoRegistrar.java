// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.runtime.registration;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleProvider;
import com.reiasu.reiparticlesapi.reflect.ReiAPIScanner;
import com.reiasu.reiparticlesapi.renderer.RenderEntity;
import com.reiasu.reiparticlesapi.renderer.client.ClientRenderEntityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Scans for classes annotated with {@link ReiAutoRegister} and automatically
 * registers emitter codecs and style providers, removing the need for manual
 * registration in {@code ReiParticleSkillForge.registerRuntimePorts()}.
 * <p>
 * Conventions:
 * <ul>
 *   <li>Emitters: must have {@code public static final ResourceLocation CODEC_ID}
 *       and {@code public static T decode(FriendlyByteBuf)}</li>
 *   <li>Styles: must have {@code public static final ResourceLocation REGISTRY_KEY}
 *       and an inner class named {@code Provider} that
 *       implements {@link ParticleStyleProvider}</li>
 *   <li>Render entities: must have {@code public static final ResourceLocation ID}
 *       (or {@code RENDER_ID}) and {@code public static T decode(FriendlyByteBuf)}</li>
 * </ul>
 */
public final class RuntimePortAutoRegistrar {

    private RuntimePortAutoRegistrar() {
    }

    @SuppressWarnings("unchecked")
    public static void registerAll(Logger logger, String... packages) {
        for (String pkg : packages) {
            ReiAPIScanner.registerPackage(pkg);
        }
        ReiAPIScanner.INSTANCE.scan();
        registerDiscoveredClasses(logger, ReiAPIScanner.INSTANCE.getClassesWithAnnotation(ReiAutoRegister.class));
    }

    static void registerDiscoveredClasses(Logger logger, Collection<Class<?>> annotated) {
        int emitters = 0;
        int styles = 0;
        int renderEntities = 0;

        for (Class<?> clazz : annotated.stream().sorted(Comparator.comparing(Class::getName)).toList()) {
            if (ParticleEmitters.class.isAssignableFrom(clazz)) {
                ResourceLocation codecId = getStaticResourceLocationField(clazz, "CODEC_ID");
                if (codecId == null) {
                    logger.warn("Emitter {} has @ReiAutoRegister but no CODEC_ID field", clazz.getName());
                    continue;
                }
                Function<FriendlyByteBuf, ParticleEmitters> decoder = findEmitterDecoder(clazz);
                if (decoder == null) {
                    logger.warn("Emitter {} has @ReiAutoRegister but no decode(FriendlyByteBuf) method", clazz.getName());
                    continue;
                }
                ParticleEmittersManager.registerCodec(codecId, decoder);
                emitters++;
                continue;
            }

            if (ParticleGroupStyle.class.isAssignableFrom(clazz)) {
                ResourceLocation registryKey = getStaticResourceLocationField(clazz, "REGISTRY_KEY");
                if (registryKey == null) {
                    logger.warn("Style {} has @ReiAutoRegister but no REGISTRY_KEY field", clazz.getName());
                    continue;
                }
                ParticleStyleProvider<?> provider = findProvider(clazz, logger);
                if (provider == null) {
                    logger.warn("Style {} has @ReiAutoRegister but no usable Provider inner class", clazz.getName());
                    continue;
                }
                ParticleStyleManager.register(registryKey, provider);
                styles++;
                continue;
            }

            if (RenderEntity.class.isAssignableFrom(clazz)) {
                ResourceLocation renderId = getStaticResourceLocationField(clazz, "ID");
                if (renderId == null) {
                    renderId = getStaticResourceLocationField(clazz, "RENDER_ID");
                }
                if (renderId == null) {
                    logger.warn("Render entity {} has @ReiAutoRegister but no ID/RENDER_ID field", clazz.getName());
                    continue;
                }
                Function<FriendlyByteBuf, RenderEntity> decoder = findRenderEntityDecoder(clazz);
                if (decoder == null) {
                    logger.warn("Render entity {} has @ReiAutoRegister but no decode(FriendlyByteBuf) method", clazz.getName());
                    continue;
                }
                ClientRenderEntityManager.INSTANCE.registerCodec(renderId,
                        data -> FriendlyByteBufs.decodeFromByteArray(data, decoder));
                renderEntities++;
            }
        }

        logger.info("Auto-registered {} emitters, {} styles, {} render entities via @ReiAutoRegister",
                emitters, styles, renderEntities);
    }

    private static ResourceLocation getStaticResourceLocationField(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            if (Modifier.isStatic(f.getModifiers()) && ResourceLocation.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (ResourceLocation) f.get(null);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Function<FriendlyByteBuf, ParticleEmitters> findEmitterDecoder(Class<?> clazz) {
        try {
            Method m = clazz.getDeclaredMethod("decode", FriendlyByteBuf.class);
            if (Modifier.isStatic(m.getModifiers()) && ParticleEmitters.class.isAssignableFrom(m.getReturnType())) {
                m.setAccessible(true);
                return buf -> {
                    try {
                        return (ParticleEmitters) m.invoke(null, buf);
                    } catch (Exception e) {
                        throw new RuntimeException("decode() failed for " + clazz.getName(), e);
                    }
                };
            }
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Function<FriendlyByteBuf, RenderEntity> findRenderEntityDecoder(Class<?> clazz) {
        try {
            Method m = clazz.getDeclaredMethod("decode", FriendlyByteBuf.class);
            if (Modifier.isStatic(m.getModifiers()) && RenderEntity.class.isAssignableFrom(m.getReturnType())) {
                m.setAccessible(true);
                return buf -> {
                    try {
                        return (RenderEntity) m.invoke(null, buf);
                    } catch (Exception e) {
                        throw new RuntimeException("decode() failed for " + clazz.getName(), e);
                    }
                };
            }
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ParticleStyleProvider<?> findProvider(Class<?> clazz, Logger logger) {
        for (Class<?> inner : clazz.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("Provider")
                    && ParticleStyleProvider.class.isAssignableFrom(inner)) {
                try {
                    return (ParticleStyleProvider<?>) inner.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    logger.warn("Failed to instantiate Provider in {} via @ReiAutoRegister", clazz.getName(), e);
                }
            }
        }
        return null;
    }
}
