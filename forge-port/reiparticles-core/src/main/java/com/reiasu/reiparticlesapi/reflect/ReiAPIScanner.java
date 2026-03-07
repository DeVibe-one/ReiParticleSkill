// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.reflect;

import com.mojang.logging.LogUtils;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classpath scanner using ClassGraph that discovers annotated classes at runtime.
 * <p>
 * Packages to scan must be registered via {@link #registerPackage(Class)} or
 * {@link #registerPackage(String)} before calling {@link #scan()}.
 */
public final class ReiAPIScanner {

    public static final ReiAPIScanner INSTANCE = new ReiAPIScanner();
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final HashSet<String> scanPackages = new HashSet<>();
    private boolean loaded = false;
    private final HashSet<SimpleClassInfo> classes = new HashSet<>();

    private ReiAPIScanner() {
    }

    /**
     * Perform the classpath scan for all currently registered packages.
     */
    public void scan() {
        loaded = true;

        try {
            long start = System.currentTimeMillis();
            LOGGER.info("Starting ClassGraph scan...");

            String[] packages = scanPackages.toArray(new String[0]);
            try (ScanResult scanResult = new ClassGraph()
                    .enableClassInfo()
                    .enableAnnotationInfo()
                    .acceptPackages(packages)
                    .scan()) {

                ClassInfoList allClasses = scanResult.getAllClasses()
                        .filter(ci -> !ci.getAnnotations().isEmpty());

                for (ClassInfo ci : allClasses) {
                    String className = ci.getName();
                    HashSet<String> annotationNames = ci.getAnnotations().stream()
                            .map(ClassInfo::getName)
                            .collect(Collectors.toCollection(HashSet::new));
                    classes.add(new SimpleClassInfo(className, annotationNames));
                }
            }

            long end = System.currentTimeMillis();
            LOGGER.info("ClassGraph scan completed in {}ms", (end - start));
        } catch (LinkageError e) {
            LOGGER.warn("ClassGraph not available; falling back to explicit listener registration only. "
                    + "Use registerListenerInstance() or registerAnnotatedClass() instead.", e);
        }
    }

    /**
     * Return all scanned classes that carry the given annotation.
     */
    public Collection<SimpleClassInfo> getWithAnnotation(Class<? extends Annotation> anno) {
        List<SimpleClassInfo> out = new ArrayList<>();
        for (SimpleClassInfo info : classes) {
            if (info.isAnnotationPresent(anno)) {
                out.add(info);
            }
        }
        return out;
    }

    /**
     * Return loaded {@link Class} objects for all scanned classes that carry the given annotation.
     */
    public Collection<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> anno) {
        List<Class<?>> out = new ArrayList<>();
        for (SimpleClassInfo info : classes) {
            if (info.isAnnotationPresent(anno)) {
                try {
                    out.add(info.toClass());
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Failed to load class: {}", info.getType(), e);
                }
            }
        }
        return out;
    }

    /**
     * Manually add a scan result (used internally during scan, or for testing).
     */
    public void inputScanResult(SimpleClassInfo scanResult) {
        classes.add(scanResult);
    }

    /**
     * Mark the scanner as loaded without performing a scan.
     * Used by NeoForge/Forge mod-scan bootstrapping that provides class info externally.
     */
    public void neoLoaded() {
        loaded = true;
    }

    public void clear() {
        scanPackages.clear();
        classes.clear();
        loaded = false;
    }

    /**
     * Register a package to scan, derived from the given class's package.
     */
    public int getScannedPackageCount() {
        return scanPackages.size();
    }

    public static void registerPackage(Class<?> main) {
        String packageName = main.getPackageName();
        registerPackage(packageName);
    }

    /**
     * Register a package name to include in the ClassGraph scan.
     */
    public static void registerPackage(String packageName) {
        INSTANCE.scanPackages.add(packageName);
        LOGGER.info("Registered scan package: {}", packageName);
    }
}

