package com.mckevin.sprintmanager.issue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class IssueWorkflowTest {

    @ParameterizedTest(name = "{0} → {1} : {2}")
    @CsvSource({
            "TODO, IN_PROGRESS, true",
            "TODO, IN_REVIEW, false",
            "TODO, DONE, false",
            "IN_PROGRESS, TODO, true",
            "IN_PROGRESS, IN_REVIEW, true",
            "IN_PROGRESS, DONE, false",
            "IN_REVIEW, IN_PROGRESS, true",
            "IN_REVIEW, DONE, true",
            "DONE, IN_PROGRESS, true",
            "DONE, TODO, false"
    })
    void transitions(IssueStatus from, IssueStatus to, boolean allowed) {
        assertThat(IssueWorkflow.canTransition(from, to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @CsvSource({"TODO", "IN_PROGRESS", "IN_REVIEW", "DONE"})
    void everyStatusHasAWayOut(IssueStatus status) {
        assertThat(IssueWorkflow.nextStatuses(status)).isNotEmpty().doesNotContain(status);
    }
}
