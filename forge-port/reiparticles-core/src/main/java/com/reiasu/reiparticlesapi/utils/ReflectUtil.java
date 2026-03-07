// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Reflection and timing utilities.
 * <p>
 * Provides class references for common Minecraft types (useful for reflection-based
 * annotation processing) and timed execution helpers that log elapsed time
 * to the mod logger.
 */
public final class ReflectUtil {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ReflectUtil() {
    }

    /**
     * Runs a {@link Runnable} and logs the elapsed time in milliseconds.
     *
     * @param name    the operation name for logging
     * @param invoker the code to run
     */
    public static void infoTimeWith(String name, Runnable invoker) {
        long start = System.currentTimeMillis();
        invoker.run();
        long end = System.currentTimeMillis();
        LOGGER.info("Executed {} completed: took {}ms", name, (end - start));
    }

    /**
     * Overload with default empty name.
     */
    public static void infoTimeWith(Runnable invoker) {
        infoTimeWith("", invoker);
    }

    /**
     * Runs a {@link Supplier} and logs the elapsed time, returning the result.
     *
     * @param name    the operation name for logging
     * @param invoker the code to run
     * @param <T>     return type
     * @return the result of the supplier
     */
    public static <T> T infoTimeCallable(String name, Supplier<T> invoker) {
        long start = System.currentTimeMillis();
        T result = invoker.get();
        long end = System.currentTimeMillis();
        LOGGER.info("Executed and returned {} completed: took {}ms", name, (end - start));
        return result;
    }

    /**
     * Overload with default empty name.
     */
    public static <T> T infoTimeCallable(Supplier<T> invoker) {
        return infoTimeCallable("", invoker);
    }
}

