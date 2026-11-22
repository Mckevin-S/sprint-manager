package com.mckevin.sprintmanager.report;

import com.mckevin.sprintmanager.activity.ActivityView;
import com.mckevin.sprintmanager.issue.IssueStatus;
import com.mckevin.sprintmanager.issue.IssueType;
import com.mckevin.sprintmanager.sprint.SprintDtos.SprintView;
import com.mckevin.sprintmanager.user.UserView;

import java.math.BigDecimal;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record BurndownView(SprintView sprint, List<BurndownCalculator.Point> points) {
    }

    public record VelocityEntry(Long sprintId, String sprintName, int committed, int completed) {
    }

    public record VelocityView(List<VelocityEntry> sprints, BigDecimal averageCompleted, BigDecimal reliability) {
    }

    public record StatusCount(IssueStatus status, String label, long issues, long points) {
    }

    public record TypeCount(IssueType type, String label, long issues) {
    }

    public record Workload(UserView member, long issues, long points, long inProgress) {
    }

    public record BacklogHealth(long issues, long estimatedPoints, long unestimatedStories) {
    }

    public record OverviewView(SprintView activeSprint, Integer completionPercent, List<StatusCount> statuses,
                               List<Workload> workload, List<TypeCount> openByType, BacklogHealth backlog,
                               List<ActivityView> recentActivity) {
    }
}
