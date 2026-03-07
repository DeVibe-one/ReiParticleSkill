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
package com.reiasu.reiparticlesapi.network.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraShakeS2CPacketTest {
    @Test
    void shouldEncodeAndDecodeWithoutLosingData() {
        CameraShakeS2CPacket input = new CameraShakeS2CPacket(12.5, new Vec3(1.25, 80.0, -4.5), 0.85, 40);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        CameraShakeS2CPacket.encode(input, buf);
        CameraShakeS2CPacket output = CameraShakeS2CPacket.decode(buf);

        assertEquals(input.range(), output.range());
        assertEquals(input.origin().x, output.origin().x);
        assertEquals(input.origin().y, output.origin().y);
        assertEquals(input.origin().z, output.origin().z);
        assertEquals(input.amplitude(), output.amplitude());
        assertEquals(input.tick(), output.tick());
    }
}
