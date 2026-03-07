// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.reflect;

import java.lang.annotation.Annotation;
import java.util.HashSet;

/**
 * Lightweight class metadata holder storing a class name and its annotation names.
 * Used by classpath scanning to defer class loading until needed.
 */
public final class SimpleClassInfo {

    private final String type;
    private final HashSet<String> annotations;

    public SimpleClassInfo(String type, HashSet<String> annotations) {
        this.type = type;
        this.annotations = annotations;
    }

    public String getType() {
        return type;
    }

    public HashSet<String> getAnnotations() {
        return annotations;
    }

    /**
     * Checks whether this class info contains the given annotation type.
     */
    public boolean isAnnotationPresent(Class<? extends Annotation> anno) {
        return annotations.contains(anno.getName());
    }

    /**
     * Loads and returns the actual {@link Class} represented by this info.
     *
     * @throws ClassNotFoundException if the class cannot be found
     */
    public Class<?> toClass() throws ClassNotFoundException {
        return Class.forName(type);
    }
}
