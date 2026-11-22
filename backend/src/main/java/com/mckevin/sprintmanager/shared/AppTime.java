package com.mckevin.sprintmanager.shared;

import java.time.ZoneId;

/** Fuseau de référence pour les dates métier (jours de sprint, burndown). */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private AppTime() {
    }
}
