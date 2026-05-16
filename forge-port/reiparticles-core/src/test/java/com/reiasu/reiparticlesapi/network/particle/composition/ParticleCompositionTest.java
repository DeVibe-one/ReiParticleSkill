// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleCompositionTest {
    @Test
    void shouldDisplayOnceAndRunPretickActionsUntilMaxTick() {
        TrackingComposition composition = new TrackingComposition();
        AtomicInteger preTicks = new AtomicInteger();
        composition.addPreTickAction(pc -> preTicks.incrementAndGet());
        composition.setMaxTick(2);

        composition.display();
        composition.display();
        composition.tick();
        composition.tick();
        composition.tick();

        assertTrue(composition.getDisplayed());
        assertEquals(1, composition.onDisplayCalls);
        assertEquals(2, preTicks.get());
        assertTrue(composition.getCanceled());
        assertEquals(2, composition.getTick());
    }

    @Test
    void shouldEncodeDecodeBaseState() {
        TrackingComposition source = new TrackingComposition();
        source.setControlUUID(java.util.UUID.randomUUID());
        source.setVisibleRange(72.0);
        source.setPosition(new Vec3(1.0, 2.0, 3.0));
        source.setAxis(new RelativeLocation(0.0, 0.0, 1.0));
        source.setScale(1.5);
        source.setRoll(0.75);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        ParticleComposition.encodeBase(source, buf);

        TrackingComposition decoded = new TrackingComposition();
        ParticleComposition.decodeBase(decoded, buf);

        assertEquals(source.getControlUUID(), decoded.getControlUUID());
        assertEquals(source.getVisibleRange(), decoded.getVisibleRange());
        assertEquals(source.getPosition(), decoded.getPosition());
        assertEquals(source.getAxis().toVector(), decoded.getAxis().toVector());
        assertEquals(source.getScale(), decoded.getScale());
        assertEquals(source.getRoll(), decoded.getRoll());
        assertFalse(decoded.getCanceled());
    }

    @Test
    void shouldReuseDefaultLengthsWhenScalingLocations() {
        TrackingComposition composition = new TrackingComposition();
        CompositionData data = new CompositionData();
        RelativeLocation location = new RelativeLocation(3.0, 0.0, 0.0);
        composition.locations.put(data, location);

        composition.scale(2.0);
        composition.toggleScale(composition.getParticles());
        assertEquals(6.0, location.length(), 1.0E-6);
        assertEquals(3.0, composition.getParticleDefaultLength().get(data.getUuid()), 1.0E-6);

        composition.scale(0.5);
        composition.toggleScale(composition.getParticles());
        assertEquals(1.5, location.length(), 1.0E-6);
    }

    @Test
    void shouldRejectSequencedParticleStatusAtCountBoundary() {
        TrackingSequencedComposition composition = new TrackingSequencedComposition();
        composition.setCount(1);

        composition.setParticleStatus(0, true);

        assertTrue(composition.isParticleDisplayed(0));
        assertFalse(composition.isParticleDisplayed(1));
    }

    @Test
    void autoCompositionShouldSnapshotAddedLocations() {
        AutoParticleComposition composition = new AutoParticleComposition();
        CompositionData data = new CompositionData();
        RelativeLocation source = new RelativeLocation(3.0, 0.0, 0.0);

        composition.addParticle(data, source);
        source.scale(2.0);
        RelativeLocation firstLocation = composition.getParticles().get(data);

        assertEquals(3.0, firstLocation.length(), 1.0E-6);
        firstLocation.scale(2.0);
        RelativeLocation secondLocation = composition.getParticles().get(data);

        assertEquals(6.0, firstLocation.length(), 1.0E-6);
        assertEquals(3.0, secondLocation.length(), 1.0E-6);
        assertNotSame(firstLocation, secondLocation);
    }

    @Test
    void autoCompositionShouldNotCompoundScaleIntoStoredLocations() {
        AutoParticleComposition composition = new AutoParticleComposition();
        CompositionData data = new CompositionData();
        composition.addParticle(data, new RelativeLocation(3.0, 0.0, 0.0));

        composition.scale(2.0);
        Map<CompositionData, RelativeLocation> firstFrame = composition.getParticles();
        composition.toggleScale(firstFrame);

        composition.scale(0.5);
        Map<CompositionData, RelativeLocation> secondFrame = composition.getParticles();
        composition.toggleScale(secondFrame);

        assertEquals(6.0, firstFrame.get(data).length(), 1.0E-6);
        assertEquals(1.5, secondFrame.get(data).length(), 1.0E-6);
        assertEquals(3.0, composition.getParticles().get(data).length(), 1.0E-6);
    }

    private static final class TrackingComposition extends ParticleComposition {
        private final Map<CompositionData, RelativeLocation> locations = new LinkedHashMap<>();
        private int onDisplayCalls;

        @Override
        public Map<CompositionData, RelativeLocation> getParticles() {
            return locations;
        }

        @Override
        public void onDisplay() {
            onDisplayCalls++;
        }
    }

    private static final class TrackingSequencedComposition extends SequencedParticleComposition {
        private TrackingSequencedComposition() {
            super(Vec3.ZERO, null);
        }

        @Override
        public SortedMap<CompositionData, RelativeLocation> getParticleSequenced() {
            return new TreeMap<>();
        }

        @Override
        public void onDisplay() {
        }
    }
}
