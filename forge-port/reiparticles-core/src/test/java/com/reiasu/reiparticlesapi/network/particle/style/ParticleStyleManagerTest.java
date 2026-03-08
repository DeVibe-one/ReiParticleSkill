/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleStyleS2C;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleStyleManagerTest {
    @AfterEach
    void cleanup() {
        ParticleStyleManager.clear();
    }

    @Test
    void shouldContinueClientTickAfterStyleFailure() {
        FailingStyle failing = new FailingStyle();
        CountingStyle healthy = new CountingStyle();
        ParticleStyleManager.getClientViewStyles().put(failing.getUuid(), failing);
        ParticleStyleManager.getClientViewStyles().put(healthy.getUuid(), healthy);

        ParticleStyleManager.doTickClient();

        assertEquals(1, healthy.ticks);
        assertEquals(1, ParticleStyleManager.getClientViewStyles().size());
        assertSame(healthy, ParticleStyleManager.getClientViewStyles().get(healthy.getUuid()));
    }

    @Test
    void shouldIncludeRegistryKeyInCreatePacket() {
        ResourceLocation key = new ResourceLocation("reiparticlesruntime", "packet_style_" + System.nanoTime());
        ParticleStyleManager.register(key, new CountingProvider());
        CountingStyle style = new CountingStyle();
        style.setRegistryKey(key);

        PacketParticleStyleS2C packet = ParticleStyleManager.buildCreatePacket(style, new Vec3(1.0, 2.0, 3.0));
        ParticleControllerDataBuffer<?> keyBuffer = packet.args().get("style_registry_key");

        assertTrue(keyBuffer != null);
        assertEquals(key.toString(), keyBuffer.getLoadedValue());
    }

    private static final class CountingProvider implements ParticleStyleProvider<CountingStyle> {
        @Override
        public CountingStyle create() {
            return new CountingStyle();
        }
    }

    private static final class CountingStyle extends ParticleGroupStyle {
        private int ticks;

        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        @Override
        public void tick() {
            super.tick();
            ticks++;
        }
    }

    private static final class FailingStyle extends ParticleGroupStyle {
        @Override
        public Map<StyleData, RelativeLocation> getCurrentFrames() {
            return Map.of();
        }

        @Override
        public void onDisplay() {
        }

        @Override
        public void tick() {
            throw new IllegalStateException("boom");
        }
    }
}
