// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.type;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry and factory for {@link EmittersShootType} implementations.
 * <p>
 * Forge port note: The Fabric version registered StreamCodec per type for network serialization.
 * In Forge we register factory suppliers instead; actual serialization happens at a higher level
 * using FriendlyByteBuf when needed.
 */
public final class EmittersShootTypes {

    public static final EmittersShootTypes INSTANCE = new EmittersShootTypes();

    private final Map<String, Supplier<EmittersShootType>> factories = new HashMap<>();

    private EmittersShootTypes() {
    }

    /**
     * Register a shoot type factory by ID.
     */
    public void register(String id, Supplier<EmittersShootType> factory) {
        factories.put(id, factory);
    }

    /**
     * Look up a factory by ID; returns null if not registered.
     */
    public Supplier<EmittersShootType> fromID(String id) {
        return factories.get(id);
    }

    // ---- convenience factory methods ----

    public static EmittersShootType point() {
        return new PointEmittersShootType();
    }

    public static EmittersShootType line(Vec3 dir, double step) {
        return new LineEmittersShootType(dir, step);
    }

    /**
     * Creates a box shoot type from a centered HitBox.
     */
    public static EmittersShootType box(com.reiasu.reiparticlesapi.barrages.HitBox hitBox) {
        return new BoxEmittersShootType(hitBox);
    }

    public static EmittersShootType math() {
        return new MathEmittersShootType();
    }

    /**
     * Called during mod init to wire up built-in factories.
     */
    public void init() {
        register(PointEmittersShootType.ID, PointEmittersShootType::new);
        register(LineEmittersShootType.ID, () -> new LineEmittersShootType(Vec3.ZERO, 1.0));
        register(BoxEmittersShootType.ID, () -> new BoxEmittersShootType(
                com.reiasu.reiparticlesapi.barrages.HitBox.of(1.0, 1.0, 1.0)));
        register(MathEmittersShootType.ID, MathEmittersShootType::new);
    }
}
