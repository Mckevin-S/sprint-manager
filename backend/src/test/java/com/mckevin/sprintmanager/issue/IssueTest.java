package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.mckevin.sprintmanager.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTest {

    private Issue newIssue(IssueType type, Integer points) {
        return new Issue(TestData.project(), 7, type, "  Titre  ", null, null, points, TestData.user("Léa Martin"), 1000, NOW);
    }

    @Test
    @DisplayName("Un nouveau ticket est à faire, de priorité moyenne, avec une clé lisible")
    void defaults() {
        Issue issue = newIssue(IssueType.TASK, null);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.TODO);
        assertThat(issue.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(issue.getTitle()).isEqualTo("Titre");
        assertThat(issue.key()).isEqualTo("ORN-7");
    }

    @Test
    @DisplayName("Le workflow complet renseigne puis efface la date de résolution")
    void fullWorkflow() {
        Issue issue = newIssue(IssueType.STORY, 5);
        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW);
        issue.transitionTo(IssueStatus.IN_REVIEW, NOW);
        issue.transitionTo(IssueStatus.DONE, NOW);

        assertThat(issue.isDone()).isTrue();
        assertThat(issue.getResolvedAt()).isEqualTo(NOW);

        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW.plusSeconds(60));
        assertThat(issue.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("On ne saute pas la revue")
    void cannotSkipReview() {
        Issue issue = newIssue(IssueType.TASK, 2);
        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW);

        assertThatThrownBy(() -> issue.transitionTo(IssueStatus.DONE, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("TRANSITION_NOT_ALLOWED");
    }

    @Test
    @DisplayName("Une story non estimée ne peut pas être terminée")
    void storyMustBeEstimatedToBeDone() {
        Issue issue = newIssue(IssueType.STORY, null);
        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW);
        issue.transitionTo(IssueStatus.IN_REVIEW, NOW);

        assertThatThrownBy(() -> issue.transitionTo(IssueStatus.DONE, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("STORY_NOT_ESTIMATED");
    }

    @Test
    @DisplayName("Un bug non estimé peut être terminé")
    void bugWithoutPointsCanBeDone() {
        Issue issue = newIssue(IssueType.BUG, null);
        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW);
        issue.transitionTo(IssueStatus.IN_REVIEW, NOW);
        issue.transitionTo(IssueStatus.DONE, NOW);

        assertThat(issue.isDone()).isTrue();
        assertThat(issue.points()).isZero();
    }

    @Test
    void pointsFollowFibonacci() {
        assertThatThrownBy(() -> newIssue(IssueType.STORY, 4))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_STORY_POINTS");
        Issue issue = newIssue(IssueType.STORY, 3);
        issue.estimate(13, NOW);
        assertThat(issue.getStoryPoints()).isEqualTo(13);
    }

    @Test
    @DisplayName("Un ticket ne peut pas rejoindre un sprint clôturé")
    void cannotPlanIntoClosedSprint() {
        Sprint closed = new Sprint(TestData.project(), "Sprint 1", "Objectif", NOW);
        closed.start(null, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16), 10, NOW);
        closed.close(8, NOW);

        Issue issue = newIssue(IssueType.TASK, 1);
        assertThatThrownBy(() -> issue.planInto(closed, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SPRINT_CLOSED");
    }

    @Test
    @DisplayName("Un ticket reporté repart « à faire » dans sa nouvelle destination")
    void carryOverResetsStatus() {
        Issue issue = newIssue(IssueType.TASK, 2);
        issue.transitionTo(IssueStatus.IN_PROGRESS, NOW);

        issue.carryOver(null, 5000, NOW);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.TODO);
        assertThat(issue.getSprint()).isNull();
        assertThat(issue.getRank()).isEqualTo(5000);
    }
}
