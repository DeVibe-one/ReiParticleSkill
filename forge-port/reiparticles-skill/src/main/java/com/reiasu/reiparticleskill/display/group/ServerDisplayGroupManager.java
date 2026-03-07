// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerDisplayGroupManager {
    public static final ServerDisplayGroupManager INSTANCE = new ServerDisplayGroupManager();
    private final Set<ServerOnlyDisplayGroup> groups = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ServerDisplayGroupManager() {
    }

    public Set<ServerOnlyDisplayGroup> getGroups() {
        return groups;
    }

    public void doTick() {
        Iterator<ServerOnlyDisplayGroup> iterator = groups.iterator();
        while (iterator.hasNext()) {
            ServerOnlyDisplayGroup group = iterator.next();
            group.tick();
            if (group.getCanceled()) {
                iterator.remove();
            }
        }
    }

    public void spawn(ServerOnlyDisplayGroup group) {
        if (group == null) {
            return;
        }
        group.display();
        groups.add(group);
    }

    public void clear() {
        for (ServerOnlyDisplayGroup group : groups) {
            group.remove();
        }
        groups.clear();
    }
}
