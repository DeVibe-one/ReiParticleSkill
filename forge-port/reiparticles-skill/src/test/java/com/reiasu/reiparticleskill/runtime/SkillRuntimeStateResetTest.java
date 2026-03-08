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
package com.reiasu.reiparticleskill.runtime;

import com.reiasu.reiparticlesapi.barrages.Barrage;
import com.reiasu.reiparticlesapi.barrages.BarrageHitResult;
import com.reiasu.reiparticlesapi.barrages.BarrageOption;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.barrages.SkillBarrageManager;
import com.reiasu.reiparticleskill.display.group.ServerDisplayGroupManager;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnPhase;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnSnapshot;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnStateBridge;
import com.reiasu.reiparticleskill.end.respawn.EndRespawnWatcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRuntimeStateResetTest {
    @AfterEach
    void tearDown() throws Exception {
        SkillBarrageManager.INSTANCE.clear();
        ServerDisplayGroupManager.INSTANCE.clear();
        syntheticTrackers().clear();
    }

    @Test
    void resetShouldClearTrackedServerRuntimeState() throws Exception {
        SkillBarrageManager.INSTANCE.spawn(new DummyBarrage());
        ServerDisplayGroupManager.INSTANCE.spawn(new DummyDisplayGroup());
        EndRespawnStateBridge bridge = activeBridge();
        syntheticTrackers().put("test", newSyntheticTracker());

        SkillRuntimeStateReset.reset(bridge, LoggerFactory.getLogger(SkillRuntimeStateResetTest.class));

        assertEquals(0, SkillBarrageManager.INSTANCE.activeCount());
        assertTrue(ServerDisplayGroupManager.INSTANCE.getGroups().isEmpty());
        assertFalse(bridge.isActive());
        assertTrue(syntheticTrackers().isEmpty());
    }

    private static EndRespawnStateBridge activeBridge() throws Exception {
        EndRespawnStateBridge bridge = new EndRespawnStateBridge();
        Field snapshot = EndRespawnStateBridge.class.getDeclaredField("snapshot");
        snapshot.setAccessible(true);
        snapshot.set(bridge, new EndRespawnSnapshot("test", Vec3.ZERO, EndRespawnPhase.START, 0L));
        return bridge;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> syntheticTrackers() throws Exception {
        Field field = EndRespawnWatcher.class.getDeclaredField("SYNTHETIC_TRACKERS");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(null);
    }

    private static Object newSyntheticTracker() throws Exception {
        Class<?> type = Class.forName("com.reiasu.reiparticleskill.end.respawn.EndRespawnWatcher$SyntheticRespawnTracker");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static final class DummyDisplayGroup extends ServerOnlyDisplayGroup {
        private DummyDisplayGroup() {
            super(Vec3.ZERO, null);
        }

        @Override
        public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
            return Map.of();
        }

        @Override
        public void tick() {
        }

        @Override
        public void onDisplay() {
        }
    }

    private static final class DummyBarrage implements Barrage {
        private final UUID uuid = UUID.randomUUID();
        private final BarrageOption option = new BarrageOption();
        private final HitBox hitBox = HitBox.of(1.0, 1.0, 1.0);
        private final DummyController controller = new DummyController();
        private Vec3 loc = Vec3.ZERO;
        private Vec3 direction = new Vec3(0.0, 0.0, 1.0);
        private boolean launch;
        private boolean valid = true;
        private LivingEntity shooter;

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
        }
    }

    private static final class DummyController implements ServerController<DummyController> {
        private boolean canceled;

        @Override
        public boolean getCanceled() {
            return canceled;
        }

        @Override
        public void cancel() {
            canceled = true;
        }
    }
}
