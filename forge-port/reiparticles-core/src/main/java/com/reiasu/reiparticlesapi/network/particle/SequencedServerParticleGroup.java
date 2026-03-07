// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ServerParticleGroup} that manages sequenced (indexed) particles,
 * allowing individual particles to be toggled on/off by index.
 */
public abstract class SequencedServerParticleGroup extends ServerParticleGroup {

    private final long[] clientIndexStatus;
    private int serverSequencedParticleCount;

    public SequencedServerParticleGroup(double visibleRange) {
        super(visibleRange);
        this.clientIndexStatus = new long[(maxCount() + 63) / 64];
        this.serverSequencedParticleCount = 0;
    }

    public SequencedServerParticleGroup() {
        this(32.0);
    }

    public abstract int maxCount();

    public int getServerSequencedParticleCount() {
        return serverSequencedParticleCount;
    }

    public void setServerSequencedParticleCount(int count) {
        this.serverSequencedParticleCount = count;
    }

    public boolean isDisplayed(int index) {
        if (index < 0 || index >= maxCount()) return false;
        int word = index / 64;
        int bit = index % 64;
        return (clientIndexStatus[word] & (1L << bit)) != 0;
    }

    public void setDisplayed(int index, boolean status) {
        if (index < 0 || index >= maxCount()) return;
        int word = index / 64;
        int bit = index % 64;
        if (status) {
            clientIndexStatus[word] |= (1L << bit);
        } else {
            clientIndexStatus[word] &= ~(1L << bit);
        }
    }

    public Map.Entry<String, ParticleControllerDataBuffer<Integer>> toggleArgLeastIndex() {
        return Map.entry("least_index",
                ParticleControllerDataBuffers.INSTANCE.intValue(serverSequencedParticleCount));
    }

    public Map.Entry<String, ParticleControllerDataBuffer<long[]>> toggleArgStatus() {
        return Map.entry("index_status",
                ParticleControllerDataBuffers.INSTANCE.longArray(clientIndexStatus.clone()));
    }

    public void addSingle() {
        if (serverSequencedParticleCount >= maxCount()) return;
        setDisplayed(serverSequencedParticleCount, true);
        serverSequencedParticleCount++;

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {
            if (g instanceof SequencedServerParticleGroup sg) {
                sg.serverSequencedParticleCount = this.serverSequencedParticleCount;
            }
        }, args);
    }

    public void addMultiple(int count) {
        for (int i = 0; i < count; i++) {
            if (serverSequencedParticleCount >= maxCount()) break;
            setDisplayed(serverSequencedParticleCount, true);
            serverSequencedParticleCount++;
        }

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {
            if (g instanceof SequencedServerParticleGroup sg) {
                sg.serverSequencedParticleCount = this.serverSequencedParticleCount;
            }
        }, args);
    }

    public boolean removeSingle() {
        if (serverSequencedParticleCount <= 0) return false;
        serverSequencedParticleCount--;
        setDisplayed(serverSequencedParticleCount, false);

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {
            if (g instanceof SequencedServerParticleGroup sg) {
                sg.serverSequencedParticleCount = this.serverSequencedParticleCount;
            }
        }, args);
        return true;
    }

    public void removeMultiple(int count) {
        for (int i = 0; i < count; i++) {
            if (serverSequencedParticleCount <= 0) break;
            serverSequencedParticleCount--;
            setDisplayed(serverSequencedParticleCount, false);
        }

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {
            if (g instanceof SequencedServerParticleGroup sg) {
                sg.serverSequencedParticleCount = this.serverSequencedParticleCount;
            }
        }, args);
    }

    public void changeSingle(int index, boolean status) {
        setDisplayed(index, status);
        if (status && index >= serverSequencedParticleCount) {
            serverSequencedParticleCount = index + 1;
        }

        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {
            if (g instanceof SequencedServerParticleGroup sg) {
                sg.serverSequencedParticleCount = this.serverSequencedParticleCount;
            }
        }, args);
    }

    public void toggleCurrentCount() {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(toggleArgLeastIndex().getKey(), toggleArgLeastIndex().getValue());
        args.put(toggleArgStatus().getKey(), toggleArgStatus().getValue());
        change(g -> {}, args);
    }
}
