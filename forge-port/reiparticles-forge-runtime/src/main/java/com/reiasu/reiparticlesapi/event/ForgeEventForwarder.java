// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event;

import com.reiasu.reiparticlesapi.event.events.entity.EntityMoveEvent;
import com.reiasu.reiparticlesapi.event.events.world.client.ClientWorldChangeEvent;
import com.reiasu.reiparticlesapi.event.events.entity.EntityPostTickEvent;
import com.reiasu.reiparticlesapi.event.events.entity.EntityPreMoveEvent;
import com.reiasu.reiparticlesapi.event.events.entity.EntityPrePlaceBlockEvent;
import com.reiasu.reiparticlesapi.event.events.entity.EntityPreTickEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.PlayerBlockBreakEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.PlayerDisconnectEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.PlayerItemDestroyEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.PlayerLoggedInEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.ServerPlayerDeathEvent;
import com.reiasu.reiparticlesapi.event.events.entity.player.ServerPlayerRespawnEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

public final class ForgeEventForwarder {
    private static boolean initialized;

    private ForgeEventForwarder() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) ->
                ReiEventBus.call(new PlayerLoggedInEvent(event.getEntity())));

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) ->
                ReiEventBus.call(new PlayerDisconnectEvent(event.getEntity())));

        MinecraftForge.EVENT_BUS.addListener((PlayerDestroyItemEvent event) ->
                ReiEventBus.call(new PlayerItemDestroyEvent(event.getEntity(), event.getOriginal())));

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) ->
                ReiEventBus.call(new ServerPlayerRespawnEvent(event.getEntity(), event.getEntity().level())));

        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }
            ServerPlayerDeathEvent deathEvent = ReiEventBus.call(new ServerPlayerDeathEvent(player, player.level(), event.getSource()));
            if (deathEvent.isCancelled() && event.isCancelable()) {
                event.setCanceled(true);
            }
        });

        MinecraftForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            PlayerBlockBreakEvent breakEvent = ReiEventBus.call(
                    new PlayerBlockBreakEvent(player, player.level(), event.getPos(), event.getState())
            );
            if (breakEvent.isCancelled()) {
                event.setCanceled(true);
            }
        });

        MinecraftForge.EVENT_BUS.addListener((TickEvent.PlayerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.START) {
                EntityPreTickEvent preTickEvent = ReiEventBus.call(new EntityPreTickEvent(event.player));
                if (preTickEvent.isCancelled() && event.isCancelable()) {
                    event.setCanceled(true);
                }
                return;
            }
            ReiEventBus.call(new EntityPostTickEvent(event.player));
        });

        // Client/Server/World tick events are forwarded by ClientTickEventForwarder
        // and ReiParticlesAPIForge lifecycle hooks to avoid duplication.

        // Client world change (dimension switch / join)
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                ReiEventBus.call(new ClientWorldChangeEvent(mc.level));
            }
        });

        // Entity place block
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
            Entity entity = event.getEntity();
            if (entity == null) return;
            EntityPrePlaceBlockEvent placeEvent = ReiEventBus.call(
                    new EntityPrePlaceBlockEvent(entity, entity.level(), event.getPos(), null)
            );
            if (placeEvent.isCancelled() && event.isCancelable()) {
                event.setCanceled(true);
            }
        });

        // Entity movement approximation (via LivingTickEvent)
        MinecraftForge.EVENT_BUS.addListener((LivingEvent.LivingTickEvent event) -> {
            Entity entity = event.getEntity();
            Vec3 pos = entity.position();
            Vec3 delta = entity.getDeltaMovement();
            if (delta.lengthSqr() < 1e-10) return;
            Vec3 target = pos.add(delta);
            EntityPreMoveEvent preMoveEvent = ReiEventBus.call(new EntityPreMoveEvent(entity, delta));
            if (preMoveEvent.isCancelled()) return;
            ReiEventBus.call(new EntityMoveEvent(entity, delta, target));
        });
    }
}

