// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.group.ServerOnlyGrowingGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class HugeSword2CenterFormation extends ServerOnlyGrowingGroup {
    private final Player owner;
    private boolean shouldRemove;
    private final double velocityMax = 2.4;

    public HugeSword2CenterFormation(Vec3 pos, Level world, Player owner) {
        super(pos, world);
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public double getVelocityMax() {
        return velocityMax;
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        List<RelativeLocation> points = new ArrayList<>(new PointsBuilder().addCircle(32.0, 64).createWithoutClone());
        Map<Supplier<Object>, RelativeLocation> result = new LinkedHashMap<>();
        for (RelativeLocation point : points) {
            point.add(
                    ThreadLocalRandom.current().nextDouble(-16.0, 16.0),
                    ThreadLocalRandom.current().nextDouble(-3.0, 3.0),
                    ThreadLocalRandom.current().nextDouble(-16.0, 16.0)
            );
            RelativeLocation copy = point.copy();
            result.put(() -> createSwordDisplay(), copy);
        }
        return result;
    }

    @Override
    protected void displayEntry(Supplier<Object> key, RelativeLocation value) {
        java.util.Set<Object> before = new java.util.HashSet<>(getDisplayed().keySet());
        super.displayEntry(key, value);
        for (Object displayObj : getDisplayed().keySet()) {
            if (before.contains(displayObj)) {
                continue;
            }
            if (displayObj instanceof BarrageItemDisplay display) {
                display.teleportTo(getPos());
            }
            break;
        }
    }

    @Override
    public void tick() {
        if (!shouldRemove) {
            addMultiple(2);
        }
        rotateAsAxis(-Math.PI / 256.0);

        Vec3 center = getPos();
        for (Map.Entry<Object, RelativeLocation> entry : getDisplayed().entrySet()) {
            if (!(entry.getKey() instanceof BarrageItemDisplay display) || !display.getValid()) {
                continue;
            }

            RelativeLocation rel = entry.getValue();
            Vec3 target = center.add(rel.toVector());

            if (shouldRemove) {
                if (display.getSign() != 2) {
                    display.setSign(2);
                    Vec3 targetRelative = rel.toVector();
                    Vec3 tangent = targetRelative.cross(getAxis().toVector());
                    if (tangent.lengthSqr() < 1.0E-8) {
                        tangent = new Vec3(0.0, 1.0, 0.0);
                    }
                    display.setVelocity(targetRelative.scale(-0.5).add(tangent.normalize().scale(0.8)));
                }

                Vec3 pull = center.subtract(display.getPos());
                double pullLength = pull.length();
                if (pullLength < 0.8) {
                    display.remove();
                    continue;
                }
                Vec3 desiredPull = pull.normalize().scale(Math.min(3.2, 0.8 + pullLength * 0.28));
                Vec3 pullVelocity = display.getVelocity().scale(0.72).add(desiredPull.scale(0.28));
                if (pullVelocity.lengthSqr() > 3.2 * 3.2) {
                    pullVelocity = pullVelocity.normalize().scale(3.2);
                }
                display.setVelocity(pullVelocity);
                continue;
            }

            switch (display.getSign()) {
                case 0:
                    display.setTargetScale(8.0F);
                    Vec3 direction = target.subtract(display.getPos());
                    if (direction.lengthSqr() < 1.0E-8) {
                        display.setSign(1);
                        display.setVelocity(Vec3.ZERO);
                        display.setScale(8.0F);
                        break;
                    }
                    Vec3 desired = direction.normalize().scale(1.8);
                    Vec3 current = display.getVelocity();
                    Vec3 velocity = current.scale(0.5).add(desired);
                    double max = getVelocityMax();
                    if (velocity.lengthSqr() > max * max) {
                        velocity = velocity.normalize().scale(max);
                    }
                    Vec3 nextDirection = target.subtract(display.getPos().add(velocity));
                    display.setVelocity(velocity);
                    if (nextDirection.dot(direction) < 0.0 || direction.length() < 0.2) {
                        display.setSign(1);
                        display.setVelocity(Vec3.ZERO);
                        display.setScale(8.0F);
                    }
                    break;
                case 1:
                    display.teleportTo(target);
                    display.rotateToPoint(rel);
                    break;
                default:
                    break;
            }
        }

        if (shouldRemove) {
            boolean allGone = true;
            for (Object value : getDisplayed().keySet()) {
                if (value instanceof BarrageItemDisplay display && display.getValid()) {
                    allGone = false;
                    break;
                }
            }
            if (allGone) {
                super.remove();
            }
        }
    }

    @Override
    public void teleportTo(Vec3 to) {
        setPos(to);
    }

    @Override
    public void onDisplay() {
        shouldRemove = false;
    }

    @Override
    public void remove() {
        if (getDisplayed().isEmpty()) {
            super.remove();
            return;
        }
        shouldRemove = true;
    }

    private static BarrageItemDisplay createSwordDisplay() {
        BarrageItemDisplay display = new BarrageItemDisplay(Vec3.ZERO);
        display.setItem(new ItemStack(Items.IRON_SWORD));
        display.setScale(0.0F);
        display.setPreScale(0.0F);
        display.setScaledSpeed(2.0F);
        display.setSign(0);
        return display;
    }
}
