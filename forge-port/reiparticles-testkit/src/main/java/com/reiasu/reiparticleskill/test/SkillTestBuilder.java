// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.test;

import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.test.SimpleTestGroup;
import com.reiasu.reiparticlesapi.test.TestManager;
import com.reiasu.reiparticlesapi.test.api.TestGroup;
import com.reiasu.reiparticlesapi.test.api.TestGroupBuilder;
import com.reiasu.reiparticlesapi.test.api.TestOption;
import com.reiasu.reiparticleskill.particles.core.emitters.p2.formation.SwordFormationEmitters;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Skill test builder that creates a test group with sword formation emitter tests.
 * Server-side port of the Fabric original, adapted to Forge test API.
 */
public final class SkillTestBuilder implements TestGroupBuilder {
    public static final String ID = "skill-test-builder";
    private static final String SWORD_FORMATION_OPTION_ID = "sword-formation";

    private final ServerPlayer player;

    public SkillTestBuilder(ServerPlayer player) {
        this.player = player;
    }

    public static void register() {
        TestManager.INSTANCE.register(ID, SkillTestBuilder::new);
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public String groupID() {
        return ID;
    }

    @Override
    public TestGroup build() {
        SimpleTestGroup group = new SimpleTestGroup(groupID(), player);
        group.appendOption(this::createSwordFormationTest);
        return group;
    }

    private TestOption createSwordFormationTest() {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return new NoOpTestOption(SWORD_FORMATION_OPTION_ID);
        }
        return createSwordFormationTest(level, player.getEyePosition());
    }

    static TestOption createSwordFormationTest(ServerLevel level, Vec3 eyePos) {
        return new SwordFormationTestOption(level, eyePos == null ? Vec3.ZERO : eyePos);
    }

    private static final class SwordFormationTestOption implements TestOption {
        private final ServerLevel level;
        private final Vec3 eyePos;
        private final SwordFormationEmitters emitter;
        private int ticks;
        private boolean started;
        private boolean valid = true;

        private SwordFormationTestOption(ServerLevel level, Vec3 eyePos) {
            this.level = level;
            this.eyePos = eyePos;
            this.emitter = new SwordFormationEmitters(eyePos, level);
            this.emitter.setMaxTick(-1);
        }

        @Override
        public void start() {
            if (started || !valid) {
                return;
            }
            started = true;
            ParticleEmittersManager.spawnEmitters(emitter);
        }

        @Override
        public void stop() {
            finish();
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public void onFailed() {
            finish();
        }

        @Override
        public void onSuccess() {
            finish();
        }

        @Override
        public String optionID() {
            return SWORD_FORMATION_OPTION_ID;
        }

        @Override
        public void doTick() {
            if (!started || !valid) {
                return;
            }
            ticks++;
            if (ticks >= 20) {
                onSuccess();
            }
        }

        private void finish() {
            valid = false;
            emitter.cancel();
        }
    }

    private record NoOpTestOption(String optionID) implements TestOption {
        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void onFailed() {
        }

        @Override
        public void onSuccess() {
        }

        @Override
        public void doTick() {
        }
    }
}

