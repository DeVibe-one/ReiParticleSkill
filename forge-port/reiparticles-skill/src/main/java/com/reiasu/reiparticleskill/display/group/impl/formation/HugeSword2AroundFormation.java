// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.impl.formation;

import com.reiasu.reiparticlesapi.animation.timeline.Eases;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticleskill.display.BarrageItemDisplay;
import com.reiasu.reiparticleskill.display.group.ServerOnlyDisplayGroup;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class HugeSword2AroundFormation extends ServerOnlyDisplayGroup {
    private final Player owner;
    private boolean shouldRemove;
    private int age;
    private int scaleMaxAge = 20;

    public HugeSword2AroundFormation(Vec3 pos, Level world, Player owner) {
        super(pos, world);
        this.owner = owner;
        setAxis(new RelativeLocation(0.0, 0.0, 1.0));
    }

    public Player getOwner() {
        return owner;
    }

    public int getScaleMaxAge() {
        return scaleMaxAge;
    }

    public void setScaleMaxAge(int scaleMaxAge) {
        this.scaleMaxAge = Math.max(1, scaleMaxAge);
    }

    @Override
    public Map<Supplier<Object>, RelativeLocation> getDisplayers() {
        Map<Supplier<Object>, RelativeLocation> result = new LinkedHashMap<>();
        result.put(() -> createPrimarySword(owner), new RelativeLocation(0.0, 0.0, -10.0));

        for (RelativeLocation point : new PointsBuilder().addCircle(60.0, 11).createWithoutClone()) {
            point.add(
                    ThreadLocalRandom.current().nextDouble(-20.0, 20.0),
                    15.0 + ThreadLocalRandom.current().nextDouble(-10.0, 10.0),
                    ThreadLocalRandom.current().nextDouble(-20.0, 20.0)
            );
            RelativeLocation copy = point.copy();
            result.put(() -> createSecondarySword(owner, 1), copy);
        }

        for (RelativeLocation point : new PointsBuilder().addCircle(90.0, 50).createWithoutClone()) {
            point.add(
                    ThreadLocalRandom.current().nextDouble(-50.0, 50.0),
                    70.0 + ThreadLocalRandom.current().nextDouble(-20.0, 20.0),
                    ThreadLocalRandom.current().nextDouble(-50.0, 50.0)
            );
            RelativeLocation copy = point.copy();
            result.put(() -> createSecondarySword(owner, 2), copy);
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
        Vec3 horizontalLook = owner.getLookAngle().multiply(1.0, 0.0, 1.0);
        RelativeLocation direction = horizontalLook.lengthSqr() > 1.0E-8
                ? RelativeLocation.of(horizontalLook.normalize())
                : new RelativeLocation(
                Math.cos(Math.toRadians(owner.getYRot() + 90.0F)),
                0.0,
                Math.sin(Math.toRadians(owner.getYRot() + 90.0F))
        ).normalize();
        rotateToPoint(direction);
        age++;

        double progress = clamp(age / (double) scaleMaxAge, 0.0, 1.0);
        double eased = Eases.outCubic.cal(progress);
        Vec3 center = getPos();

        for (Map.Entry<Object, RelativeLocation> entry : getDisplayed().entrySet()) {
            if (!(entry.getKey() instanceof BarrageItemDisplay display) || !display.getValid()) {
                continue;
            }
            RelativeLocation rel = entry.getValue();
            if (display.getSign() != 2) {
                display.setTargetYaw(owner.getYRot());
            }
            display.setPitch(-90.0F);
            display.setTargetPitch(-90.0F);

            if (shouldRemove) {
                display.setTargetScale(0.0F);
                if (display.getScale() <= 0.01F) {
                    display.remove();
                }
                continue;
            }

            float maxScale = switch (display.getSign()) {
                case 0 -> 28.0F;
                case 1, 2 -> 20.0F;
                default -> 20.0F;
            };
            display.setTargetScale(maxScale);
            display.teleportTo(center.add(rel.toVector().scale(eased)));
        }

        if (shouldRemove) {
            boolean allGone = true;
            for (Object key : getDisplayed().keySet()) {
                if (key instanceof BarrageItemDisplay display && display.getValid()) {
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
    public void onDisplay() {
        shouldRemove = false;
        age = 0;
    }

    @Override
    public void remove() {
        if (getDisplayed().isEmpty()) {
            super.remove();
            return;
        }
        shouldRemove = true;
    }

    private static BarrageItemDisplay createPrimarySword(Player owner) {
        BarrageItemDisplay display = new BarrageItemDisplay(Vec3.ZERO);
        display.setItem(new ItemStack(Items.IRON_SWORD));
        display.setScale(0.0F);
        display.setPreScale(0.0F);
        display.setScaledSpeed(4.0F);
        display.setPitch(-90.0F);
        display.setTargetPitch(-90.0F);
        float yaw = owner == null ? 0.0F : owner.getYRot();
        display.setYaw(yaw);
        display.setTargetYaw(yaw);
        return display;
    }

    private static BarrageItemDisplay createSecondarySword(Player owner, int sign) {
        BarrageItemDisplay display = new BarrageItemDisplay(Vec3.ZERO);
        display.setItem(new ItemStack(Items.IRON_SWORD));
        display.setScale(0.0F);
        display.setPreScale(0.0F);
        display.setScaledSpeed(4.0F);
        display.setPitch(-90.0F);
        display.setTargetPitch(-90.0F);
        if (sign == 1) {
            float yaw = owner == null ? 0.0F : owner.getYRot();
            display.setYaw(yaw);
            display.setTargetYaw(yaw);
        } else {
            float yaw = -180.0F + ThreadLocalRandom.current().nextFloat() * 360.0F;
            display.setYaw(yaw);
            display.setTargetYaw(yaw);
            display.setBlendCount(0);
        }
        display.setSign(sign);
        return display;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
