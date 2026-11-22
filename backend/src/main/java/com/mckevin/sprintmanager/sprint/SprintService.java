package com.mckevin.sprintmanager.sprint;

import com.mckevin.sprintmanager.activity.ActivityRecorder;
import com.mckevin.sprintmanager.activity.ActivityType;
import com.mckevin.sprintmanager.issue.Issue;
import com.mckevin.sprintmanager.issue.IssueDtos.IssueView;
import com.mckevin.sprintmanager.issue.IssueRepository;
import com.mckevin.sprintmanager.issue.IssueRepository.SprintStats;
import com.mckevin.sprintmanager.issue.IssueStatus;
import com.mckevin.sprintmanager.issue.IssueType;
import com.mckevin.sprintmanager.project.Membership;
import com.mckevin.sprintmanager.project.ProjectAccess;
import com.mckevin.sprintmanager.project.ProjectRole;
import com.mckevin.sprintmanager.shared.AppTime;
import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.shared.NotFoundException;
import com.mckevin.sprintmanager.sprint.SprintDtos.BoardColumn;
import com.mckevin.sprintmanager.sprint.SprintDtos.BoardView;
import com.mckevin.sprintmanager.sprint.SprintDtos.CloseReport;
import com.mckevin.sprintmanager.sprint.SprintDtos.CreateSprintRequest;
import com.mckevin.sprintmanager.sprint.SprintDtos.SprintView;
import com.mckevin.sprintmanager.sprint.SprintDtos.StartSprintRequest;
import com.mckevin.sprintmanager.sprint.SprintDtos.UpdateSprintRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cycle de vie des sprints : planification, démarrage, tableau, clôture avec report des tickets. */
@Service
public class SprintService {

    private static final long RANK_STEP = 1_000;
    private static final String PLAN_ACTION = "planifier les sprints";

    private final SprintRepository sprints;
    private final IssueRepository issues;
    private final ActivityRecorder activity;
    private final ProjectAccess access;
    private final Clock clock;

    public SprintService(SprintRepository sprints, IssueRepository issues, ActivityRecorder activity,
                         ProjectAccess access, Clock clock) {
        this.sprints = sprints;
        this.issues = issues;
        this.activity = activity;
        this.access = access;
        this.clock = clock;
    }

    /** Sprint actif d'abord, puis sprints planifiés, puis clôturés du plus récent au plus ancien. */
    @Transactional(readOnly = true)
    public List<SprintView> list(Long projectId, Long userId) {
        access.member(projectId, userId);
        Map<Long, SprintStats> stats = statsOf(projectId);
        Comparator<Sprint> order = Comparator.<Sprint>comparingInt(s -> switch (s.getStatus()) {
            case ACTIVE -> 0;
            case PLANNED -> 1;
            case CLOSED -> 2;
        }).thenComparing((a, b) -> a.getStatus() == SprintStatus.CLOSED
                ? b.getCreatedAt().compareTo(a.getCreatedAt())
                : a.getCreatedAt().compareTo(b.getCreatedAt()));
        return sprints.findAllOfProject(projectId).stream()
                .sorted(order)
                .map(s -> view(s, stats.get(s.getId())))
                .toList();
    }

    @Transactional
    public SprintView create(Long projectId, Long userId, CreateSprintRequest request) {
        Membership me = access.requireRole(projectId, userId, PLAN_ACTION, ProjectRole.SCRUM_MASTER, ProjectRole.PRODUCT_OWNER);
        String name = request.name() == null || request.name().isBlank()
                ? "Sprint " + (sprints.countByProjectId(projectId) + 1)
                : request.name();
        Instant now = Instant.now(clock);
        Sprint sprint = sprints.save(new Sprint(me.getProject(), name, request.goal(), now));
        activity.sprint(sprint, me.getUser(), ActivityType.SPRINT_CREATED, "a créé " + sprint.getName(), null, now);
        return view(sprint, null);
    }

    @Transactional
    public SprintView update(Long projectId, Long sprintId, Long userId, UpdateSprintRequest request) {
        access.requireRole(projectId, userId, PLAN_ACTION, ProjectRole.SCRUM_MASTER, ProjectRole.PRODUCT_OWNER);
        Sprint sprint = load(projectId, sprintId);
        sprint.edit(request.name(), request.goal());
        return view(sprint, statsOf(projectId).get(sprintId));
    }

    /**
     * Démarre un sprint. Règles : un seul sprint actif par projet, au moins un ticket,
     * toutes les stories estimées. L'engagement (points) est figé au démarrage.
     */
    @Transactional
    public SprintView start(Long projectId, Long sprintId, Long userId, StartSprintRequest request) {
        Membership me = access.requireRole(projectId, userId, "démarrer un sprint",
                ProjectRole.SCRUM_MASTER, ProjectRole.PRODUCT_OWNER);
        Sprint sprint = load(projectId, sprintId);
        sprint.requireGoal(request.goal());
        sprints.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE).ifPresent(active -> {
            throw new BusinessRuleException("SPRINT_ALREADY_ACTIVE",
                    active.getName() + " est déjà en cours. Clôturez-le avant d'en démarrer un autre.");
        });
        List<Issue> scope = issues.findBySprintIdOrderByRankAscNumberAsc(sprintId);
        if (scope.isEmpty()) {
            throw new BusinessRuleException("EMPTY_SPRINT", "Ajoutez au moins un ticket au sprint avant de le démarrer.");
        }
        List<String> unestimated = scope.stream()
                .filter(i -> i.getType() == IssueType.STORY && i.getStoryPoints() == null)
                .map(Issue::key)
                .toList();
        if (!unestimated.isEmpty()) {
            throw new BusinessRuleException("UNESTIMATED_STORIES",
                    "Estimez les stories avant de démarrer le sprint : " + String.join(", ", unestimated) + ".");
        }
        int committed = scope.stream().filter(i -> !i.isDone()).mapToInt(Issue::points).sum();
        Instant now = Instant.now(clock);
        sprint.start(request.goal(), request.startDate(), request.endDate(), committed, now);
        activity.sprint(sprint, me.getUser(), ActivityType.SPRINT_STARTED,
                "a démarré " + sprint.getName() + " (" + committed + " points engagés)", null, now);
        return view(sprint, statsOf(projectId).get(sprintId));
    }

    /**
     * Clôture le sprint actif. Les points terminés sont figés pour la vélocité ;
     * les tickets non terminés repartent « à faire » vers le backlog ou un sprint planifié.
     */
    @Transactional
    public CloseReport close(Long projectId, Long sprintId, Long userId, Long moveToSprintId) {
        Membership me = access.requireRole(projectId, userId, "clôturer un sprint",
                ProjectRole.SCRUM_MASTER, ProjectRole.PRODUCT_OWNER);
        Sprint sprint = load(projectId, sprintId);
        Sprint target = null;
        if (moveToSprintId != null) {
            target = load(projectId, moveToSprintId);
            if (target.getStatus() != SprintStatus.PLANNED) {
                throw new BusinessRuleException("INVALID_CARRY_OVER_TARGET",
                        "Les tickets non terminés ne peuvent être reportés que vers un sprint planifié.");
            }
        }
        List<Issue> scope = issues.findBySprintIdOrderByRankAscNumberAsc(sprintId);
        int completed = scope.stream().filter(Issue::isDone).mapToInt(Issue::points).sum();
        Instant now = Instant.now(clock);
        sprint.close(completed, now);

        List<Issue> unfinished = scope.stream().filter(i -> !i.isDone()).toList();
        long rank = issues.maxRank(projectId);
        String destination = target == null ? "le backlog" : target.getName();
        for (Issue issue : unfinished) {
            rank += RANK_STEP;
            issue.carryOver(target, rank, now);
            activity.issue(issue, me.getUser(), ActivityType.SCOPE_REMOVED,
                    "a reporté le ticket de " + sprint.getName() + " vers " + destination, null, now);
        }
        activity.sprint(sprint, me.getUser(), ActivityType.SPRINT_CLOSED, "a clôturé " + sprint.getName() + " : "
                + completed + " / " + sprint.getCommittedPoints() + " points terminés", null, now);
        return new CloseReport(view(sprint, statsOf(projectId).get(sprintId)), unfinished.size(), destination);
    }

    @Transactional
    public void delete(Long projectId, Long sprintId, Long userId) {
        access.requireRole(projectId, userId, PLAN_ACTION, ProjectRole.SCRUM_MASTER, ProjectRole.PRODUCT_OWNER);
        Sprint sprint = load(projectId, sprintId);
        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new BusinessRuleException("SPRINT_NOT_PLANNED", "Seul un sprint planifié peut être supprimé.");
        }
        Instant now = Instant.now(clock);
        issues.findBySprintIdOrderByRankAscNumberAsc(sprintId).forEach(i -> i.planInto(null, now));
        sprints.delete(sprint);
    }

    /** Tableau Kanban du sprint actif ; sprint nul s'il n'y en a pas. */
    @Transactional(readOnly = true)
    public BoardView board(Long projectId, Long userId) {
        Membership me = access.member(projectId, userId);
        int wipLimit = me.getProject().getWipLimit();
        return sprints.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE)
                .map(sprint -> {
                    List<Issue> scope = issues.findBySprintIdOrderByRankAscNumberAsc(sprint.getId());
                    List<BoardColumn> columns = Arrays.stream(IssueStatus.values())
                            .map(status -> {
                                List<Issue> inColumn = scope.stream().filter(i -> i.getStatus() == status).toList();
                                return new BoardColumn(status, status.label(),
                                        inColumn.stream().map(IssueView::of).toList(),
                                        inColumn.stream().mapToInt(Issue::points).sum());
                            })
                            .toList();
                    return new BoardView(view(sprint, statsOf(projectId).get(sprint.getId())), wipLimit, columns);
                })
                .orElse(new BoardView(null, wipLimit, List.of()));
    }

    private Sprint load(Long projectId, Long sprintId) {
        return sprints.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new NotFoundException("Sprint introuvable."));
    }

    private Map<Long, SprintStats> statsOf(Long projectId) {
        return issues.sprintStats(projectId).stream()
                .collect(Collectors.toMap(SprintStats::getSprintId, Function.identity()));
    }

    /** Vue d'un sprint avec ses statistiques, sans contrôle d'accès (à appeler après ce contrôle). */
    public SprintView describe(Long projectId, Sprint sprint) {
        return view(sprint, statsOf(projectId).get(sprint.getId()));
    }

    private SprintView view(Sprint s, SprintStats stats) {
        LocalDate today = LocalDate.now(clock.withZone(AppTime.ZONE));
        return new SprintView(s.getId(), s.getName(), s.getGoal(), s.getStatus(), s.getStartDate(), s.getEndDate(),
                s.getCommittedPoints(), s.getCompletedPoints(),
                stats == null ? 0 : stats.getIssueCount().longValue(),
                stats == null ? 0 : stats.getTotalPoints().longValue(),
                stats == null ? 0 : stats.getDonePoints().longValue(),
                s.daysRemaining(today));
    }
}
