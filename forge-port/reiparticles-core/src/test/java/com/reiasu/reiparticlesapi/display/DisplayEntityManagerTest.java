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
package com.reiasu.reiparticlesapi.display;

import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayEntityManagerTest {
    @AfterEach
    void cleanup() {
        DisplayEntityManager.INSTANCE.clear();
    }

    @Test
    void shouldTickAndRemoveCanceledDisplays() {
        DisplayEntityManager.INSTANCE.spawn(new DebugDisplayEntity(0.0, 64.0, 0.0, "group"));
        assertEquals(1, DisplayEntityManager.INSTANCE.activeCount());

        DisplayEntity entity = DisplayEntityManager.INSTANCE.getDisplays().get(0);
        entity.cancel();
        DisplayEntityManager.INSTANCE.tickAll();

        assertEquals(0, DisplayEntityManager.INSTANCE.activeCount());
    }

    @Test
    void shouldIgnoreUnknownDisplayObjects() {
        DisplayEntityManager.INSTANCE.spawn("invalid");
        assertEquals(0, DisplayEntityManager.INSTANCE.activeCount());
    }

    @Test
    void shouldBindWorldAndPositionWhenSpawningInWorld() {
        ServerLevel level = UnsafeAllocator.allocate(ServerLevel.class);
        Vec3 pos = new Vec3(1.25, 64.0, -3.5);
        TestDisplayEntity entity = new TestDisplayEntity();

        entity.spawnInWorld(level, pos);

        assertEquals(1, DisplayEntityManager.INSTANCE.activeCount());
        assertSame(level, entity.boundLevel());
        assertEquals(pos, entity.getPos());
    }

    @Test
    void shouldUsePolymorphicPositionAccessorsForBaseSerialization() {
        Vec3 pos = new Vec3(4.0, 5.0, 6.0);
        ShadowedPositionDisplay source = new ShadowedPositionDisplay();
        source.setPos(pos);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DisplayEntity.encodeBase(source, buf);

        ShadowedPositionDisplay decoded = new ShadowedPositionDisplay();
        DisplayEntity.decodeBase(decoded, buf);

        assertEquals(pos, decoded.getPos());

        ShadowedPositionDisplay updated = new ShadowedPositionDisplay();
        updated.update(source);
        assertEquals(pos, updated.getPos());
    }

    @Test
    void shouldUpdateDebugDisplayPositionAfterCreation() {
        DebugDisplayEntity entity = new DebugDisplayEntity(0.0, 64.0, 0.0, "group");
        Vec3 updated = new Vec3(2.0, 70.0, -1.0);

        entity.setPos(updated);

        assertEquals(updated, entity.getPos());
        assertEquals(updated.x, entity.getX());
        assertEquals(updated.y, entity.getY());
        assertEquals(updated.z, entity.getZ());
    }

    @Test
    void shouldRegisterBuiltinDebugDisplayDecoder() {
        DisplayEntityManager.INSTANCE.registerBuiltinTypes();

        assertTrue(DisplayEntityManager.INSTANCE.getRegisteredTypes().containsKey(DebugDisplayEntity.TYPE_ID));
    }

    @Test
    void shouldEncodeAndDecodeDebugDisplayEntityState() {
        DebugDisplayEntity source = new DebugDisplayEntity(null, 1.0, 2.0, 3.0, "group", 12);
        source.setScale(1.5f);
        source.tick();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(source.encodeToBytes()));
        DebugDisplayEntity decoded = DebugDisplayEntity.decode(buf);

        assertEquals(source.getControlUUID(), decoded.getControlUUID());
        assertEquals(source.getPos(), decoded.getPos());
        assertEquals(source.getKind(), decoded.getKind());
        assertEquals(source.getMaxTicks(), decoded.getMaxTicks());
        assertEquals(source.getTickCount(), decoded.getTickCount());
        assertEquals(source.getScale(), decoded.getScale());
    }

    private static final class TestDisplayEntity extends DisplayEntity {
        private Level boundLevel() {
            return level();
        }
    }

    private static final class ShadowedPositionDisplay extends DisplayEntity {
        private Vec3 shadowedPos = Vec3.ZERO;

        @Override
        public Vec3 getPos() {
            return shadowedPos;
        }

        @Override
        public void setPos(Vec3 pos) {
            this.shadowedPos = pos == null ? Vec3.ZERO : pos;
        }
    }
}

