// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.command.layout;

import java.util.Optional;

public record DisplayIndexPlan(
        int index,
        DisplayIndexKind kind,
        Optional<DisplaySpawnProfile> profile
) {
}
