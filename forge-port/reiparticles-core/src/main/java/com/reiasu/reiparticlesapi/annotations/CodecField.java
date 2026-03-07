// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field for inclusion in automatic codec serialization/deserialization.
 * <p>
 * Fields are serialized in ascending {@link #index()} order. Two fields with the
 * same index fall back to declaration order (which is JVM-dependent and fragile).
 * Always assign explicit indices when wire compatibility matters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CodecField {
    /**
     * Explicit serialization order index. Lower values are encoded first.
     * Default {@code 0} — fields sharing the same index are sorted by name as a fallback.
     */
    int index() default 0;
}
