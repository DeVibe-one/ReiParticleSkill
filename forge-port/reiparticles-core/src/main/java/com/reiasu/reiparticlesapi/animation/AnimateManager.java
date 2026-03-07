// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnimateManager {
    public static final AnimateManager INSTANCE = new AnimateManager();
    private final List<Animate> serverActive = new ArrayList<>();
    private final List<Animate> clientActive = new ArrayList<>();

    private AnimateManager() {
    }

    public void displayAnimateServer(Animate animate) {
        if (animate == null) {
            return;
        }
        animate.start();
        synchronized (serverActive) {
            serverActive.add(animate);
        }
    }

    public void displayAnimateClient(Animate animate) {
        if (animate == null) {
            return;
        }
        animate.start();
        synchronized (clientActive) {
            clientActive.add(animate);
        }
    }

    public void tickServer() {
        synchronized (serverActive) {
            Iterator<Animate> iterator = serverActive.iterator();
            while (iterator.hasNext()) {
                Animate animate = iterator.next();
                animate.tick();
                if (animate.getDone()) {
                    iterator.remove();
                }
            }
        }
    }

    public void tickClient() {
        synchronized (clientActive) {
            Iterator<Animate> iterator = clientActive.iterator();
            while (iterator.hasNext()) {
                Animate animate = iterator.next();
                animate.tick();
                if (animate.getDone()) {
                    iterator.remove();
                }
            }
        }
    }

    public int activeCount() {
        synchronized (serverActive) {
            return serverActive.size();
        }
    }

    public int clientActiveCount() {
        synchronized (clientActive) {
            return clientActive.size();
        }
    }

    public void clear() {
        synchronized (serverActive) {
            for (Animate animate : serverActive) {
                animate.cancel();
            }
            serverActive.clear();
        }
        synchronized (clientActive) {
            for (Animate animate : clientActive) {
                animate.cancel();
            }
            clientActive.clear();
        }
    }
}
