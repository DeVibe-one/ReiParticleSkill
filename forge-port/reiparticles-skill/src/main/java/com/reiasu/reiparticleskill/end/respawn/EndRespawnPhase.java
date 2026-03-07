// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import java.util.Locale;
import java.util.Optional;

public enum EndRespawnPhase {
    START("start"),
    SUMMON_PILLARS("summon_pillars"),
    SUMMONING_DRAGON("summoning_dragon"),
    BEFORE_END_WAITING("before_end_waiting"),
    END("end");

    private final String id;

    EndRespawnPhase(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<EndRespawnPhase> fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT).trim();
        for (EndRespawnPhase value : values()) {
            if (value.id.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static Optional<EndRespawnPhase> fromStageName(String stageName) {
        String s = stageName.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (s.contains("START")) {
            return Optional.of(EndRespawnPhase.START);
        }
        if (s.contains("PREPARING") || s.contains("SUMMON_PILLARS") || s.contains("SUMMONING_PILLARS")) {
            return Optional.of(EndRespawnPhase.SUMMON_PILLARS);
        }
        if (s.contains("SUMMONING_DRAGON")) {
            return Optional.of(EndRespawnPhase.SUMMONING_DRAGON);
        }
        if (s.contains("WAIT")) {
            return Optional.of(EndRespawnPhase.BEFORE_END_WAITING);
        }
        if (s.contains("END")) {
            return Optional.of(EndRespawnPhase.END);
        }
        return Optional.empty();
    }
}
