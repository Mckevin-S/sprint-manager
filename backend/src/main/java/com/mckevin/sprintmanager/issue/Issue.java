package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.project.Project;
import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/** Ticket du backlog : story, bug ou tâche, identifié par une clé lisible (ORN-42). */
@Entity
@Table(name = "issue")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private int number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IssueType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority;

    @Column(name = "story_points")
    private Integer storyPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @Column(name = "backlog_rank", nullable = false)
    private long rank;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    private long version;

    protected Issue() {
    }

    @SuppressWarnings("java:S107")
    public Issue(Project project, int number, IssueType type, String title, String description, Priority priority,
                 Integer storyPoints, User reporter, long rank, Instant now) {
        this.project = project;
        this.number = number;
        this.type = type;
        this.title = title.trim();
        this.description = description;
        this.priority = priority == null ? Priority.MEDIUM : priority;
        this.storyPoints = StoryPoints.validate(storyPoints);
        this.reporter = reporter;
        this.rank = rank;
        this.status = IssueStatus.TODO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String key() {
        return project.getKey() + "-" + number;
    }

    /**
     * Change le statut en respectant le workflow. Une story ne peut être terminée que si elle est estimée,
     * sinon la vélocité serait faussée.
     */
    public void transitionTo(IssueStatus target, Instant now) {
        if (target == status) {
            return;
        }
        if (!IssueWorkflow.canTransition(status, target)) {
            throw new BusinessRuleException("TRANSITION_NOT_ALLOWED",
                    "Passage de « " + status.label() + " » à « " + target.label() + " » impossible. Étapes possibles : "
                            + IssueWorkflow.nextStatuses(status).stream().map(IssueStatus::label).toList() + ".");
        }
        if (target == IssueStatus.DONE && type == IssueType.STORY && storyPoints == null) {
            throw new BusinessRuleException("STORY_NOT_ESTIMATED", "Estimez la story avant de la terminer.");
        }
        this.status = target;
        this.resolvedAt = target == IssueStatus.DONE ? now : null;
        this.updatedAt = now;
    }

    public void edit(String title, String description, Priority priority, Instant now) {
        this.title = title.trim();
        this.description = description;
        this.priority = priority == null ? this.priority : priority;
        this.updatedAt = now;
    }

    public void estimate(Integer points, Instant now) {
        this.storyPoints = StoryPoints.validate(points);
        this.updatedAt = now;
    }

    public void assignTo(User user, Instant now) {
        this.assignee = user;
        this.updatedAt = now;
    }

    public void planInto(Sprint target, Instant now) {
        if (target != null && target.isClosed()) {
            throw new BusinessRuleException("SPRINT_CLOSED", "Impossible d'ajouter un ticket à un sprint clôturé.");
        }
        if (status == IssueStatus.DONE && target != null && sprint == null) {
            throw new BusinessRuleException("ISSUE_ALREADY_DONE", "Un ticket terminé ne peut pas être planifié.");
        }
        this.sprint = target;
        this.updatedAt = now;
    }

    /** Ticket non terminé à la clôture d'un sprint : reporté (backlog ou sprint suivant) et remis à faire. */
    public void carryOver(Sprint target, long newRank, Instant now) {
        this.sprint = target;
        this.rank = newRank;
        if (status != IssueStatus.TODO) {
            this.status = IssueStatus.TODO;
        }
        this.updatedAt = now;
    }

    public void reRank(long rank) {
        this.rank = rank;
    }

    public boolean isDone() {
        return status == IssueStatus.DONE;
    }

    public int points() {
        return StoryPoints.orZero(storyPoints);
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public int getNumber() {
        return number;
    }

    public IssueType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Integer getStoryPoints() {
        return storyPoints;
    }

    public User getAssignee() {
        return assignee;
    }

    public User getReporter() {
        return reporter;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public long getRank() {
        return rank;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public long getVersion() {
        return version;
    }
}
