// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerDisplayGroupManager {
    public static final ServerDisplayGroupManager INSTANCE = new ServerDisplayGroupManager();
    private static final Logger LOGGER = LogUtils.getLogger();

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
            boolean discard = false;
            try {
                group.tick();
            } catch (RuntimeException e) {
                LOGGER.warn("Display group {} ({}) failed during server tick; removing group",
                        group.getUuid(), group.getClass().getName(), e);
                safeRemove(group);
                discard = true;
            }
            if (discard || group.getCanceled()) {
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
            safeRemove(group);
        }
        groups.clear();
    }

    private void safeRemove(ServerOnlyDisplayGroup group) {
        if (group == null) {
            return;
        }
        try {
            group.remove();
        } catch (RuntimeException e) {
            LOGGER.debug("Failed to remove display group during cleanup", e);
        }
    }
}

