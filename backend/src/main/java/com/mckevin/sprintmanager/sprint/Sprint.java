package com.mckevin.sprintmanager.sprint;

import com.mckevin.sprintmanager.project.Project;
import com.mckevin.sprintmanager.shared.BusinessRuleException;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Itération Scrum : planifiée, puis active (une seule par projet), puis clôturée. */
@Entity
@Table(name = "sprint")
public class Sprint {

    public static final int MAX_DURATION_DAYS = 28;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 300)
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SprintStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "committed_points")
    private Integer committedPoints;

    @Column(name = "completed_points")
    private Integer completedPoints;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Sprint() {
    }

    public Sprint(Project project, String name, String goal, Instant createdAt) {
        this.project = project;
        this.name = name.trim();
        this.goal = blankToNull(goal);
        this.status = SprintStatus.PLANNED;
        this.createdAt = createdAt;
    }

    public void edit(String name, String goal) {
        if (status == SprintStatus.CLOSED) {
            throw new BusinessRuleException("SPRINT_CLOSED", "Un sprint clôturé ne peut plus être modifié.");
        }
        this.name = name.trim();
        this.goal = blankToNull(goal);
    }

    /**
     * Démarre le sprint. Règles : sprint planifié, objectif renseigné, dates cohérentes, durée de 4 semaines au plus.
     * Les règles qui dépendent des autres sprints ou des tickets sont vérifiées par le service.
     */
    public void start(String goal, LocalDate startDate, LocalDate endDate, int committedPoints, Instant now) {
        if (status != SprintStatus.PLANNED) {
            throw new BusinessRuleException("SPRINT_NOT_PLANNED", "Seul un sprint planifié peut être démarré.");
        }
        String effectiveGoal = requireGoal(goal);
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new BusinessRuleException("INVALID_SPRINT_DATES", "La date de fin doit être postérieure à la date de début.");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_DURATION_DAYS) {
            throw new BusinessRuleException("SPRINT_TOO_LONG", "Un sprint dure au plus " + MAX_DURATION_DAYS + " jours.");
        }
        this.goal = effectiveGoal.trim();
        this.startDate = startDate;
        this.endDate = endDate;
        this.committedPoints = committedPoints;
        this.startedAt = now;
        this.status = SprintStatus.ACTIVE;
    }

    /** Objectif retenu au démarrage : celui fourni, sinon celui défini en planification. */
    public String requireGoal(String requested) {
        String effectiveGoal = requested != null && !requested.isBlank() ? requested : this.goal;
        if (effectiveGoal == null || effectiveGoal.isBlank()) {
            throw new BusinessRuleException("SPRINT_GOAL_REQUIRED", "Définissez un objectif de sprint avant de le démarrer.");
        }
        return effectiveGoal;
    }

    public void close(int completedPoints, Instant now) {
        if (status != SprintStatus.ACTIVE) {
            throw new BusinessRuleException("SPRINT_NOT_ACTIVE", "Seul le sprint actif peut être clôturé.");
        }
        this.completedPoints = completedPoints;
        this.closedAt = now;
        this.status = SprintStatus.CLOSED;
    }

    /** Jours restants avant la fin prévue (0 si dépassée), uniquement pour un sprint actif. */
    public Long daysRemaining(LocalDate today) {
        if (status != SprintStatus.ACTIVE) {
            return null;
        }
        return Math.max(0, ChronoUnit.DAYS.between(today, endDate));
    }

    public boolean isActive() {
        return status == SprintStatus.ACTIVE;
    }

    public boolean isClosed() {
        return status == SprintStatus.CLOSED;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public SprintStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Integer getCommittedPoints() {
        return committedPoints;
    }

    public Integer getCompletedPoints() {
        return completedPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
