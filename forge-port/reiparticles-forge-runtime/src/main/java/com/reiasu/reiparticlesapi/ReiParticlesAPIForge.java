// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.animation.AnimateManager;
import com.reiasu.reiparticlesapi.client.ClientTickEventForwarder;
import com.reiasu.reiparticlesapi.commands.APICommand;
import com.reiasu.reiparticlesapi.config.APIConfigSpec;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.event.ForgeEventForwarder;
import com.reiasu.reiparticlesapi.event.ReiEventBus;
import com.reiasu.reiparticlesapi.event.events.server.ServerPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.server.ServerPreTickEvent;
import com.reiasu.reiparticlesapi.network.ForgeReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.animation.PathMotionManager;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind.WindDirections;
import com.reiasu.reiparticlesapi.network.particle.emitters.type.EmittersShootTypes;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.particles.ControllableParticleEffectManager;
import com.reiasu.reiparticlesapi.particles.ForgeReiModParticles;
import com.reiasu.reiparticlesapi.particles.ReiModParticles;
import com.reiasu.reiparticlesapi.particles.control.group.ClientParticleGroupManager;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableCloudParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableEndRodParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableEnchantmentParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableFallingDustParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableFireworkParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableFlashParticle;
import com.reiasu.reiparticlesapi.particles.impl.particles.ControllableSplashParticle;
import com.reiasu.reiparticlesapi.renderer.client.ClientRenderEntityManager;
import com.reiasu.reiparticlesapi.renderer.server.ServerRenderEntityManager;
import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import com.reiasu.reiparticlesapi.test.TestManager;
import com.reiasu.reiparticlesapi.utils.ClientCameraUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(ReiParticlesAPIForge.MOD_ID)
public final class ReiParticlesAPIForge {
    public static final String MOD_ID = ReiParticlesConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();

    public ReiParticlesAPIForge() {
        registerConfig();
        registerTickCallbacks();
        registerCommands();
        initSystems();

        LOGGER.info("ReiParticlesAPI Forge runtime initialized");
    }

    private void registerConfig() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((ModConfigEvent.Loading event) -> applyConfig(event.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading event) -> applyConfig(event.getConfig()));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, APIConfigSpec.SPEC);
        APIConfigSpec.apply();
    }

    private void applyConfig(ModConfig config) {
        if (APIConfigSpec.owns(config)) {
            APIConfigSpec.apply();
        }
    }

    private void registerTickCallbacks() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((FMLClientSetupEvent event) -> onClientSetup());
        ForgeReiModParticles.register(modBus);
        modBus.addListener(this::onRegisterParticleProviders);

        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientTickEventForwarder::onClientStartTick);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                onClientEndTick();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START && event.getServer() != null) {
                ReiEventBus.call(new ServerPreTickEvent(event.getServer()));
            }
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
                onServerEndTick(event.getServer());
            }
        });
    }

    private void registerCommands() {
        MinecraftForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> APICommand.INSTANCE.register(event.getDispatcher())
        );
    }

    private void initSystems() {
        ForgeReiParticlesNetwork.init();
        ParticleEmittersManager.registerBuiltinCodecs();
        DisplayEntityManager.INSTANCE.registerBuiltinTypes();
        EmittersShootTypes.INSTANCE.init();
        WindDirections.INSTANCE.init();
        ControllableParticleEffectManager.INSTANCE.init();
        ReiParticlesAPI.init();
        ForgeEventForwarder.init();
        ReiParticlesAPI.INSTANCE.loadScannerPackages();
        ReiParticlesAPI.INSTANCE.registerTest();
    }

    private void onClientEndTick() {
        safeTick("ClientTickEventForwarder", () ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientTickEventForwarder::onClientEndTick));
        safeTick("AnimateManager.client", () -> AnimateManager.INSTANCE.tickClient());
        safeTick("ParticleEmittersManager.client", ParticleEmittersManager::tickClient);
        safeTick("DisplayEntityManager.client", () -> DisplayEntityManager.INSTANCE.tickClient());
        safeTick("ParticleCompositionManager.client", () -> ParticleCompositionManager.INSTANCE.tickClient());
        safeTick("ParticleStyleManager.client", ParticleStyleManager::doTickClient);
        safeTick("ClientParticleGroupManager", () -> ClientParticleGroupManager.INSTANCE.doClientTick());
        safeTick("ClientRenderEntityManager", () -> ClientRenderEntityManager.INSTANCE.doClientTick());
        safeTick("PathMotionManager", () -> PathMotionManager.INSTANCE.tick());
        safeTick("ReiScheduler.client", () -> ReiScheduler.INSTANCE.doClientTick());
        safeTick("ClientCameraUtil", () -> ClientCameraUtil.INSTANCE.tick());
    }

    private void onServerEndTick(net.minecraft.server.MinecraftServer server) {
        safeTick("AnimateManager.server", () -> AnimateManager.INSTANCE.tickServer());
        safeTick("ParticleEmittersManager.server", ParticleEmittersManager::tickAll);
        safeTick("DisplayEntityManager.server", () -> DisplayEntityManager.INSTANCE.tickAll());
        safeTick("ParticleCompositionManager.server", () -> ParticleCompositionManager.INSTANCE.tickAll());
        safeTick("ParticleStyleManager.server", ParticleStyleManager::doTickServer);
        safeTick("ServerRenderEntityManager.tick", () -> ServerRenderEntityManager.INSTANCE.tick());
        safeTick("ServerRenderEntityManager.upgrade", () -> ServerRenderEntityManager.INSTANCE.upgrade(server));
        safeTick("TestManager", () -> TestManager.INSTANCE.doTickServer());
        safeTick("ReiScheduler.server", () -> ReiScheduler.INSTANCE.doServerTick());
        safeTick("ServerPostTickEvent", () -> ReiEventBus.call(new ServerPostTickEvent(server)));
    }

    private static final Map<String, Long> LAST_ERROR_LOG = new ConcurrentHashMap<>();
    private static final long ERROR_LOG_INTERVAL_MS = 10_000L;

    private static void safeTick(String name, Runnable tick) {
        try {
            tick.run();
        } catch (Exception e) {
            long now = System.currentTimeMillis();
            Long last = LAST_ERROR_LOG.get(name);
            if (last == null || now - last >= ERROR_LOG_INTERVAL_MS) {
                LAST_ERROR_LOG.put(name, now);
                LOGGER.warn("Tick handler '{}' threw (throttled to once per {}s):",
                        name, ERROR_LOG_INTERVAL_MS / 1000, e);
            }
        }
    }

    private void onClientSetup() {
        LOGGER.info("ReiParticlesAPI client setup completed");
    }

    private void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_END_ROD.get(), ControllableEndRodParticle.Factory::new);
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_ENCHANTMENT.get(), ControllableEnchantmentParticle.Factory::new);
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_CLOUD.get(), ControllableCloudParticle.Factory::new);
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_FLASH.get(), ControllableFlashParticle.Factory::new);
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_FIREWORK.get(), ControllableFireworkParticle.Factory::new);
        event.registerSpecial(ReiModParticles.CONTROLLABLE_FALLING_DUST.get(), new ControllableFallingDustParticle.Factory());
        event.registerSpriteSet(ReiModParticles.CONTROLLABLE_SPLASH.get(), ControllableSplashParticle.Factory::new);
        LOGGER.info("Registered ReiParticlesAPI particle providers (7 types)");
    }
}
