// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

/**
 * Utility methods for reading dragon-respawn state from {@link EndDragonFight}
 * via reflection (Forge 1.20.x mappings).
 */
public final class EndDragonFightHelper {

    private EndDragonFightHelper() {
    }

    public static Optional<EndRespawnPhase> detectPhase(EndDragonFight fight) {
        Object stage = readRespawnStage(fight);
        if (stage == null) {
            return detectByRespawnTimer(fight);
        }
        if (stage instanceof Enum<?> enumStage) {
            Optional<EndRespawnPhase> byName = EndRespawnPhase.fromStageName(enumStage.name());
            if (byName.isPresent()) {
                return byName;
            }
            Optional<EndRespawnPhase> byOrdinal = mapByOrdinal(enumStage.ordinal());
            if (byOrdinal.isPresent()) {
                return byOrdinal;
            }
        }

        Object enumName = invokeNoArg(stage, "name");
        if (enumName != null) {
            Optional<EndRespawnPhase> byName = EndRespawnPhase.fromStageName(enumName.toString());
            if (byName.isPresent()) {
                return byName;
            }
        }
        Optional<EndRespawnPhase> byToString = EndRespawnPhase.fromStageName(stage.toString());
        if (byToString.isPresent()) {
            return byToString;
        }
        return detectByRespawnTimer(fight);
    }

    public static Vec3 portalCenter(EndDragonFight fight) {
        Object portal = invokeNoArg(fight, "getPortalLocation");
        if (portal instanceof BlockPos pos) {
            return Vec3.atCenterOf(pos);
        }

        Object portalLocation = readField(fight, "portalLocation", "f_64072_", "origin", "f_286985_");
        if (portalLocation instanceof BlockPos pos) {
            return Vec3.atCenterOf(pos);
        }
        Object origin = readField(fight, "origin", "f_64061_");
        if (origin instanceof BlockPos pos) {
            double y = pos.getY() > 8 ? pos.getY() + 0.5 : 64.5;
            return new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5);
        }
        return new Vec3(0.0, 64.5, 0.0);
    }

    private static Optional<EndRespawnPhase> detectByRespawnTimer(EndDragonFight fight) {
        Object crystals = readField(fight, "respawnCrystals", "f_64075_");
        if (!(crystals instanceof Collection<?> collection) || collection.isEmpty()) {
            return Optional.empty();
        }

        Integer respawnTime = readIntField(fight, "respawnTime", "f_64074_");
        if (respawnTime == null) {
            return Optional.of(EndRespawnPhase.START);
        }
        int t = Math.max(0, respawnTime);
        if (t < 80) {
            return Optional.of(EndRespawnPhase.START);
        }
        if (t < 520) {
            return Optional.of(EndRespawnPhase.SUMMON_PILLARS);
        }
        if (t < 620) {
            return Optional.of(EndRespawnPhase.SUMMONING_DRAGON);
        }
        return Optional.of(EndRespawnPhase.END);
    }

    private static Optional<EndRespawnPhase> mapByOrdinal(int ordinal) {
        return switch (ordinal) {
            case 0 -> Optional.of(EndRespawnPhase.START);
            case 1, 2 -> Optional.of(EndRespawnPhase.SUMMON_PILLARS);
            case 3 -> Optional.of(EndRespawnPhase.SUMMONING_DRAGON);
            case 4 -> Optional.of(EndRespawnPhase.END);
            default -> Optional.empty();
        };
    }

    private static Object readRespawnStage(EndDragonFight fight) {
        Object byName = readField(fight, "respawnStage", "dragonRespawnStage", "f_64073_");
        if (byName != null) {
            return byName;
        }

        for (Field field : fight.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!field.getType().isEnum()) {
                continue;
            }
            String typeName = field.getType().getSimpleName();
            if (!typeName.toLowerCase().contains("respawn")) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(fight);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Object readField(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Integer readIntField(Object target, String... names) {
        Object value = readField(target, names);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
