// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.EndBeamExplosionEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.PillarFourierBeamEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.RespawnEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.ShockwaveWallEmitter;
import com.reiasu.reiparticleskill.end.respawn.runtime.emitter.SummonFlashEmitter;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonRespawnAnimationDirectorTest {
    @Test
    void endFinaleShouldIncludeSummonFlashOnlyOnce() throws Exception {
        DragonRespawnAnimationDirector director = new DragonRespawnAnimationDirector();

        assertTrue(director.startEndFinale(Vec3.ZERO, List.of()));
        assertFalse(director.startEndFinale(Vec3.ZERO, List.of()));

        List<RespawnEmitter> emitters = activeEmitters(director);
        assertEquals(4, emitters.size());
        assertEquals(1, count(emitters, SummonFlashEmitter.class));
        assertEquals(1, count(emitters, EndBeamExplosionEmitter.class));
        assertEquals(1, count(emitters, PillarFourierBeamEmitter.class));
        assertEquals(1, count(emitters, ShockwaveWallEmitter.class));
    }

    @SuppressWarnings("unchecked")
    private static List<RespawnEmitter> activeEmitters(DragonRespawnAnimationDirector director) throws Exception {
        Field field = DragonRespawnAnimationDirector.class.getDeclaredField("activeEmitters");
        field.setAccessible(true);
        return (List<RespawnEmitter>) field.get(director);
    }

    private static int count(List<RespawnEmitter> emitters, Class<? extends RespawnEmitter> type) {
        int count = 0;
        for (RespawnEmitter emitter : emitters) {
            if (type.isInstance(emitter)) {
                count++;
            }
        }
        return count;
    }
}
