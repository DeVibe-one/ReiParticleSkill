// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * An ordered queue of {@link ParticleCommand}s with optional execution predicates.
 * <p>
 * Each entry pairs a command with a predicate that decides whether it should fire
 * for a given particle on each tick.
 */
public final class ParticleCommandQueue {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Predicate that receives (command, data, particle) and returns true if the command should execute.
     */
    @FunctionalInterface
    public interface CommandPredicate {
        boolean test(ParticleCommand command, ControllableParticleData data, ControllableParticle particle);
    }

    private static final CommandPredicate ALWAYS_TRUE = (cmd, data, particle) -> true;

    private final Deque<Entry> commands = new ArrayDeque<>();

    public Deque<Entry> getCommands() {
        return commands;
    }

    /**
     * Iterate all commands, executing those whose predicate passes.
     */
    public void applyVelocity(ControllableParticleData data, ControllableParticle particle) {
        for (Entry entry : commands) {
            if (entry.predicate.test(entry.command, data, particle)) {
                entry.command.execute(data, particle);
            }
        }
    }

    /**
     * Apply a mutator to the command at the given index (0-based).
     * Out-of-bounds indices are silently ignored.
     */
    public <T extends ParticleCommand> ParticleCommandQueue updateWith(int index, Consumer<T> applier) {
        int i = 0;
        for (Entry entry : commands) {
            if (i == index) {
                try {
                    @SuppressWarnings("unchecked")
                    T typed = (T) entry.command;
                    applier.accept(typed);
                } catch (ClassCastException e) {
                    LOGGER.debug("Command type mismatch at index {}: {}", index, e.getMessage());
                }
                break;
            }
            i++;
        }
        return this;
    }

    /**
     * Apply a mutator to every command that is an instance of the given type.
     */
    public <T extends ParticleCommand> ParticleCommandQueue updateWithTypes(Class<T> type, Consumer<T> applier) {
        for (Entry entry : commands) {
            if (type.isInstance(entry.command)) {
                applier.accept(type.cast(entry.command));
            }
        }
        return this;
    }

    /**
     * Add a command that always executes.
     */
    public ParticleCommandQueue add(ParticleCommand command) {
        commands.add(new Entry(command, ALWAYS_TRUE));
        return this;
    }

    /**
     * Add a command with a custom execution predicate.
     */
    public ParticleCommandQueue add(ParticleCommand command, CommandPredicate predicate) {
        commands.add(new Entry(command, predicate));
        return this;
    }

    /**
     * Internal storage for a command + its predicate.
     */
    public static final class Entry {
        public final ParticleCommand command;
        public final CommandPredicate predicate;

        public Entry(ParticleCommand command, CommandPredicate predicate) {
            this.command = command;
            this.predicate = predicate;
        }
    }
}
