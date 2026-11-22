package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.activity.ActivityView;
import com.mckevin.sprintmanager.user.UserView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class IssueDtos {

    private IssueDtos() {
    }

    public record CreateIssueRequest(
            @NotNull IssueType type,
            @NotBlank @Size(max = 160) String title,
            @Size(max = 10_000) String description,
            Priority priority,
            Integer storyPoints,
            Long assigneeId,
            Long sprintId) {
    }

    public record UpdateIssueRequest(
            @NotBlank @Size(max = 160) String title,
            @Size(max = 10_000) String description,
            @NotNull Priority priority,
            Integer storyPoints,
            Long assigneeId) {
    }

    public record TransitionRequest(@NotNull IssueStatus status) {
    }

    public record PlanRequest(Long sprintId) {
    }

    public record RankRequest(@NotEmpty List<Long> issueIds) {
    }

    public record CommentRequest(@NotBlank @Size(max = 4000) String body) {
    }

    public record IssueView(Long id, String key, IssueType type, String title, IssueStatus status, Priority priority,
                            Integer storyPoints, UserView assignee, Long sprintId, long rank, Instant updatedAt,
                            long version) {

        public static IssueView of(Issue i) {
            return new IssueView(i.getId(), i.key(), i.getType(), i.getTitle(), i.getStatus(), i.getPriority(),
                    i.getStoryPoints(), UserView.of(i.getAssignee()), i.getSprint() == null ? null : i.getSprint().getId(),
                    i.getRank(), i.getUpdatedAt(), i.getVersion());
        }
    }

    public record CommentView(Long id, UserView author, String body, Instant createdAt) {

        public static CommentView of(Comment c) {
            return new CommentView(c.getId(), UserView.of(c.getAuthor()), c.getBody(), c.getCreatedAt());
        }
    }

    public record IssueDetail(IssueView issue, String description, UserView reporter, String sprintName,
                              Instant createdAt, Instant resolvedAt, Set<IssueStatus> nextStatuses,
                              List<CommentView> comments, List<ActivityView> history) {
    }
}
