// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class ServerOnlyGrowingGroup extends ServerOnlyDisplayGroup {
    private final List<Entry> displayData = new ArrayList<>();
    private int index;

    protected ServerOnlyGrowingGroup(Vec3 pos, Level world) {
        super(pos, world);
    }

    public List<Entry> getDisplayData() {
        return displayData;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.max(0, index);
    }

    @Override
    public void flush() {
        clear();
        displayAll();
    }

    @Override
    public void clear() {
        super.clear();
        displayData.clear();
        index = 0;
    }

    @Override
    public void displayAll() {
        displayData.clear();
        Map<Supplier<Object>, RelativeLocation> displayers = getDisplayers();
        if (displayers == null || displayers.isEmpty()) {
            return;
        }
        for (Map.Entry<Supplier<Object>, RelativeLocation> entry : displayers.entrySet()) {
            displayData.add(new Entry(entry.getKey(), entry.getValue() == null ? new RelativeLocation() : entry.getValue().copy()));
        }
    }

    public void addSingle() {
        if (index >= displayData.size()) {
            return;
        }
        Entry entry = displayData.get(index++);
        displayEntry(entry.supplier(), entry.location().copy());
    }

    public void addMultiple(int count) {
        int safeCount = Math.max(0, count);
        for (int i = 0; i < safeCount; i++) {
            addSingle();
        }
    }

    public record Entry(Supplier<Object> supplier, RelativeLocation location) {
    }
}
