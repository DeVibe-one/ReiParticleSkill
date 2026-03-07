// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.key;

public enum KeyActionType {
    SINGLE_CLICK(0),
    DOUBLE_CLICK(1),
    LONG_PRESS(2);

    private final int id;

    KeyActionType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static KeyActionType fromId(int id) {
        for (KeyActionType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        return SINGLE_CLICK;
    }
}

