// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.ReiParticlesConstants;
import com.reiasu.reiparticlesapi.client.CameraShakeClientState;
import com.reiasu.reiparticlesapi.network.packet.CameraShakeS2CPacket;
import com.reiasu.reiparticlesapi.network.packet.PacketCameraShakeS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketDisplayEntityS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketKeyActionC2S;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleEmittersS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleStyleS2C;
import com.reiasu.reiparticlesapi.network.packet.PacketRenderEntityS2C;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientCameraShakeHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientDisplayEntityPacketHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientParticleCompositionHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientParticleEmittersPacketHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientParticleGroupPacketHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientParticlePacketHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientParticleStylePacketHandler;
import com.reiasu.reiparticlesapi.network.packet.client.listener.ClientRenderEntityPacketHandler;
import com.reiasu.reiparticlesapi.network.packet.server.listener.ServerKeyActionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ForgeReiParticlesNetwork {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PROTOCOL_VERSION = 1;
    private static int packetId = 0;
    private static boolean initialized;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ReiParticlesConstants.MOD_ID, "main"))
            .networkProtocolVersion(() -> Integer.toString(PROTOCOL_VERSION))
            .clientAcceptedVersions(v -> true)
            .serverAcceptedVersions(v -> true)
            .simpleChannel();

    private ForgeReiParticlesNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ReiParticlesNetwork.bindSender(ForgeReiParticlesNetwork::sendTo);

        registerClientMessage(CameraShakeS2CPacket.class, CameraShakeS2CPacket::encode, CameraShakeS2CPacket::decode,
                CameraShakeClientState::start);
        registerClientMessage(PacketCameraShakeS2C.class, PacketCameraShakeS2C::encode, PacketCameraShakeS2C::decode,
                ClientCameraShakeHandler::receive);
        registerClientMessage(PacketParticleS2C.class, PacketParticleS2C::encode, PacketParticleS2C::decode,
                ClientParticlePacketHandler::receive);
        registerClientMessage(PacketParticleEmittersS2C.class, PacketParticleEmittersS2C::encode, PacketParticleEmittersS2C::decode,
                ClientParticleEmittersPacketHandler::receive);
        registerClientMessage(PacketParticleCompositionS2C.class, PacketParticleCompositionS2C::encode, PacketParticleCompositionS2C::decode,
                ClientParticleCompositionHandler::receive);
        registerClientMessage(PacketDisplayEntityS2C.class, PacketDisplayEntityS2C::encode, PacketDisplayEntityS2C::decode,
                ClientDisplayEntityPacketHandler::receive);
        registerClientMessage(PacketParticleStyleS2C.class, PacketParticleStyleS2C::encode, PacketParticleStyleS2C::decode,
                ClientParticleStylePacketHandler::receive);
        registerClientMessage(PacketParticleGroupS2C.class, PacketParticleGroupS2C::encode, PacketParticleGroupS2C::decode,
                ClientParticleGroupPacketHandler::receive);
        registerClientMessage(PacketRenderEntityS2C.class, PacketRenderEntityS2C::encode, PacketRenderEntityS2C::decode,
                ClientRenderEntityPacketHandler::receive);
        registerServerMessage(PacketKeyActionC2S.class, PacketKeyActionC2S::encode, PacketKeyActionC2S::decode,
                ServerKeyActionHandler::receive);
    }

    private static <T> void registerClientMessage(Class<T> type,
                                                  BiConsumer<T, FriendlyByteBuf> encoder,
                                                  Function<FriendlyByteBuf, T> decoder,
                                                  Consumer<T> handler) {
        CHANNEL.registerMessage(packetId++, type, encoder, decoder,
                (packet, contextSupplier) -> handleClient(packet, contextSupplier, handler));
    }

    private static <T> void registerServerMessage(Class<T> type,
                                                  BiConsumer<T, FriendlyByteBuf> encoder,
                                                  Function<FriendlyByteBuf, T> decoder,
                                                  BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.registerMessage(packetId++, type, encoder, decoder,
                (packet, contextSupplier) -> handleServer(packet, contextSupplier, handler));
    }

    private static <T> void handleClient(T packet,
                                         Supplier<NetworkEvent.Context> contextSupplier,
                                         Consumer<T> handler) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handler.accept(packet)));
        context.setPacketHandled(true);
    }

    private static <T> void handleServer(T packet,
                                         Supplier<NetworkEvent.Context> contextSupplier,
                                         BiConsumer<T, ServerPlayer> handler) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> handler.accept(packet, sender));
        }
        context.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        try {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Player {} lacks channel; packet {} dropped",
                    player.getName().getString(), packet.getClass().getSimpleName());
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to send packet {} to {}: {}",
                    packet.getClass().getSimpleName(), player.getName().getString(), e.getMessage());
        }
    }
}
