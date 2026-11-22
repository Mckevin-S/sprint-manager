package com.mckevin.sprintmanager.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul pur du burndown d'un sprint, sans accès à la base.
 *
 * <p>Le travail restant d'un jour J vaut l'engagement initial plus la somme des variations survenues
 * jusqu'à la fin de J (ajouts de périmètre, tickets terminés ou rouverts, réestimations).
 * Les variations antérieures au premier jour sont rattachées au premier jour.
 * Les jours futurs n'ont pas de valeur réelle, seulement la droite idéale.</p>
 */
public final class BurndownCalculator {

    public record Change(Instant at, int delta) {
    }

    public record Point(LocalDate date, Integer remaining, BigDecimal ideal) {
    }

    private BurndownCalculator() {
    }

    public static List<Point> compute(LocalDate start, LocalDate end, LocalDate lastKnownDay, int committed,
                                      List<Change> changes, ZoneId zone) {
        if (start == null || end == null || end.isBefore(start)) {
            return List.of();
        }
        long length = ChronoUnit.DAYS.between(start, end);
        List<Point> points = new ArrayList<>();
        for (long i = 0; i <= length; i++) {
            LocalDate day = start.plusDays(i);
            Integer remaining = day.isAfter(lastKnownDay) ? null : remainingAt(day, start, committed, changes, zone);
            BigDecimal ideal = length == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(committed)
                    .multiply(BigDecimal.valueOf(length - i))
                    .divide(BigDecimal.valueOf(length), 1, RoundingMode.HALF_UP);
            points.add(new Point(day, remaining, ideal));
        }
        return points;
    }

    private static int remainingAt(LocalDate day, LocalDate start, int committed, List<Change> changes, ZoneId zone) {
        int remaining = committed;
        for (Change change : changes) {
            LocalDate changeDay = change.at().atZone(zone).toLocalDate();
            if (changeDay.isBefore(start)) {
                changeDay = start;
            }
            if (!changeDay.isAfter(day)) {
                remaining += change.delta();
            }
        }
        return Math.max(remaining, 0);
    }
}
