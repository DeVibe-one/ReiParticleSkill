// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;

import java.util.List;
import java.util.Map;

public abstract class StatusHelper implements ParticleHelper {
    private int displayStatus = Status.ENABLE.id();
    private int closedInternal;
    private int current;

    public final int getDisplayStatus() {
        return displayStatus;
    }

    public final int getClosedInternal() {
        return closedInternal;
    }

    public final void setClosedInternal(int closedInternal) {
        this.closedInternal = Math.max(0, closedInternal);
    }

    public final int getCurrent() {
        return current;
    }

    protected final void setCurrent(int current) {
        this.current = Math.max(0, current);
    }

    public final void setStatus(int status) {
        this.displayStatus = status == Status.DISABLE.id() ? Status.DISABLE.id() : Status.ENABLE.id();
        changeStatus(this.displayStatus);
    }

    public final void setStatus(Status status) {
        setStatus(status.id());
    }

    public final Status getCurrentStatus() {
        return displayStatus == Status.DISABLE.id() ? Status.DISABLE : Status.ENABLE;
    }

    public final List<ArgPair> toArgsPairs() {
        return List.of(
                new ArgPair("display_status", ParticleControllerDataBuffers.INSTANCE.intValue(displayStatus)),
                new ArgPair("display_time", ParticleControllerDataBuffers.INSTANCE.intValue(current))
        );
    }

    public final void readFromServer(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        ParticleControllerDataBuffer<?> statusBuffer = args.get("display_status");
        if (statusBuffer != null && statusBuffer.getLoadedValue() instanceof Number number) {
            setStatus(number.intValue());
        }
        ParticleControllerDataBuffer<?> timeBuffer = args.get("display_time");
        if (timeBuffer != null && timeBuffer.getLoadedValue() instanceof Number number) {
            current = Math.max(0, number.intValue());
        }
    }

    public abstract void changeStatus(int status);

    public enum Status {
        ENABLE(1),
        DISABLE(2);

        private final int id;

        Status(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Status fromId(int id) {
            return id == DISABLE.id ? DISABLE : ENABLE;
        }
    }

    public record ArgPair(String name, ParticleControllerDataBuffer<?> value) {
    }
}
