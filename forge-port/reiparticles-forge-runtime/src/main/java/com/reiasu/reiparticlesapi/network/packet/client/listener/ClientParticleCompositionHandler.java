// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.packet.client.listener;

import com.reiasu.reiparticlesapi.network.buffer.FriendlyByteBufs;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleCompositionS2C;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.SequencedParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public final class ClientParticleCompositionHandler {
    private ClientParticleCompositionHandler() {
    }

    public static void receive(PacketParticleCompositionS2C packet) {
        Minecraft minecraft = Minecraft.getInstance();
        receive(packet, minecraft.level);
    }

    static void receive(PacketParticleCompositionS2C packet, Level clientLevel) {
        ParticleComposition old = ParticleCompositionManager.INSTANCE.getClientView().get(packet.getUuid());
        if (packet.getDistanceRemove()) {
            if (old != null) {
                old.remove();
            }
            return;
        }
        if (clientLevel == null) {
            return;
        }

        Function<FriendlyByteBuf, ParticleComposition> decoder =
                ParticleCompositionManager.INSTANCE.getRegisteredTypes().get(packet.getType());
        if (decoder == null) {
            return;
        }
        ParticleComposition decoded = FriendlyByteBufs.decodeFromByteArray(packet.getData(), decoder);
        if (decoded == null) {
            return;
        }
        decoded.setWorld(clientLevel);
        decoded.setClient(true);

        if (old == null) {
            ParticleCompositionManager.INSTANCE.addClient(decoded);
            return;
        }

        old.update(decoded);
        old.setWorld(clientLevel);
        old.setClient(true);
        if (!(old instanceof SequencedParticleComposition)) {
            old.flush();
        }
    }
}
