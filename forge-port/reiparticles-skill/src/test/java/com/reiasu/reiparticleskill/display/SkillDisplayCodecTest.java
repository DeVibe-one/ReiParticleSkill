/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticleSkill.
 *
 * ReiParticleSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticleSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticleSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.display;

import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.testutil.UnsafeAllocator;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillDisplayCodecTest {
    @AfterEach
    void cleanup() {
        DisplayEntityManager.INSTANCE.getRegisteredTypes().remove(BarrageItemDisplay.TYPE_ID);
        DisplayEntityManager.INSTANCE.getRegisteredTypes().remove(SwordLightDisplay.TYPE_ID);
        DisplayEntityManager.INSTANCE.getRegisteredTypes().remove(LightFlashDisplay.TYPE_ID);
    }

    @Test
    void shouldRegisterSkillDisplayTypes() {
        SkillDisplayTypes.register();

        assertTrue(DisplayEntityManager.INSTANCE.getRegisteredTypes().containsKey(BarrageItemDisplay.TYPE_ID));
        assertTrue(DisplayEntityManager.INSTANCE.getRegisteredTypes().containsKey(SwordLightDisplay.TYPE_ID));
        assertTrue(DisplayEntityManager.INSTANCE.getRegisteredTypes().containsKey(LightFlashDisplay.TYPE_ID));
    }

    @Test
    void shouldRoundTripBarrageItemDisplayState() {
        UnsafeAllocator.allocate(ServerLevel.class);
        BarrageItemDisplay source = new BarrageItemDisplay(new Vec3(1.0, 2.0, 3.0));
        source.setItem(new ItemStack(Items.IRON_SWORD));
        source.setBlock(true);
        source.setSign(2);
        source.setPrevPos(new Vec3(-1.0, -2.0, -3.0));
        source.setVelocity(new Vec3(0.25, 0.5, 0.75));
        source.setYaw(15.0f);
        source.setPrevYaw(10.0f);
        source.setPitch(20.0f);
        source.setPrevPitch(18.0f);
        source.setRoll(30.0f);
        source.setPrevRoll(28.0f);
        source.setScale(1.5f);
        source.setPreScale(1.25f);
        source.setTargetScale(2.0f);
        source.setScaledSpeed(0.4f);
        source.setTargetYaw(90.0f);
        source.setTargetPitch(35.0f);
        source.setRotateSpeed(12.0f);
        source.setBlendCount(4);
        source.setAge(9);
        source.setDisplayTick(3);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(source.encodeToBytes()));
        BarrageItemDisplay decoded = BarrageItemDisplay.decode(buf);

        assertEquals(source.getPos(), decoded.getPos());
        assertEquals(source.getItem().getItem(), decoded.getItem().getItem());
        assertEquals(source.isBlock(), decoded.isBlock());
        assertEquals(source.getSign(), decoded.getSign());
        assertEquals(source.getPrevPos(), decoded.getPrevPos());
        assertEquals(source.getVelocity(), decoded.getVelocity());
        assertEquals(source.getYaw(), decoded.getYaw());
        assertEquals(source.getPrevYaw(), decoded.getPrevYaw());
        assertEquals(source.getPitch(), decoded.getPitch());
        assertEquals(source.getPrevPitch(), decoded.getPrevPitch());
        assertEquals(source.getRoll(), decoded.getRoll());
        assertEquals(source.getPrevRoll(), decoded.getPrevRoll());
        assertEquals(source.getScale(), decoded.getScale());
        assertEquals(source.getTargetScale(), decoded.getTargetScale());
        assertEquals(source.getRotateSpeed(), decoded.getRotateSpeed());
        assertEquals(source.getBlendCount(), decoded.getBlendCount());
        assertEquals(source.getAge(), decoded.getAge());
        assertEquals(source.getDisplayTick(), decoded.getDisplayTick());
    }

    @Test
    void shouldRoundTripSwordLightDisplayState() {
        SwordLightDisplay source = new SwordLightDisplay(new Vec3(2.0, 3.0, 4.0));
        source.setEnd(new Vec3(8.0, 9.0, 10.0));
        source.setMaxAge(40);
        source.setBloomCount(5);
        source.setThinness(0.25f);
        source.tick();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(source.encodeToBytes()));
        SwordLightDisplay decoded = SwordLightDisplay.decode(buf);

        assertEquals(source.getPos(), decoded.getPos());
        assertEquals(source.getEnd(), decoded.getEnd());
        assertEquals(source.getMaxAge(), decoded.getMaxAge());
        assertEquals(source.getBloomCount(), decoded.getBloomCount());
        assertEquals(source.getThinness(), decoded.getThinness());
        assertEquals(source.getAge(), decoded.getAge());
        assertEquals(source.isRemoved(), decoded.isRemoved());
    }

    @Test
    void shouldRoundTripLightFlashDisplayState() {
        LightFlashDisplay source = new LightFlashDisplay(new Vec3(5.0, 6.0, 7.0));
        source.setBloomCount(6);
        source.setAge(4);
        source.setMaxAge(25);
        source.setLengthMax(42.0f);
        source.setThicknessMax(3.5f);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(source.encodeToBytes()));
        LightFlashDisplay decoded = LightFlashDisplay.decode(buf);

        assertEquals(source.getPos(), decoded.getPos());
        assertEquals(source.getBloomCount(), decoded.getBloomCount());
        assertEquals(source.getAge(), decoded.getAge());
        assertEquals(source.getMaxAge(), decoded.getMaxAge());
        assertEquals(source.getLengthMax(), decoded.getLengthMax());
        assertEquals(source.getThicknessMax(), decoded.getThicknessMax());
    }
}

