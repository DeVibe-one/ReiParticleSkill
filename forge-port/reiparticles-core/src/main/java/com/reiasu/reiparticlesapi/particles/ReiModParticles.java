// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.reiasu.reiparticlesapi.ReiParticlesConstants;
import com.reiasu.reiparticlesapi.particles.impl.ControllableCloudEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableEnchantmentEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableEndRodEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFallingDustEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFireworkEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableFlashEffect;
import com.reiasu.reiparticlesapi.particles.impl.ControllableSplashEffect;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ReiModParticles {
    public static final ReiModParticles INSTANCE = new ReiModParticles();

    public static final ParticleTypeRef<ControllableEndRodEffect> CONTROLLABLE_END_ROD = particle("controllable_end_rod");
    public static final ParticleTypeRef<ControllableEnchantmentEffect> CONTROLLABLE_ENCHANTMENT = particle("controllable_enchantment");
    public static final ParticleTypeRef<ControllableCloudEffect> CONTROLLABLE_CLOUD = particle("controllable_cloud");
    public static final ParticleTypeRef<ControllableFlashEffect> CONTROLLABLE_FLASH = particle("controllable_flash");
    public static final ParticleTypeRef<ControllableFireworkEffect> CONTROLLABLE_FIREWORK = particle("controllable_firework");
    public static final ParticleTypeRef<ControllableFallingDustEffect> CONTROLLABLE_FALLING_DUST = particle("controllable_falling_dust");
    public static final ParticleTypeRef<ControllableSplashEffect> CONTROLLABLE_SPLASH = particle("controllable_splash");

    private ReiModParticles() {
    }

    private static <T extends ParticleOptions> ParticleTypeRef<T> particle(String path) {
        return new ParticleTypeRef<>(new ResourceLocation(ReiParticlesConstants.MOD_ID, path));
    }

    public void reg() {
        // Registration is provided by the runtime module.
    }

    public static final class ParticleTypeRef<T extends ParticleOptions> {
        private final ResourceLocation id;

        private ParticleTypeRef(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public String path() {
            return id.getPath();
        }

        public boolean isPresent() {
            return BuiltInRegistries.PARTICLE_TYPE.containsKey(id);
        }

        @SuppressWarnings("unchecked")
        public ParticleType<T> get() {
            return (ParticleType<T>) BuiltInRegistries.PARTICLE_TYPE.get(id);
        }
    }
}
