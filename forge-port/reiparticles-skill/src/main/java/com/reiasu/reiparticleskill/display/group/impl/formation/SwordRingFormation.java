// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.group.ServerOnlyGrowingGroup;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class SwordRingFormation extends ServerOnlyGrowingGroup {
    private final double rotationSpeed;
    private final Map<Supplier<Object>, RelativeLocation> points;
    private RelativeLocation direction = new RelativeLocation(0.0, 1.0, 0.0);
    private boolean allSet;
    private boolean shouldRemove;

    public SwordRingFormation(
            Vec3 pos,
            Level world,
            double rotationSpeed,
            Map<Supplier<Object>, RelativeLocation> points
    ) {
        super(pos, world);
        this.rotationSpeed = rotationSpeed;
        this.points = points;
    }

    public double getRotationSpeed() {
        return rotationSpeed;
    }

    public Map<Supplier<Object>, RelativeLocation> getPoints() {
        return points;
    }

    public RelativeLocation getDirection() {
        return direction.copy();
    }

    public void setDirection(RelativeLocation direction) {
        this.direction = direction == null ? new RelativeLocation(0.0, 1.0, 0.0) : direction.copy();
    }

    public boolean getAllSet() {
        return allSet;
    }

    public void setAllSet(boolean allSet) {
        this.allSet = allSet;
    }

    public boolean getShouldRemove() {
        return shouldRemove;
    }

    public void setShouldRemove(boolean shouldRemove) {
        this.shouldRemove = shouldRemove;
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        return points;
    }

    @Override
    public void tick() {
        if (getIndex() < getDisplayData().size()) {
            addMultiple(3);
        }

        if (!shouldRemove) {
            allSet = getIndex() == getDisplayData().size();
        }

        if (allSet && !shouldRemove) {
            rotateToWithAngle(direction, rotationSpeed);
            for (Map.Entry<Object, RelativeLocation> entry : getDisplayed().entrySet()) {
                if (entry.getKey() instanceof BarrageItemDisplay display) {
                    display.rotateToPoint(aimDirection(entry.getValue()));
                }
            }
            return;
        }

        Vec3 center = getPos();
        for (Map.Entry<Object, RelativeLocation> entry : getDisplayed().entrySet()) {
            if (!(entry.getKey() instanceof BarrageItemDisplay display)) {
                continue;
            }

            RelativeLocation rel = entry.getValue();
            Vec3 targetRelative = rel.toVector();
            Vec3 finalTarget = center.add(targetRelative);

            switch (display.getSign()) {
                case 0:
                    display.setRotateSpeed(30.0F);
                    display.teleportTo(center);
                    display.setSign(1);
                    if (targetRelative.lengthSqr() > 1.0E-8) {
                        Vec3 outward = targetRelative.normalize()
                                .scale(ThreadLocalRandom.current().nextDouble(1.8, 3.0));
                        display.setVelocity(outward);
                    } else {
                        display.setVelocity(Vec3.ZERO);
                    }
                    allSet = false;
                    break;
                case 1:
                    if (display.getAge() > 15) {
                        display.setSign(2);
                    }
                    allSet = false;
                    break;
                case 2:
                    display.setRotateSpeed(10.0F);
                    Vec3 toCenter = center.subtract(display.getPos());
                    if (toCenter.lengthSqr() < 1.0E-8) {
                        display.setSign(3);
                        break;
                    }

                    Vec3 desiredVel = toCenter.normalize().scale(2.0);
                    double finalSteer = lerp(clamp(toCenter.length() / 16.0, 0.0, 1.0), 1.0, 0.5);
                    Vec3 blended = display.getVelocity().scale(1.0 - finalSteer).add(desiredVel.scale(finalSteer));
                    Vec3 velocity = blended.lengthSqr() < 1.0E-8 ? desiredVel : blended.normalize().scale(2.0);
                    display.setVelocity(velocity);

                    Vec3 next = toCenter.subtract(velocity);
                    if (toCenter.dot(next) <= 0.0 || toCenter.lengthSqr() <= 1.0E-8) {
                        display.setSign(3);
                    }
                    allSet = false;
                    break;
                case 3:
                    Vec3 toFinal = finalTarget.subtract(display.getPos());
                    double dist = toFinal.length();
                    Vec3 curToTarget = display.getPos().subtract(finalTarget);
                    Vec3 nextToTarget = display.getPos().add(display.getVelocity()).subtract(finalTarget);
                    boolean passedTarget = curToTarget.dot(nextToTarget) <= 0.0;
                    if (passedTarget || dist <= 0.9) {
                        display.setSign(4);
                        display.setVelocity(Vec3.ZERO);
                        break;
                    }

                    Vec3 desiredToFinal = toFinal.normalize().scale(3.0);
                    Vec3 blendedToFinal = display.getVelocity().scale(0.25).add(desiredToFinal.scale(0.75));
                    Vec3 velToFinal = blendedToFinal.lengthSqr() < 1.0E-8
                            ? desiredToFinal
                            : blendedToFinal.normalize().scale(3.0);
                    display.setVelocity(velToFinal);
                    allSet = false;
                    break;
                case 4:
                    display.teleportTo(finalTarget);
                    display.rotateToPoint(aimDirection(rel));
                    if (!shouldRemove) {
                        break;
                    }
                    display.setSign(5);
                    Vec3 tangent = targetRelative.cross(direction.toVector());
                    if (tangent.lengthSqr() < 1.0E-8) {
                        tangent = new Vec3(0.0, 1.0, 0.0);
                    }
                    display.setVelocity(targetRelative.scale(-0.5).add(tangent.normalize().scale(0.8)));
                    break;
                case 5:
                    Vec3 pull = center.subtract(display.getPos());
                    double pullLen = pull.length();
                    if (pullLen < 0.8) {
                        display.remove();
                        break;
                    }
                    Vec3 desiredPull = pull.normalize().scale(Math.min(3.2, 0.8 + pullLen * 0.28));
                    Vec3 pullVel = display.getVelocity().scale(0.72).add(desiredPull.scale(0.28));
                    if (pullVel.lengthSqr() > 3.2 * 3.2) {
                        pullVel = pullVel.normalize().scale(3.2);
                    }
                    display.setVelocity(pullVel);
                    allSet = false;
                    break;
                default:
                    break;
            }
        }

        if (allSet && !shouldRemove) {
            for (Map.Entry<Object, RelativeLocation> entry : getDisplayed().entrySet()) {
                if (entry.getKey() instanceof BarrageItemDisplay display) {
                    display.setVelocity(Vec3.ZERO);
                    display.teleportTo(center.add(entry.getValue().toVector()));
                    display.rotateToPoint(entry.getValue());
                }
            }
        }

        if (shouldRemove) {
            boolean anyValid = false;
            for (Object key : getDisplayed().keySet()) {
                if (key instanceof BarrageItemDisplay display && display.getValid()) {
                    anyValid = true;
                    break;
                }
            }
            if (!anyValid) {
                super.remove();
            }
        }
    }

    @Override
    public void remove() {
        if (getDisplayed().isEmpty()) {
            super.remove();
            return;
        }
        shouldRemove = true;
    }

    @Override
    public void teleportTo(Vec3 to) {
        if (allSet) {
            super.teleportTo(to);
        } else {
            setPos(to);
        }
    }

    @Override
    public void onDisplay() {
        allSet = false;
        shouldRemove = false;
    }

    private RelativeLocation aimDirection(RelativeLocation rel) {
        RelativeLocation toRel = rel.copy();
        if (toRel.length() > 1.0E-8) {
            toRel.normalize();
        }
        RelativeLocation dir = direction.copy();
        if (dir.length() > 1.0E-8) {
            dir.normalize().scale(0.7);
        }
        return toRel.plus(dir);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }
}
