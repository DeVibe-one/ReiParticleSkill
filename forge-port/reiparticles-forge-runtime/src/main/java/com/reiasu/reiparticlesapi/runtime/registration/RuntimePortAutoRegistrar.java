// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.runtime.registration;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleProvider;
import com.reiasu.reiparticlesapi.reflect.ReiAPIScanner;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
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
 * </ul>
 */
public final class RuntimePortAutoRegistrar {

    private RuntimePortAutoRegistrar() {
    }

    @SuppressWarnings("unchecked")
    public static void registerAll(Logger logger, String... packages) {
        int emitters = 0;
        int styles = 0;

        // Register packages for scanning, then scan
        for (String pkg : packages) {
            ReiAPIScanner.registerPackage(pkg);
        }
        ReiAPIScanner.INSTANCE.scan();

        Collection<Class<?>> annotated = ReiAPIScanner.INSTANCE.getClassesWithAnnotation(ReiAutoRegister.class);

        for (Class<?> clazz : annotated) {
            // --- Emitter registration ---
            if (ParticleEmitters.class.isAssignableFrom(clazz)) {
                ResourceLocation codecId = getStaticResourceLocationField(clazz, "CODEC_ID");
                if (codecId == null) {
                    logger.warn("Emitter {} has @ReiAutoRegister but no CODEC_ID field", clazz.getName());
                    continue;
                }
                Function<FriendlyByteBuf, ParticleEmitters> decoder = findDecoder(clazz);
                if (decoder == null) {
                    logger.warn("Emitter {} has @ReiAutoRegister but no decode(FriendlyByteBuf) method", clazz.getName());
                    continue;
                }
                ParticleEmittersManager.registerCodec(codecId, decoder);
                emitters++;
                continue;
            }

            // --- Style registration ---
            if (ParticleGroupStyle.class.isAssignableFrom(clazz)) {
                ResourceLocation registryKey = getStaticResourceLocationField(clazz, "REGISTRY_KEY");
                if (registryKey == null) {
                    logger.warn("Style {} has @ReiAutoRegister but no REGISTRY_KEY field", clazz.getName());
                    continue;
                }
                ParticleStyleProvider<?> provider = findProvider(clazz);
                if (provider == null) {
                    logger.warn("Style {} has @ReiAutoRegister but no Provider inner class", clazz.getName());
                    continue;
                }
                ParticleStyleManager.register(registryKey, provider);
                styles++;
            }
        }

        logger.info("Auto-registered {} emitters, {} styles via @ReiAutoRegister", emitters, styles);
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
    private static Function<FriendlyByteBuf, ParticleEmitters> findDecoder(Class<?> clazz) {
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
    private static ParticleStyleProvider<?> findProvider(Class<?> clazz) {
        for (Class<?> inner : clazz.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("Provider")
                    && ParticleStyleProvider.class.isAssignableFrom(inner)) {
                try {
                    return (ParticleStyleProvider<?>) inner.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    // Log so that Provider instantiation failures are visible during debugging
                    com.mojang.logging.LogUtils.getLogger().debug(
                            "Failed to instantiate Provider in {}: {}", clazz.getName(), e.getMessage());
                }
            }
        }
        return null;
    }
}

