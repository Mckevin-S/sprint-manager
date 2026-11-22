package com.mckevin.sprintmanager.support;

import com.mckevin.sprintmanager.project.Project;
import com.mckevin.sprintmanager.user.User;

import java.time.Instant;

/** Fabriques d'objets métier pour les tests unitaires. */
public final class TestData {

    public static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");

    private TestData() {
    }

    public static Project project() {
        return new Project("ORN", "Orion", null, NOW);
    }

    public static User user(String name) {
        return new User(name.toLowerCase().replace(' ', '.') + "@test.fr", name, "hash", NOW);
    }
}
