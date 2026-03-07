// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind;

import com.reiasu.reiparticlesapi.network.particle.emitters.PhysicConstant;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry and physics helper for {@link WindDirection} implementations.
 * <p>
 * Forge port note: The Fabric version registered StreamCodec per wind type. In the
 * Forge port we register factory suppliers instead; actual serialization happens at
 * a higher level using FriendlyByteBuf when needed.
 */
public final class WindDirections {

    public static final WindDirections INSTANCE = new WindDirections();

    private final Map<String, Supplier<WindDirection>> factories = new HashMap<>();

    private WindDirections() {
    }

    /**
     * Register a wind direction factory by ID.
     */
    public void register(String id, Supplier<WindDirection> factory) {
        factories.put(id, factory);
    }

    /**
     * Look up a factory by ID. Throws if not found.
     */
    public Supplier<WindDirection> getFactoryFromID(String id) {
        Supplier<WindDirection> factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown WindDirection ID: " + id);
        }
        return factory;
    }

    /**
     * Compute the acceleration vector caused by wind drag on a particle.
     * <p>
     * Uses simplified aerodynamic drag: F = 0.5 * rho * Cd * A * |V_rel|^2 * dir(V_rel)
     * scaled by 0.05 for per-tick integration.
     *
     * @param wind               the wind source
     * @param pos                the particle's world position
     * @param airDensity         air density (kg/m^3), typically {@link PhysicConstant#SEA_AIR_DENSITY}
     * @param dragCoefficient    drag coefficient, typically {@link PhysicConstant#DRAG_COEFFICIENT}
     * @param crossSectionalArea cross-section area, typically {@link PhysicConstant#CROSS_SECTIONAL_AREA}
     * @param v                  the particle's current velocity
     * @return the acceleration vector to add to particle velocity
     */
    public Vec3 handleWindForce(WindDirection wind, Vec3 pos, double airDensity,
                                double dragCoefficient, double crossSectionalArea, Vec3 v) {
        if (!wind.inRange(pos)) {
            return Vec3.ZERO;
        }

        Vec3 windVec = wind.getWind(pos);
        if (windVec.lengthSqr() > 0.0) {
            Vec3 relativeWind = windVec.subtract(v);
            double magnitude = 0.5 * airDensity * dragCoefficient * crossSectionalArea
                    * relativeWind.lengthSqr() * 0.05;
            return relativeWind.normalize().scale(magnitude);
        }
        return Vec3.ZERO;
    }

    /**
     * Overload that defaults velocity to zero.
     */
    public Vec3 handleWindForce(WindDirection wind, Vec3 pos, double airDensity,
                                double dragCoefficient, double crossSectionalArea) {
        return handleWindForce(wind, pos, airDensity, dragCoefficient, crossSectionalArea, Vec3.ZERO);
    }

    /**
     * Called during mod init to register built-in wind types.
     */
    public void init() {
        register(GlobalWindDirection.ID, () -> new GlobalWindDirection(Vec3.ZERO));
        register(BallWindDirection.ID, () -> new BallWindDirection(Vec3.ZERO, 10.0,
                new com.reiasu.reiparticlesapi.utils.RelativeLocation()));
        register(BoxWindDirection.ID, () -> new BoxWindDirection(Vec3.ZERO,
                com.reiasu.reiparticlesapi.barrages.HitBox.of(1.0, 1.0, 1.0),
                new com.reiasu.reiparticlesapi.utils.RelativeLocation()));
    }
}
