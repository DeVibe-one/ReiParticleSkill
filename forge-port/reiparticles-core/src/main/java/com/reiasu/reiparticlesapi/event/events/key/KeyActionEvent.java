// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.key;

import com.reiasu.reiparticlesapi.event.events.entity.player.PlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class KeyActionEvent extends PlayerEvent {
    private final ResourceLocation keyId;
    private final KeyActionType action;
    private final int pressTick;
    private final boolean release;
    private final boolean serverSide;

    public KeyActionEvent(
            Player player,
            ResourceLocation keyId,
            KeyActionType action,
            int pressTick,
            boolean release,
            boolean serverSide
    ) {
        super(player);
        this.keyId = keyId;
        this.action = action;
        this.pressTick = pressTick;
        this.release = release;
        this.serverSide = serverSide;
    }

    public ResourceLocation getKeyId() {
        return keyId;
    }

    public KeyActionType getAction() {
        return action;
    }

    public int getPressTick() {
        return pressTick;
    }

    public boolean isRelease() {
        return release;
    }

    public boolean getServerSide() {
        return serverSide;
    }
}

