// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Interface for displaying particles/groups/styles/compositions at a world position.
 * <p>
 * Each implementation wraps a specific display mechanism and returns
 * a {@link Controllable} handle that can be used for subsequent control operations.
 */
public interface ParticleDisplayer {

    /**
     * Display this displayer at the given location in the given client world.
     *
     * @return a {@link Controllable} handle, or null if spawning failed
     */
    @Nullable
    Controllable<?> display(Vec3 loc, ClientLevel world);

    // ---- Factory methods ----

    static ParticleDisplayer withSingle(ControllableParticleEffect effect) {
        return new SingleParticleDisplayer(effect);
    }

    static ParticleDisplayer withGroup(ControllableParticleGroup group) {
        return new ParticleGroupDisplayer(group);
    }

    static ParticleDisplayer withStyle(ParticleGroupStyle style) {
        return new ParticleStyleDisplayer(style);
    }

    static ParticleDisplayer withDisplayEntity(DisplayEntity entity) {
        return new DisplayEntityDisplayer(entity);
    }

    static ParticleDisplayer withComposition(ParticleComposition composition) {
        return new ParticleCompositionDisplayer(composition);
    }

    // ---- Companion for backward compat ----

    Companion Companion = new Companion();

    class Companion {
        public ParticleDisplayer withSingle(ControllableParticleEffect effect) {
            return ParticleDisplayer.withSingle(effect);
        }

        public ParticleDisplayer withGroup(ControllableParticleGroup group) {
            return ParticleDisplayer.withGroup(group);
        }

        public ParticleDisplayer withStyle(ParticleGroupStyle style) {
            return ParticleDisplayer.withStyle(style);
        }

        public ParticleDisplayer withDisplayEntity(DisplayEntity entity) {
            return ParticleDisplayer.withDisplayEntity(entity);
        }

        public ParticleDisplayer withComposition(ParticleComposition composition) {
            return ParticleDisplayer.withComposition(composition);
        }
    }

    // ---- Inner implementations ----

    /**
     * Displays a single ControllableParticle via its ControllableParticleEffect.
     */
    class SingleParticleDisplayer implements ParticleDisplayer {
        private final ControllableParticleEffect effect;

        public SingleParticleDisplayer(ControllableParticleEffect effect) {
            this.effect = effect;
        }

        public ControllableParticleEffect getEffect() {
            return effect;
        }

        @Override
        @Nullable
        public Controllable<?> display(Vec3 loc, ClientLevel world) {
            // Spawn the particle effect in the world (ControllableParticleEffect extends ParticleOptions)
            world.addAlwaysVisibleParticle(effect, true, loc.x, loc.y, loc.z, 0.0, 0.0, 0.0);
            ParticleController controller = ControlParticleManager.INSTANCE.getControl(effect.getControlUUID());
            return controller;
        }
    }

    /**
     * Displays a ControllableParticleGroup.
     */
    class ParticleGroupDisplayer implements ParticleDisplayer {
        private final ControllableParticleGroup group;

        public ParticleGroupDisplayer(ControllableParticleGroup group) {
            this.group = group;
        }

        public ControllableParticleGroup getGroup() {
            return group;
        }

        @Override
        @Nullable
        public Controllable<?> display(Vec3 loc, ClientLevel world) {
            group.display(loc, world);
            return group;
        }
    }

    /**
     * Displays a ParticleGroupStyle.
     */
    class ParticleStyleDisplayer implements ParticleDisplayer {
        private final ParticleGroupStyle style;

        public ParticleStyleDisplayer(ParticleGroupStyle style) {
            this.style = style;
        }

        public ParticleGroupStyle getStyle() {
            return style;
        }

        @Override
        @Nullable
        public Controllable<?> display(Vec3 loc, ClientLevel world) {
            style.display(loc, world);
            return style;
        }
    }

    /**
     * Displays a DisplayEntity.
     */
    class DisplayEntityDisplayer implements ParticleDisplayer {
        private final DisplayEntity entity;

        public DisplayEntityDisplayer(DisplayEntity entity) {
            this.entity = entity;
        }

        public DisplayEntity getEntity() {
            return entity;
        }

        @Override
        @Nullable
        public Controllable<?> display(Vec3 loc, ClientLevel world) {
            entity.setPos(loc);
            // DisplayEntity does not have setWorld(ClientLevel); position is sufficient
            DisplayEntityManager.INSTANCE.addClient(entity);
            return null; // DisplayEntity does not implement Controllable
        }
    }

    /**
     * Displays a ParticleComposition.
     */
    class ParticleCompositionDisplayer implements ParticleDisplayer {
        private final ParticleComposition composition;

        public ParticleCompositionDisplayer(ParticleComposition composition) {
            this.composition = composition;
        }

        public ParticleComposition getComposition() {
            return composition;
        }

        @Override
        @Nullable
        public Controllable<?> display(Vec3 loc, ClientLevel world) {
            composition.setPosition(loc);
            composition.setWorld(world);
            composition.display();
            return composition;
        }
    }
}
