// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill;

import com.reiasu.reiparticlesapi.ReiParticlesAPI;
import com.reiasu.reiparticlesapi.runtime.registration.RuntimePortAutoRegistrar;
import com.reiasu.reiparticleskill.command.ReiParticleSkillDebugCommand;
import com.reiasu.reiparticleskill.command.SkillActionCommand;
import com.reiasu.reiparticleskill.command.port.APITestCommandPort;
import com.reiasu.reiparticleskill.command.port.DisplayCommandPort;
import com.reiasu.reiparticleskill.command.port.RailgunCommandPort;
import com.reiasu.reiparticleskill.config.SkillClientConfig;
import com.reiasu.reiparticleskill.display.SkillDisplayTypes;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnStateBridge;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnWatcher;
import com.reiasu.reiparticleskill.listener.KeyListener;
import com.reiasu.reiparticleskill.listener.ServerListener;
import com.reiasu.reiparticleskill.registry.ForgeSkillEnchantments;
import com.reiasu.reiparticleskill.registry.ForgeSkillEntityTypes;
import com.reiasu.reiparticleskill.registry.ForgeSkillSoundEvents;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.lang.reflect.Method;

@Mod(ReiParticleSkillConstants.MOD_ID)
public final class ReiParticleSkillForge {
    public static final String MOD_ID = ReiParticleSkillConstants.MOD_ID;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final EndRespawnStateBridge endRespawnBridge = new EndRespawnStateBridge();

    public ReiParticleSkillForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ForgeSkillEntityTypes.register(modBus);
        ForgeSkillEnchantments.register(modBus);
        ForgeSkillSoundEvents.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SkillClientConfig.SPEC);

        modBus.addListener((FMLClientSetupEvent event) -> onClientSetup());

        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                onRegisterCommands(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
                EndRespawnWatcher.tickServer(event.getServer(), endRespawnBridge, LOGGER);
                ServerListener.onServerPostTick(event.getServer());
            }
        });

        ReiParticlesAPI.init();
        ReiParticlesAPI.INSTANCE.loadScannerPackages();
        registerApiListeners();
        registerDisplayTypes();
        registerRuntimePorts();
        registerTests();
        ReiParticlesAPI.INSTANCE.registerTest();

        LOGGER.info("ReiParticleSkill Forge runtime initialized");
    }

    private void onClientSetup() {
        LOGGER.info("ReiParticleSkill client setup completed");
    }

    private void onRegisterCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        ReiParticleSkillDebugCommand.register(dispatcher, endRespawnBridge, LOGGER);
        SkillActionCommand.register(dispatcher);
        DisplayCommandPort.register(dispatcher);
        RailgunCommandPort.register(dispatcher);
        APITestCommandPort.register(dispatcher);
        LOGGER.info("Registered reiparticleskill debug commands");
    }

    private void registerApiListeners() {
        try {
            ReiParticlesAPI.INSTANCE.registerEventListener(MOD_ID, new KeyListener());
            LOGGER.info("Registered ReiParticleSkill API listeners");
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to register ReiParticleSkill API listeners", e);
        }
    }

    private void registerTests() {
        try {
            Class<?> builderClass = Class.forName("com.reiasu.reiparticleskill.test.SkillTestBuilder");
            Method register = builderClass.getMethod("register");
            register.invoke(null);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Skill test builder not present on runtime classpath; skipping test registration");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to register optional SkillTestBuilder", e);
        }
    }

    private void registerDisplayTypes() {
        SkillDisplayTypes.register();
    }

    private void registerRuntimePorts() {
        try {
            RuntimePortAutoRegistrar.registerAll(LOGGER,
                    "com.reiasu.reiparticleskill.particles",
                    "com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client");
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to auto-register ReiParticleSkill runtime ports", e);
        }
    }
}
