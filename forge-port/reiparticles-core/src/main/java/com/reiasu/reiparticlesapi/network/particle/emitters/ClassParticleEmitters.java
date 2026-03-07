// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.network.particle.emitters.command.ParticleCommandQueue;
import com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEvent;
import com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler;
import com.reiasu.reiparticlesapi.network.particle.emitters.type.EmittersShootType;
import com.reiasu.reiparticlesapi.network.particle.emitters.type.PointEmittersShootType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.SortedMap;

/**
 * Extended emitter class that adds shoot types, command queues,
 * collision detection, and per-particle physics on top of ClassEmitters.
 * Server-side port of the Fabric ClassParticleEmitters.
 */
public abstract class ClassParticleEmitters extends ClassEmitters {

    public static final Companion Companion = new Companion();

    private EmittersShootType shootType = new PointEmittersShootType();
    private final ParticleCommandQueue commandQueue = new ParticleCommandQueue();
    private boolean enableCollision = false;
    private double collisionRadius = 0.5;

    public ClassParticleEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    // ---- Shoot type ----

    public EmittersShootType getShootType() {
        return shootType;
    }

    public void setShootType(EmittersShootType shootType) {
        if (shootType != null) {
            this.shootType = shootType;
        }
    }

    // ---- Command queue ----

    public ParticleCommandQueue getCommandQueue() {
        return commandQueue;
    }

    // ---- Collision ----

    public boolean getEnableCollision() {
        return enableCollision;
    }

    public void setEnableCollision(boolean enableCollision) {
        this.enableCollision = enableCollision;
    }

    public double getCollisionRadius() {
        return collisionRadius;
    }

    public void setCollisionRadius(double collisionRadius) {
        this.collisionRadius = collisionRadius;
    }

    /**
     * Fire an event to all registered handlers matching the target event ID.
     */
    public void fireEvent(ParticleEvent event) {
        if (event == null) return;
        String eventId = event.getEventID();
        for (SortedMap<ParticleEventHandler, Boolean> map : getHandlerList().values()) {
            for (ParticleEventHandler handler : map.keySet()) {
                if (handler.getTargetEventID().equals(eventId)) {
                    handler.handle(event);
                    if (event.getCanceled()) return;
                }
            }
        }
    }

    // ---- Encode/Decode ----

    public static final class Companion {
        private Companion() {
        }

        public void encodeBase(ClassParticleEmitters data, FriendlyByteBuf buf) {
            if (data == null || buf == null) return;
            ClassEmitters.Companion.encodeBase(data, buf);
        }

        public void decodeBase(ClassParticleEmitters container, FriendlyByteBuf buf) {
            if (container == null || buf == null) return;
            ClassEmitters.Companion.decodeBase(container, buf);
        }
    }
}
