// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation.effects;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class SwordFormationWaveEffectGroup extends ServerOnlyDisplayGroup {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private int delay = 2;
    private int minCount = 16;
    private int maxCount = 32;
    private double minParticleSpeed = 0.5;
    private double maxParticleSpeed = 0.52;
    private int radianCountMin = 2;
    private int radianCountMax = 5;
    private double minRadian = Math.PI * 0.5;
    private double maxRadian = Math.PI * 2.0;
    private float alphaMin = 0.2F;
    private float alphaMax = 0.6F;
    private int minParticleAge = 40;
    private int maxParticleAge = 70;
    private double randomOffsetMin = 0.02;
    private double randomOffsetMax = 0.3;
    private double radianProgressMinSpeedScale = 0.4;
    private double radianProgressMaxSpeedScale = 1.2;
    private Vector3f randomColorLeft = new Vector3f(83.0F / 255.0F, 133.0F / 255.0F, 102.0F / 255.0F);
    private Vector3f randomColorRight = new Vector3f(106.0F / 255.0F, 193.0F / 255.0F, 156.0F / 255.0F);
    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);
    private int age;
    private int maxAge = -1;
    private boolean fading;
    private int fadeTick;
    private int fadeMaxTick = 20;

    public SwordFormationWaveEffectGroup(Vec3 pos, Level world) {
        super(pos, world);
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        return Collections.emptyMap();
    }

    public Vec3 getDirection() {
        return direction;
    }

    public void setDirection(Vec3 direction) {
        this.direction = FormationParticleHelper.safeDirection(direction);
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = Math.max(1, delay);
    }

    public int getMinCount() {
        return minCount;
    }

    public void setMinCount(int minCount) {
        this.minCount = Math.max(1, minCount);
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = Math.max(1, maxCount);
    }

    public double getMinParticleSpeed() {
        return minParticleSpeed;
    }

    public void setMinParticleSpeed(double minParticleSpeed) {
        this.minParticleSpeed = Math.max(0.001, minParticleSpeed);
    }

    public double getMaxParticleSpeed() {
        return maxParticleSpeed;
    }

    public void setMaxParticleSpeed(double maxParticleSpeed) {
        this.maxParticleSpeed = Math.max(0.001, maxParticleSpeed);
    }

    public int getRadianCountMin() {
        return radianCountMin;
    }

    public void setRadianCountMin(int radianCountMin) {
        this.radianCountMin = Math.max(1, radianCountMin);
    }

    public int getRadianCountMax() {
        return radianCountMax;
    }

    public void setRadianCountMax(int radianCountMax) {
        this.radianCountMax = Math.max(1, radianCountMax);
    }

    public double getMinRadian() {
        return minRadian;
    }

    public void setMinRadian(double minRadian) {
        this.minRadian = minRadian;
    }

    public double getMaxRadian() {
        return maxRadian;
    }

    public void setMaxRadian(double maxRadian) {
        this.maxRadian = maxRadian;
    }

    public float getAlphaMin() {
        return alphaMin;
    }

    public void setAlphaMin(float alphaMin) {
        this.alphaMin = alphaMin;
    }

    public float getAlphaMax() {
        return alphaMax;
    }

    public void setAlphaMax(float alphaMax) {
        this.alphaMax = alphaMax;
    }

    public int getMinParticleAge() {
        return minParticleAge;
    }

    public void setMinParticleAge(int minParticleAge) {
        this.minParticleAge = Math.max(1, minParticleAge);
    }

    public int getMaxParticleAge() {
        return maxParticleAge;
    }

    public void setMaxParticleAge(int maxParticleAge) {
        this.maxParticleAge = Math.max(1, maxParticleAge);
    }

    public double getRandomOffsetMin() {
        return randomOffsetMin;
    }

    public void setRandomOffsetMin(double randomOffsetMin) {
        this.randomOffsetMin = Math.max(0.0, randomOffsetMin);
    }

    public double getRandomOffsetMax() {
        return randomOffsetMax;
    }

    public void setRandomOffsetMax(double randomOffsetMax) {
        this.randomOffsetMax = Math.max(0.0, randomOffsetMax);
    }

    public double getRadianProgressMinSpeedScale() {
        return radianProgressMinSpeedScale;
    }

    public void setRadianProgressMinSpeedScale(double radianProgressMinSpeedScale) {
        this.radianProgressMinSpeedScale = radianProgressMinSpeedScale;
    }

    public double getRadianProgressMaxSpeedScale() {
        return radianProgressMaxSpeedScale;
    }

    public void setRadianProgressMaxSpeedScale(double radianProgressMaxSpeedScale) {
        this.radianProgressMaxSpeedScale = radianProgressMaxSpeedScale;
    }

    public Vector3f getRandomColorLeft() {
        return randomColorLeft;
    }

    public void setRandomColorLeft(Vector3f randomColorLeft) {
        this.randomColorLeft = randomColorLeft;
    }

    public Vector3f getRandomColorRight() {
        return randomColorRight;
    }

    public void setRandomColorRight(Vector3f randomColorRight) {
        this.randomColorRight = randomColorRight;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getFadeMaxTick() {
        return fadeMaxTick;
    }

    public void setFadeMaxTick(int fadeMaxTick) {
        this.fadeMaxTick = Math.max(1, fadeMaxTick);
    }

    @Override
    public void tick() {
        age++;
        if (maxAge > 0 && age > maxAge) {
            remove();
        }
        if (fading) {
            fadeTick++;
            if (fadeTick >= fadeMaxTick) {
                super.remove();
                return;
            }
        }
        if (delay > 1 && age % delay != 0) {
            return;
        }
        if (!(getWorld() instanceof ServerLevel level)) {
            return;
        }

        double fade = fading ? Math.max(0.0, 1.0 - fadeTick / (double) fadeMaxTick) : 1.0;
        int countLow = Math.min(minCount, maxCount);
        int countHigh = Math.max(minCount, maxCount);
        int spawnCount = Math.max(1, (int) (random.nextInt(countLow, countHigh + 1) * fade));
        int radianCountLow = Math.min(radianCountMin, radianCountMax);
        int radianCountHigh = Math.max(radianCountMin, radianCountMax);
        FormationParticleHelper.Basis basis = FormationParticleHelper.basis(direction);
        Vec3 center = getPos();

        for (int i = 0; i < spawnCount; i++) {
            int radianSegment = random.nextInt(radianCountLow, radianCountHigh + 1);
            for (int j = 0; j < radianSegment; j++) {
                double theta = FormationParticleHelper.randomDouble(0.0, Math.PI * 2.0);
                double radian = FormationParticleHelper.randomDouble(minRadian, maxRadian);
                Vec3 radial = basis.u().scale(Math.cos(theta)).add(basis.v().scale(Math.sin(theta))).normalize();
                Vec3 waveDir = basis.axis().scale(Math.cos(radian)).add(radial.scale(Math.sin(radian))).normalize();

                double speedBase = FormationParticleHelper.randomDouble(minParticleSpeed, maxParticleSpeed);
                double speedScale = FormationParticleHelper.randomDouble(radianProgressMinSpeedScale, radianProgressMaxSpeedScale);
                Vec3 velocity = waveDir.scale(speedBase * speedScale);

                double offsetLen = FormationParticleHelper.randomDouble(randomOffsetMin, randomOffsetMax);
                Vec3 offset = radial.scale(offsetLen).add(basis.axis().scale((random.nextDouble() - 0.5) * offsetLen * 0.5));
                Vec3 spawnPos = center.add(offset);

                float mix = random.nextFloat();
                Vector3f color = FormationParticleHelper.lerpColor(randomColorLeft, randomColorRight, mix);
                float alpha = (float) (FormationParticleHelper.randomFloat(alphaMin, alphaMax) * fade);
                float size = Math.max(0.08F, 0.55F * alpha);

                FormationParticleHelper.spawnMovingDust(level, spawnPos, velocity, color, size);
                if ((i + j) % 2 == 0) {
                    Vector3f accent = FormationParticleHelper.lerpColor(randomColorRight, randomColorLeft, 0.35F + mix * 0.3F);
                    FormationParticleHelper.spawnMovingDust(level, spawnPos, velocity.scale(0.7), accent, Math.max(0.05F, size * 0.7F));
                }
                if ((i + j) % 3 == 0) {
                    level.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 0,
                            velocity.x * 0.6, velocity.y * 0.6, velocity.z * 0.6, 1.0);
                }
            }
        }
    }

    @Override
    public void onDisplay() {
        age = 0;
        fading = false;
        fadeTick = 0;
    }

    @Override
    public void remove() {
        if (!fading) {
            fading = true;
            fadeTick = 0;
            return;
        }
        super.remove();
    }
}
