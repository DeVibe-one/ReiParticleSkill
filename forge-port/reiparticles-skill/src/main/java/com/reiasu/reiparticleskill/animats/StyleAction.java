// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Consumer;

public final class StyleAction extends AnimateAction {
    private final ParticleGroupStyle style;
    private final Level spawnWorld;
    private final Vec3 spawnPos;
    private final Consumer<StyleAction> ticking;
    private boolean firstTick;

    public StyleAction(ParticleGroupStyle style, Level spawnWorld, Vec3 spawnPos, Consumer<StyleAction> ticking) {
        this.style = Objects.requireNonNull(style, "style");
        this.spawnWorld = Objects.requireNonNull(spawnWorld, "spawnWorld");
        this.spawnPos = Objects.requireNonNull(spawnPos, "spawnPos");
        this.ticking = Objects.requireNonNull(ticking, "ticking");
    }

    public ParticleGroupStyle getStyle() {
        return style;
    }

    public Level getSpawnWorld() {
        return spawnWorld;
    }

    public Vec3 getSpawnPos() {
        return spawnPos;
    }

    public Consumer<StyleAction> getTicking() {
        return ticking;
    }

    public boolean getFirstTick() {
        return firstTick;
    }

    public void setFirstTick(boolean firstTick) {
        this.firstTick = firstTick;
    }

    @Override
    public boolean checkDone() {
        return style.getCanceled();
    }

    @Override
    public void tick() {
        if (!firstTick) {
            firstTick = true;
            ParticleStyleManager.spawnStyle(spawnWorld, spawnPos, style);
        }
        ticking.accept(this);
    }

    @Override
    public void onStart() {
        firstTick = false;
    }

    @Override
    public void onDone() {
        style.remove();
        setDone(true);
    }
}
