// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.mojang.brigadier.StringReader;
import com.reiasu.reiparticlesapi.ReiParticlesConstants;
import com.reiasu.reiparticlesapi.particles.impl.ControllableCloudEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableEnchantmentEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableEndRodEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFallingDustEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFireworkEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFlashEffect;
import com.reiasu.reiparticlesapi.particles.ControllableParticleEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableSplashEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.BiFunction;

public final class ForgeReiModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ReiParticlesConstants.MOD_ID);

    static {
        PARTICLES.register(ReiModParticles.CONTROLLABLE_END_ROD.path(), () -> createType(ControllableEndRodEffect::new));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_ENCHANTMENT.path(), () -> createType(ControllableEnchantmentEffect::new));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_CLOUD.path(), () -> createType(ControllableCloudEffect::new));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_FLASH.path(), () -> createType(ControllableFlashEffect::new));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_FIREWORK.path(), () -> createType(ControllableFireworkEffect::new));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_FALLING_DUST.path(),
                () -> createType((uuid, face) -> new ControllableFallingDustEffect(uuid, Blocks.SAND.defaultBlockState(), face)));
        PARTICLES.register(ReiModParticles.CONTROLLABLE_SPLASH.path(), () -> createType(ControllableSplashEffect::new));
    }

    private ForgeReiModParticles() {
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }

    @SuppressWarnings("deprecation")
    private static <T extends ControllableParticleEffect> ParticleType<T> createType(BiFunction<UUID, Boolean, T> factory) {
        ParticleOptions.Deserializer<T> deserializer = new ParticleOptions.Deserializer<>() {
            @Override
            public T fromCommand(ParticleType<T> type, StringReader reader) {
                return factory.apply(UUID.randomUUID(), false);
            }

            @Override
            public T fromNetwork(ParticleType<T> type, FriendlyByteBuf buf) {
                return factory.apply(buf.readUUID(), buf.readBoolean());
            }
        };
        return new ParticleType<T>(true, deserializer) {
            @Override
            public com.mojang.serialization.Codec<T> codec() {
                return com.mojang.serialization.Codec.unit(() -> factory.apply(UUID.randomUUID(), false));
            }
        };
    }
}

