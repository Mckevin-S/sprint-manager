package com.mckevin.sprintmanager.sprint;

import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.support.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.mckevin.sprintmanager.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SprintTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 16);

    private Sprint planned(String goal) {
        return new Sprint(TestData.project(), "Sprint 4", goal, NOW);
    }

    @Test
    void startAndClose() {
        Sprint sprint = planned(null);
        sprint.start("Livrer le suivi des dossiers", START, START.plusDays(14), 34, NOW);

        assertThat(sprint.isActive()).isTrue();
        assertThat(sprint.getCommittedPoints()).isEqualTo(34);
        assertThat(sprint.daysRemaining(START.plusDays(10))).isEqualTo(4);
        assertThat(sprint.daysRemaining(START.plusDays(20))).isZero();

        sprint.close(29, NOW.plusSeconds(3600));
        assertThat(sprint.isClosed()).isTrue();
        assertThat(sprint.getCompletedPoints()).isEqualTo(29);
        assertThat(sprint.daysRemaining(START)).isNull();
    }

    @Test
    void goalIsRequired() {
        assertThatThrownBy(() -> planned(" ").start(" ", START, START.plusDays(14), 10, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SPRINT_GOAL_REQUIRED");
    }

    @Test
    void goalFromPlanningIsKept() {
        Sprint sprint = planned("Objectif défini en planification");
        sprint.start(null, START, START.plusDays(7), 5, NOW);
        assertThat(sprint.getGoal()).isEqualTo("Objectif défini en planification");
    }

    @Test
    void datesMustBeConsistent() {
        assertThatThrownBy(() -> planned("Objectif").start(null, START, START, 10, NOW))
                .extracting("code").isEqualTo("INVALID_SPRINT_DATES");
        assertThatThrownBy(() -> planned("Objectif").start(null, START, START.plusDays(29), 10, NOW))
                .extracting("code").isEqualTo("SPRINT_TOO_LONG");
    }

    @Test
    void onlyPlannedSprintCanStartAndOnlyActiveCanClose() {
        Sprint sprint = planned("Objectif");
        assertThatThrownBy(() -> sprint.close(0, NOW)).extracting("code").isEqualTo("SPRINT_NOT_ACTIVE");
        sprint.start(null, START, START.plusDays(14), 10, NOW);
        assertThatThrownBy(() -> sprint.start(null, START, START.plusDays(14), 10, NOW))
                .extracting("code").isEqualTo("SPRINT_NOT_PLANNED");
    }

    @Test
    void closedSprintCannotBeEdited() {
        Sprint sprint = planned("Objectif");
        sprint.start(null, START, START.plusDays(14), 10, NOW);
        sprint.close(10, NOW);
        assertThatThrownBy(() -> sprint.edit("Nouveau nom", null)).extracting("code").isEqualTo("SPRINT_CLOSED");
    }
}
