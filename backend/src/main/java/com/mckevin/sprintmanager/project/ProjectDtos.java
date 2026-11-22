package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.user.UserView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(min = 2, max = 6) String key,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String description) {
    }

    public record UpdateProjectRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String description,
            @NotNull @Min(1) @Max(10) Integer wipLimit) {
    }

    public record AddMemberRequest(@NotBlank @Email String email, @NotNull ProjectRole role) {
    }

    public record ChangeRoleRequest(@NotNull ProjectRole role) {
    }

    public record MemberView(Long membershipId, UserView user, ProjectRole role, String roleLabel, Instant joinedAt) {

        public static MemberView of(Membership m) {
            return new MemberView(m.getId(), UserView.of(m.getUser()), m.getRole(), m.getRole().label(), m.getJoinedAt());
        }
    }

    public record ProjectSummary(Long id, String key, String name, String description, ProjectRole myRole,
                                 long memberCount, String activeSprint, long openIssues) {
    }

    public record ProjectDetail(Long id, String key, String name, String description, int wipLimit,
                                ProjectRole myRole, List<MemberView> members, Instant createdAt) {
    }
}
