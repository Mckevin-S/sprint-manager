package com.mckevin.sprintmanager.sprint;

import com.mckevin.sprintmanager.issue.IssueDtos.IssueView;
import com.mckevin.sprintmanager.issue.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class SprintDtos {

    private SprintDtos() {
    }

    public record CreateSprintRequest(@Size(max = 60) String name, @Size(max = 300) String goal) {
    }

    public record UpdateSprintRequest(@NotBlank @Size(max = 60) String name, @Size(max = 300) String goal) {
    }

    public record StartSprintRequest(@Size(max = 300) String goal, @NotNull LocalDate startDate,
                                     @NotNull LocalDate endDate) {
    }

    /** Destination des tickets non terminés : un sprint planifié, ou le backlog si nul. */
    public record CloseSprintRequest(Long moveToSprintId) {
    }

    public record SprintView(Long id, String name, String goal, SprintStatus status, LocalDate startDate,
                             LocalDate endDate, Integer committedPoints, Integer completedPoints, long issueCount,
                             long totalPoints, long donePoints, Long daysRemaining) {
    }

    public record BoardColumn(IssueStatus status, String label, List<IssueView> issues, int points) {
    }

    public record BoardView(SprintView sprint, int wipLimit, List<BoardColumn> columns) {
    }

    public record CloseReport(SprintView sprint, int carriedOver, String carriedTo) {
    }
}
