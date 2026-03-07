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

import com.reiasu.reiparticlesapi.network.packet.PacketDisplayEntityS2C;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketDisplayEntityS2CTest {
    @Test
    void shouldRoundTripDisplayPacketMethodAndPayload() {
        DebugDisplayEntity entity = new DebugDisplayEntity(null, 4.0, 5.0, 6.0, "group", 20);
        PacketDisplayEntityS2C source = PacketDisplayEntityS2C.ofToggle(entity);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        PacketDisplayEntityS2C.encode(source, buf);
        PacketDisplayEntityS2C decoded = PacketDisplayEntityS2C.decode(buf);

        assertEquals(PacketDisplayEntityS2C.Method.TOGGLE, decoded.method());
        assertEquals(source.uuid(), decoded.uuid());
        assertEquals(source.type(), decoded.type());
        assertArrayEquals(source.data(), decoded.data());
    }
}

