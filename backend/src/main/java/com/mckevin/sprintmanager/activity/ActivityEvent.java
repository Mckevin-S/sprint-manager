package com.mckevin.sprintmanager.activity;

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

import java.time.Instant;

/**
 * Événement du journal d'activité, en ajout seul.
 * {@code remainingDelta} indique l'effet de l'événement sur le travail restant du sprint actif
 * (+ points ajoutés, − points terminés) : c'est la source de vérité du burndown.
 */
@Entity
@Table(name = "activity_event")
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "issue_id")
    private Long issueId;

    @Column(name = "issue_key", length = 20)
    private String issueKey;

    @Column(name = "sprint_id")
    private Long sprintId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(name = "remaining_delta")
    private Integer remainingDelta;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ActivityEvent() {
    }

    @SuppressWarnings("java:S107")
    public ActivityEvent(Long projectId, Long issueId, String issueKey, Long sprintId, User actor, ActivityType type,
                         String summary, Integer remainingDelta, Instant occurredAt) {
        this.projectId = projectId;
        this.issueId = issueId;
        this.issueKey = issueKey;
        this.sprintId = sprintId;
        this.actor = actor;
        this.type = type;
        this.summary = summary.length() > 300 ? summary.substring(0, 297) + "..." : summary;
        this.remainingDelta = remainingDelta;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getIssueId() {
        return issueId;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public User getActor() {
        return actor;
    }

    public ActivityType getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public Integer getRemainingDelta() {
        return remainingDelta;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
