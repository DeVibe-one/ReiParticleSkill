// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class PackageDependencyRulesTest {
    private static final Path CORE_MAIN = Path.of("src/main/java/com/reiasu/reiparticlesapi");
    private static final Path SKILL_MAIN = Path.of("../reiparticles-skill/src/main/java/com/reiasu/reiparticleskill");
    private static final Path TESTKIT_MAIN = Path.of("../reiparticles-testkit/src/main/java");
    private static final String CATCH_THROWABLE = "catch (" + "Throwable";
    private static final String CATCH_THROWABLE_NO_SPACE = "catch(" + "Throwable";

    @Test
    void apiSourcesDoNotImportSkillPackages() throws IOException {
        List<String> violations = findImportViolations(CORE_MAIN, "import com.reiasu.reiparticleskill.");
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void apiSourcesDoNotImportForgePackages() throws IOException {
        List<String> violations = findImportViolations(CORE_MAIN, "import net.minecraftforge.");
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void skillSourcesDoNotImportScannerInternals() throws IOException {
        List<String> violations = new ArrayList<>();
        violations.addAll(findImportViolations(SKILL_MAIN, "import com.reiasu.reiparticlesapi.reflect."));
        violations.addAll(findImportViolations(SKILL_MAIN, "import com.reiasu.reiparticlesapi.event.ReiEventBus;"));
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void skillSourcesDoNotImportRuntimeOrForgePackages() throws IOException {
        List<String> violations = new ArrayList<>();
        violations.addAll(findImportViolations(SKILL_MAIN, "import com.reiasu.reiparticlesapi.runtime."));
        violations.addAll(findImportViolations(SKILL_MAIN, "import com.reiasu.reiparticleskill.ReiParticleSkillForge;"));
        violations.addAll(findImportViolations(SKILL_MAIN, "import net.minecraftforge."));
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void testkitSourcesDoNotImportRuntimeBootstrap() throws IOException {
        List<String> violations = new ArrayList<>();
        violations.addAll(findImportViolations(TESTKIT_MAIN, "import com.reiasu.reiparticlesapi.runtime."));
        violations.addAll(findImportViolations(TESTKIT_MAIN, "import com.reiasu.reiparticlesapi.ReiParticlesAPIForge;"));
        violations.addAll(findImportViolations(TESTKIT_MAIN, "import com.reiasu.reiparticleskill.ReiParticleSkillForge;"));
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    @Test
    void nonRuntimeSourcesDoNotCatchThrowable() throws IOException {
        List<String> violations = new ArrayList<>();
        violations.addAll(findTextViolations(CORE_MAIN, CATCH_THROWABLE));
        violations.addAll(findTextViolations(CORE_MAIN, CATCH_THROWABLE_NO_SPACE));
        violations.addAll(findTextViolations(SKILL_MAIN, CATCH_THROWABLE));
        violations.addAll(findTextViolations(SKILL_MAIN, CATCH_THROWABLE_NO_SPACE));
        violations.addAll(findTextViolations(TESTKIT_MAIN, CATCH_THROWABLE));
        violations.addAll(findTextViolations(TESTKIT_MAIN, CATCH_THROWABLE_NO_SPACE));
        assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
    }

    private static List<String> findImportViolations(Path root, String forbiddenImport) throws IOException {
        return findTextViolations(root, forbiddenImport);
    }

    private static List<String> findTextViolations(Path root, String forbiddenText) throws IOException {
        assertTrue(Files.exists(root), "Missing source root: " + root.toAbsolutePath());
        List<String> violations = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    List<String> lines = List.of(new String(Files.readAllBytes(path), StandardCharsets.UTF_8).split("\\R", -1));
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).contains(forbiddenText)) {
                            violations.add(path + ":" + (i + 1) + " -> " + forbiddenText);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return violations;
    }
}
