// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.reflect;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReiAPIScannerTest {
    @AfterEach
    void cleanup() {
        ReiAPIScanner.INSTANCE.clear();
    }

    @Test
    void shouldReturnAnnotatedClassesInStableTypeOrder() {
        ReiAPIScanner.INSTANCE.inputScanResult(info("com.example.ZetaPort"));
        ReiAPIScanner.INSTANCE.inputScanResult(info("com.example.AlphaPort"));
        ReiAPIScanner.INSTANCE.inputScanResult(info("com.example.BetaPort"));

        List<String> discovered = ReiAPIScanner.INSTANCE.getWithAnnotation(ReiAutoRegister.class).stream()
                .map(SimpleClassInfo::getType)
                .toList();

        assertEquals(List.of(
                "com.example.AlphaPort",
                "com.example.BetaPort",
                "com.example.ZetaPort"
        ), discovered);
    }

    @Test
    void shouldAllowConcurrentReadsWhileScanResultsAreAdded() throws Exception {
        int writes = 1000;
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean writing = new AtomicBoolean(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> writer = executor.submit(() -> {
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to start scanner writer");
                    }
                    for (int i = 0; i < writes; i++) {
                        ReiAPIScanner.INSTANCE.inputScanResult(info("com.example.Concurrent" + i));
                        if ((i & 7) == 0) {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted while writing scan results", e);
                } finally {
                    writing.set(false);
                }
            });
            Future<?> reader = executor.submit(() -> {
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to start scanner reader");
                    }
                    while (writing.get()) {
                        ReiAPIScanner.INSTANCE.getWithAnnotation(ReiAutoRegister.class);
                    }
                    ReiAPIScanner.INSTANCE.getWithAnnotation(ReiAutoRegister.class);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted while reading scan results", e);
                }
            });

            start.countDown();
            writer.get(5, TimeUnit.SECONDS);
            reader.get(5, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertEquals(writes, ReiAPIScanner.INSTANCE.getWithAnnotation(ReiAutoRegister.class).size());
        assertTrue(ReiAPIScanner.INSTANCE.getWithAnnotation(ReiAutoRegister.class).stream()
                .map(SimpleClassInfo::getType)
                .allMatch(type -> type.startsWith("com.example.Concurrent")));
    }

    private static SimpleClassInfo info(String typeName) {
        return new SimpleClassInfo(typeName, new HashSet<>(Set.of(ReiAutoRegister.class.getName())));
    }
}
