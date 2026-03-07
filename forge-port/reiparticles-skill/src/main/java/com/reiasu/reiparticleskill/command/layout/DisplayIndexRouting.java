// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command.layout;

import java.util.Optional;

public final class DisplayIndexRouting {
    private DisplayIndexRouting() {
    }

    public static Optional<DisplayIndexPlan> plan(int index) {
        return switch (index) {
            case 0 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.EMITTER, Optional.empty()));
            case 1 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.STYLE, Optional.empty()));
            case 2 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.ENTITY, DisplayCommandLayout.profileForIndex(index)));
            case 3 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.DISPLAY, DisplayCommandLayout.profileForIndex(index)));
            case 4 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.GROUP, Optional.empty()));
            case 5 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.COMPOSITION, Optional.empty()));
            case 6 -> Optional.of(new DisplayIndexPlan(index, DisplayIndexKind.DISPLAY, DisplayCommandLayout.profileForIndex(index)));
            default -> Optional.empty();
        };
    }

    public static boolean usesGroupSpawn(int index) {
        return index == 4;
    }

    public static boolean usesCompositionSpawn(int index) {
        return index == 5;
    }

    public static Optional<DisplaySpawnProfile> spawnProfile(int index) {
        return DisplayCommandLayout.profileForIndex(index);
    }
}
