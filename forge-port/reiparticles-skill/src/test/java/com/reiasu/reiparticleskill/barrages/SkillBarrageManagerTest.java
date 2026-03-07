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
package com.reiasu.reiparticleskill.barrages;

import com.reiasu.reiparticlesapi.barrages.Barrage;
import com.reiasu.reiparticlesapi.barrages.BarrageHitResult;
import com.reiasu.reiparticlesapi.barrages.BarrageOption;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillBarrageManagerTest {
    @AfterEach
    void tearDown() {
        SkillBarrageManager.INSTANCE.clear();
    }

    @Test
    void invalidBarragesArePrunedAfterTick() {
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(1));
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(3));
        assertEquals(2, SkillBarrageManager.INSTANCE.activeCount());

        SkillBarrageManager.INSTANCE.tickAll();
        assertEquals(1, SkillBarrageManager.INSTANCE.activeCount());

        SkillBarrageManager.INSTANCE.tickAll();
        assertEquals(1, SkillBarrageManager.INSTANCE.activeCount());

        SkillBarrageManager.INSTANCE.tickAll();
        assertEquals(0, SkillBarrageManager.INSTANCE.activeCount());
    }

    @Test
    void clearCancelsAndRemovesAll() {
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(8));
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(8));
        assertEquals(2, SkillBarrageManager.INSTANCE.activeCount());

        SkillBarrageManager.INSTANCE.clear();

        assertEquals(0, SkillBarrageManager.INSTANCE.activeCount());
    }

    @Test
    void clearShouldIgnoreControllerCleanupFailures() {
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(8, true));
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage(8));

        assertDoesNotThrow(() -> SkillBarrageManager.INSTANCE.clear());
        assertEquals(0, SkillBarrageManager.INSTANCE.activeCount());
    }

    private static final class DummyBarrage implements Barrage {
        private final int maxTick;
        private final UUID uuid = UUID.randomUUID();
        private final BarrageOption option = new BarrageOption();
        private final HitBox hitBox = HitBox.of(1.0, 1.0, 1.0);
        private final DummyController controller;
        private Vec3 loc = Vec3.ZERO;
        private Vec3 direction = new Vec3(0.0, 0.0, 1.0);
        private boolean launch = true;
        private boolean valid = true;
        private int tick;
        private LivingEntity shooter;

        private DummyBarrage(int maxTick) {
            this(maxTick, false);
        }

        private DummyBarrage(int maxTick, boolean failOnCancel) {
            this.maxTick = maxTick;
            this.controller = new DummyController(failOnCancel);
        }

        @Override
        public Vec3 getLoc() {
            return loc;
        }

        @Override
        public void setLoc(Vec3 loc) {
            this.loc = loc;
        }

        @Override
        public ServerLevel getWorld() {
            return null;
        }

        @Override
        public HitBox getHitBox() {
            return hitBox;
        }

        @Override
        public void setHitBox(HitBox hitBox) {
        }

        @Override
        public LivingEntity getShooter() {
            return shooter;
        }

        @Override
        public void setShooter(LivingEntity shooter) {
            this.shooter = shooter;
        }

        @Override
        public Vec3 getDirection() {
            return direction;
        }

        @Override
        public void setDirection(Vec3 direction) {
            this.direction = direction;
        }

        @Override
        public boolean getLaunch() {
            return launch;
        }

        @Override
        public void setLaunch(boolean launch) {
            this.launch = launch;
        }

        @Override
        public boolean getValid() {
            return valid;
        }

        @Override
        public BarrageOption getOptions() {
            return option;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public ServerController<?> getBindControl() {
            return controller;
        }

        @Override
        public void hit(BarrageHitResult result) {
            valid = false;
            controller.cancel();
        }

        @Override
        public void onHit(BarrageHitResult result) {
        }

        @Override
        public boolean noclip() {
            return false;
        }

        @Override
        public void tick() {
            if (!launch || !valid) {
                return;
            }
            tick++;
            if (tick >= maxTick) {
                valid = false;
                controller.cancel();
            }
        }
    }

    private static final class DummyController implements ServerController<DummyController> {
        private final boolean failOnCancel;
        private boolean canceled;

        private DummyController(boolean failOnCancel) {
            this.failOnCancel = failOnCancel;
        }

        @Override
        public boolean getCanceled() {
            return canceled;
        }

        @Override
        public void cancel() {
            if (failOnCancel) {
                throw new IllegalStateException("boom");
            }
            canceled = true;
        }
    }
}
