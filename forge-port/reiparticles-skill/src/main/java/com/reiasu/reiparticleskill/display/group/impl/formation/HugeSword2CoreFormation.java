// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordFormationSigilEffectGroup;
import com.reiasu.reiparticleskill.display.group.impl.formation.effects.SwordRuneEffectGroup;
import com.reiasu.reiparticleskill.util.SwordFormationHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class HugeSword2CoreFormation extends ServerOnlyDisplayGroup {
    private final Player owner;
    private int age;

    public HugeSword2CoreFormation(Vec3 pos, Level world, Player owner) {
        super(pos, world);
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        Map<Supplier<Object>, RelativeLocation> result = new LinkedHashMap<>();
        result.put(() -> new HugeSword2CenterFormation(Vec3.ZERO, getWorld(), owner), new RelativeLocation());
        result.put(() -> new HugeSword2AroundFormation(Vec3.ZERO, getWorld(), owner), new RelativeLocation(0.0, -50.0, 0.0));
        result.put(() -> {
            SwordFormationSigilEffectGroup effect = new SwordFormationSigilEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(new Vec3(0.0, -1.0, 0.0));
            effect.setScale(1.35);
            effect.setOuterRingSamples(320);
            effect.setFastSpinSpeed(0.035);
            effect.setSlowSpinSpeed(0.020);
            return effect;
        }, new RelativeLocation());
        result.put(() -> {
            SwordRuneEffectGroup effect = new SwordRuneEffectGroup(Vec3.ZERO, getWorld());
            effect.setDirection(new Vec3(0.0, -1.0, 0.0));
            effect.setOuterRadius(10.0);
            effect.setRuneScale(1.35);
            effect.setRingSamples(120);
            return effect;
        }, new RelativeLocation());
        return result;
    }

    @Override
    public void tick() {
        if (owner == null || !owner.isAlive()) {
            remove();
            return;
        }

        teleportTo(owner.position().add(0.0, 60.0, 0.0));
        age++;
        if (age > 600) {
            remove();
            return;
        }

        Level level = getWorld();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB range = new AABB(getPos(), getPos()).inflate(256.0);
        List<LivingEntity> entities = new ArrayList<>(serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                range,
                it -> it.isAlive() && it != owner
        ));
        if (entities.isEmpty()) {
            return;
        }

        java.util.Collections.shuffle(entities);
        int limit = Math.min(5, entities.size());
        for (int i = 0; i < limit; i++) {
            SwordFormationHelper.INSTANCE.attackEntityFormation2(entities.get(i), owner);
        }
    }

    @Override
    public void onDisplay() {
        age = 0;
    }
}
