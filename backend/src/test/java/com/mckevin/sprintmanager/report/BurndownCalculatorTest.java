package com.mckevin.sprintmanager.report;

import com.mckevin.sprintmanager.report.BurndownCalculator.Change;
import com.mckevin.sprintmanager.report.BurndownCalculator.Point;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BurndownCalculatorTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate START = LocalDate.of(2026, 11, 16);
    private static final LocalDate END = START.plusDays(10);

    private static Instant on(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(PARIS).toInstant();
    }

    @Test
    @DisplayName("Travail restant jour par jour : terminés, ajout de périmètre, réouverture")
    void remainingWork() {
        List<Change> changes = List.of(
                new Change(on(START.plusDays(1), 16), -5),
                new Change(on(START.plusDays(2), 11), 3),
                new Change(on(START.plusDays(2), 17), -8),
                new Change(on(START.plusDays(3), 10), 2));

        List<Point> points = BurndownCalculator.compute(START, END, START.plusDays(3), 30, changes, PARIS);

        assertThat(points).hasSize(11);
        assertThat(points).extracting(Point::remaining).startsWith(30, 25, 20, 22).containsSequence(22, null);
        assertThat(points.get(4).remaining()).isNull();
    }

    @Test
    @DisplayName("La droite idéale descend de l'engagement à zéro")
    void idealLine() {
        List<Point> points = BurndownCalculator.compute(START, END, END, 40, List.of(), PARIS);

        assertThat(points.get(0).ideal()).isEqualByComparingTo("40.0");
        assertThat(points.get(5).ideal()).isEqualByComparingTo("20.0");
        assertThat(points.get(10).ideal()).isEqualByComparingTo("0.0");
        assertThat(points).allSatisfy(p -> assertThat(p.remaining()).isEqualTo(40));
    }

    @Test
    @DisplayName("Un changement avant le premier jour compte dès le premier jour")
    void changeBeforeStartCountsOnFirstDay() {
        List<Point> points = BurndownCalculator.compute(START, END, START,
                10, List.of(new Change(on(START.minusDays(2), 9), 3)), PARIS);

        assertThat(points.get(0).remaining()).isEqualTo(13);
    }

    @Test
    @DisplayName("Le fuseau de Paris décide du jour d'un changement tardif")
    void usesParisDay() {
        // 00h30 à Paris le deuxième jour = encore le premier jour en UTC
        Instant afterMidnight = START.plusDays(1).atTime(0, 30).atZone(PARIS).toInstant();
        List<Point> points = BurndownCalculator.compute(START, END, START.plusDays(1),
                10, List.of(new Change(afterMidnight, -4)), PARIS);

        assertThat(points.get(0).remaining()).isEqualTo(10);
        assertThat(points.get(1).remaining()).isEqualTo(6);
    }

    @Test
    void invalidDatesGiveEmptyChart() {
        assertThat(BurndownCalculator.compute(END, START, END, 10, List.of(), PARIS)).isEmpty();
        assertThat(BurndownCalculator.compute(null, END, END, 10, List.of(), PARIS)).isEmpty();
    }
}
