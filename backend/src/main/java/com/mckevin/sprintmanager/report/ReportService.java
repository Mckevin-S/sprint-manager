package com.mckevin.sprintmanager.report;

import com.mckevin.sprintmanager.activity.ActivityRepository;
import com.mckevin.sprintmanager.activity.ActivityView;
import com.mckevin.sprintmanager.issue.Issue;
import com.mckevin.sprintmanager.issue.IssueRepository;
import com.mckevin.sprintmanager.issue.IssueStatus;
import com.mckevin.sprintmanager.issue.IssueType;
import com.mckevin.sprintmanager.project.Membership;
import com.mckevin.sprintmanager.project.MembershipRepository;
import com.mckevin.sprintmanager.project.ProjectAccess;
import com.mckevin.sprintmanager.report.ReportDtos.BacklogHealth;
import com.mckevin.sprintmanager.report.ReportDtos.BurndownView;
import com.mckevin.sprintmanager.report.ReportDtos.OverviewView;
import com.mckevin.sprintmanager.report.ReportDtos.StatusCount;
import com.mckevin.sprintmanager.report.ReportDtos.TypeCount;
import com.mckevin.sprintmanager.report.ReportDtos.VelocityEntry;
import com.mckevin.sprintmanager.report.ReportDtos.VelocityView;
import com.mckevin.sprintmanager.report.ReportDtos.Workload;
import com.mckevin.sprintmanager.shared.AppTime;
import com.mckevin.sprintmanager.shared.NotFoundException;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.sprint.SprintDtos.SprintView;
import com.mckevin.sprintmanager.sprint.SprintRepository;
import com.mckevin.sprintmanager.sprint.SprintService;
import com.mckevin.sprintmanager.sprint.SprintStatus;
import com.mckevin.sprintmanager.user.UserView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Indicateurs Scrum : burndown, vélocité et vue d'ensemble du projet. */
@Service
public class ReportService {

    private final SprintRepository sprints;
    private final IssueRepository issues;
    private final ActivityRepository activities;
    private final MembershipRepository memberships;
    private final SprintService sprintService;
    private final ProjectAccess access;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public ReportService(SprintRepository sprints, IssueRepository issues, ActivityRepository activities,
                         MembershipRepository memberships, SprintService sprintService, ProjectAccess access,
                         Clock clock) {
        this.sprints = sprints;
        this.issues = issues;
        this.activities = activities;
        this.memberships = memberships;
        this.sprintService = sprintService;
        this.access = access;
        this.clock = clock;
    }

    /** Burndown d'un sprint démarré ; par défaut le sprint actif. */
    @Transactional(readOnly = true)
    public BurndownView burndown(Long projectId, Long userId, Long sprintId) {
        access.member(projectId, userId);
        Sprint sprint = sprintId != null
                ? sprints.findByIdAndProjectId(sprintId, projectId).orElseThrow(() -> new NotFoundException("Sprint introuvable."))
                : sprints.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE).orElse(null);
        if (sprint == null || sprint.getStatus() == SprintStatus.PLANNED) {
            return new BurndownView(sprint == null ? null : sprintView(projectId, sprint), List.of());
        }
        LocalDate today = LocalDate.now(clock.withZone(AppTime.ZONE));
        LocalDate lastKnown = sprint.isClosed() ? sprint.getClosedAt().atZone(AppTime.ZONE).toLocalDate() : today;
        if (lastKnown.isAfter(sprint.getEndDate())) {
            lastKnown = sprint.getEndDate();
        }
        List<BurndownCalculator.Change> changes = activities
                .findBySprintIdAndRemainingDeltaIsNotNullOrderByOccurredAtAsc(sprint.getId()).stream()
                .map(e -> new BurndownCalculator.Change(e.getOccurredAt(), e.getRemainingDelta()))
                .toList();
        List<BurndownCalculator.Point> points = BurndownCalculator.compute(sprint.getStartDate(), sprint.getEndDate(),
                lastKnown, Objects.requireNonNullElse(sprint.getCommittedPoints(), 0), changes, AppTime.ZONE);
        return new BurndownView(sprintView(projectId, sprint), points);
    }

    /**
     * Vélocité sur les six derniers sprints clôturés. La fiabilité est le rapport entre points terminés
     * et points engagés, utile pour calibrer l'engagement du prochain sprint.
     */
    @Transactional(readOnly = true)
    public VelocityView velocity(Long projectId, Long userId) {
        access.member(projectId, userId);
        List<VelocityEntry> entries = sprints.findTop6ByProjectIdAndStatusOrderByClosedAtDesc(projectId, SprintStatus.CLOSED)
                .stream()
                .sorted(Comparator.comparing(Sprint::getClosedAt))
                .map(s -> new VelocityEntry(s.getId(), s.getName(),
                        Objects.requireNonNullElse(s.getCommittedPoints(), 0),
                        Objects.requireNonNullElse(s.getCompletedPoints(), 0)))
                .toList();
        if (entries.isEmpty()) {
            return new VelocityView(entries, BigDecimal.ZERO, null);
        }
        int completed = entries.stream().mapToInt(VelocityEntry::completed).sum();
        int committed = entries.stream().mapToInt(VelocityEntry::committed).sum();
        BigDecimal average = BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(entries.size()), 1, RoundingMode.HALF_UP);
        BigDecimal reliability = committed == 0 ? null
                : BigDecimal.valueOf(completed * 100L).divide(BigDecimal.valueOf(committed), 0, RoundingMode.HALF_UP);
        return new VelocityView(entries, average, reliability);
    }

    @Transactional(readOnly = true)
    public OverviewView overview(Long projectId, Long userId) {
        access.member(projectId, userId);
        Sprint active = sprints.findFirstByProjectIdAndStatus(projectId, SprintStatus.ACTIVE).orElse(null);
        List<Issue> sprintIssues = active == null ? List.of() : issues.findBySprintIdOrderByRankAscNumberAsc(active.getId());

        List<StatusCount> statuses = Arrays.stream(IssueStatus.values())
                .map(st -> {
                    List<Issue> matching = sprintIssues.stream().filter(i -> i.getStatus() == st).toList();
                    return new StatusCount(st, st.label(), matching.size(), matching.stream().mapToInt(Issue::points).sum());
                })
                .toList();

        long total = sprintIssues.stream().mapToInt(Issue::points).sum();
        long done = sprintIssues.stream().filter(Issue::isDone).mapToInt(Issue::points).sum();
        Integer completion = active == null ? null : total == 0 ? 0 : (int) Math.round(done * 100.0 / total);

        List<Workload> workload = memberships.findTeam(projectId).stream()
                .map(Membership::getUser)
                .map(user -> {
                    List<Issue> mine = sprintIssues.stream()
                            .filter(i -> i.getAssignee() != null && i.getAssignee().getId().equals(user.getId()) && !i.isDone())
                            .toList();
                    return new Workload(UserView.of(user), mine.size(), mine.stream().mapToInt(Issue::points).sum(),
                            mine.stream().filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS).count());
                })
                .toList();

        List<Issue> backlog = issues.findByProjectIdAndSprintIsNullAndStatusNot(projectId, IssueStatus.DONE);
        List<TypeCount> openByType = Arrays.stream(IssueType.values())
                .map(t -> new TypeCount(t, t.label(),
                        backlog.stream().filter(i -> i.getType() == t).count()
                                + sprintIssues.stream().filter(i -> i.getType() == t && !i.isDone()).count()))
                .toList();
        BacklogHealth health = new BacklogHealth(backlog.size(),
                backlog.stream().mapToInt(Issue::points).sum(),
                backlog.stream().filter(i -> i.getType() == IssueType.STORY && i.getStoryPoints() == null).count());

        List<ActivityView> recent = activities.findByProjectIdOrderByOccurredAtDescIdDesc(projectId, PageRequest.of(0, 8))
                .map(ActivityView::of).getContent();

        return new OverviewView(active == null ? null : sprintView(projectId, active), completion, statuses, workload,
                openByType, health, recent);
    }

    private SprintView sprintView(Long projectId, Sprint sprint) {
        return sprintService.describe(projectId, sprint);
    }
}
