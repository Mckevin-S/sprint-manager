package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.activity.ActivityRecorder;
import com.mckevin.sprintmanager.activity.ActivityRepository;
import com.mckevin.sprintmanager.activity.ActivityType;
import com.mckevin.sprintmanager.activity.ActivityView;
import com.mckevin.sprintmanager.issue.IssueDtos.CommentRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.CommentView;
import com.mckevin.sprintmanager.issue.IssueDtos.CreateIssueRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.IssueDetail;
import com.mckevin.sprintmanager.issue.IssueDtos.IssueView;
import com.mckevin.sprintmanager.issue.IssueDtos.UpdateIssueRequest;
import com.mckevin.sprintmanager.project.Membership;
import com.mckevin.sprintmanager.project.MembershipRepository;
import com.mckevin.sprintmanager.project.Project;
import com.mckevin.sprintmanager.project.ProjectAccess;
import com.mckevin.sprintmanager.project.ProjectRole;
import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.shared.ForbiddenException;
import com.mckevin.sprintmanager.shared.NotFoundException;
import com.mckevin.sprintmanager.shared.PageResponse;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.sprint.SprintRepository;
import com.mckevin.sprintmanager.user.User;
import com.mckevin.sprintmanager.user.UserView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage des tickets. Matrice des droits :
 * <ul>
 *   <li>créer ou modifier une story, prioriser le backlog, supprimer : Product Owner ;</li>
 *   <li>créer un bug ou une tâche, estimer, assigner, commenter : tout membre ;</li>
 *   <li>faire avancer un ticket : son assigné, ou le PO / Scrum Master ;</li>
 *   <li>planifier dans un sprint : Product Owner ou Scrum Master.</li>
 * </ul>
 */
@Service
public class IssueService {

    static final long RANK_STEP = 1_000;

    private final IssueRepository issues;
    private final CommentRepository comments;
    private final SprintRepository sprints;
    private final MembershipRepository memberships;
    private final ActivityRepository activities;
    private final ActivityRecorder activity;
    private final ProjectAccess access;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public IssueService(IssueRepository issues, CommentRepository comments, SprintRepository sprints,
                        MembershipRepository memberships, ActivityRepository activities, ActivityRecorder activity,
                        ProjectAccess access, Clock clock) {
        this.issues = issues;
        this.comments = comments;
        this.sprints = sprints;
        this.memberships = memberships;
        this.activities = activities;
        this.activity = activity;
        this.access = access;
        this.clock = clock;
    }

    public record IssueFilter(Long sprintId, Boolean backlog, IssueStatus status, IssueType type, Long assigneeId,
                              String q) {
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueView> search(Long projectId, Long userId, IssueFilter filter, int page, int size) {
        access.member(projectId, userId);
        Specification<Issue> spec = IssueSpecifications.inProject(projectId);
        if (filter.sprintId() != null) {
            spec = spec.and(IssueSpecifications.inSprint(filter.sprintId()));
        }
        if (Boolean.TRUE.equals(filter.backlog())) {
            spec = spec.and(IssueSpecifications.inBacklog());
        }
        if (filter.status() != null) {
            spec = spec.and(IssueSpecifications.hasStatus(filter.status()));
        }
        if (filter.type() != null) {
            spec = spec.and(IssueSpecifications.hasType(filter.type()));
        }
        if (filter.assigneeId() != null) {
            spec = spec.and(IssueSpecifications.assignedTo(filter.assigneeId()));
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            spec = spec.and(IssueSpecifications.textContains(filter.q()));
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by("rank").ascending().and(Sort.by("number").ascending()));
        return PageResponse.of(issues.findAll(spec, pageable), IssueView::of);
    }

    @Transactional(readOnly = true)
    public IssueDetail detail(Long projectId, Long issueId, Long userId) {
        access.member(projectId, userId);
        return toDetail(load(projectId, issueId));
    }

    @Transactional
    public IssueDetail create(Long projectId, Long userId, CreateIssueRequest request) {
        Membership me = access.member(projectId, userId);
        if (request.type() == IssueType.STORY && !me.hasAnyRole(ProjectRole.PRODUCT_OWNER)) {
            throw new ForbiddenException("Seul le Product Owner rédige les user stories.");
        }
        Project project = me.getProject();
        Instant now = Instant.now(clock);
        Issue issue = new Issue(project, project.allocateIssueNumber(), request.type(), request.title(),
                request.description(), request.priority(), request.storyPoints(), me.getUser(),
                issues.maxRank(projectId) + RANK_STEP, now);
        if (request.assigneeId() != null) {
            issue.assignTo(memberOf(projectId, request.assigneeId()), now);
        }
        if (request.sprintId() != null) {
            access.requireRole(projectId, userId, "planifier un sprint", ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER);
            issue.planInto(sprintOf(projectId, request.sprintId()), now);
        }
        issues.save(issue);
        activity.issue(issue, me.getUser(), ActivityType.ISSUE_CREATED,
                "a créé « " + issue.getTitle() + " » (" + issue.getType().label() + ")", issue.points(), now);
        return toDetail(issue);
    }

    @Transactional
    public IssueDetail update(Long projectId, Long issueId, Long userId, UpdateIssueRequest request) {
        Membership me = access.member(projectId, userId);
        Issue issue = load(projectId, issueId);
        Instant now = Instant.now(clock);
        User actor = me.getUser();

        boolean contentChanged = !Objects.equals(issue.getTitle(), request.title().trim())
                || !Objects.equals(issue.getDescription(), request.description())
                || issue.getPriority() != request.priority();
        if (contentChanged) {
            if (issue.getType() == IssueType.STORY && !me.hasAnyRole(ProjectRole.PRODUCT_OWNER)) {
                throw new ForbiddenException("Seul le Product Owner modifie le contenu et la priorité d'une story.");
            }
            issue.edit(request.title(), request.description(), request.priority(), now);
            activity.issue(issue, actor, ActivityType.ISSUE_UPDATED, "a modifié le ticket", null, now);
        }

        Integer oldPoints = issue.getStoryPoints();
        if (!Objects.equals(oldPoints, request.storyPoints())) {
            issue.estimate(request.storyPoints(), now);
            int delta = issue.isDone() ? 0 : StoryPoints.orZero(request.storyPoints()) - StoryPoints.orZero(oldPoints);
            activity.issue(issue, actor, ActivityType.POINTS_CHANGED,
                    "a estimé à " + describePoints(request.storyPoints()) + " (avant : " + describePoints(oldPoints) + ")",
                    delta, now);
        }

        Long oldAssignee = issue.getAssignee() == null ? null : issue.getAssignee().getId();
        if (!Objects.equals(oldAssignee, request.assigneeId())) {
            User assignee = request.assigneeId() == null ? null : memberOf(projectId, request.assigneeId());
            issue.assignTo(assignee, now);
            activity.issue(issue, actor, ActivityType.ASSIGNEE_CHANGED,
                    assignee == null ? "a retiré l'assignation" : "a assigné le ticket à " + assignee.getFullName(), null, now);
        }
        return toDetail(issue);
    }

    /**
     * Fait avancer un ticket dans le workflow. Règles supplémentaires :
     * le ticket doit être dans le sprint actif pour démarrer, il est auto-assigné s'il ne l'est pas,
     * et la limite de travail en cours (WIP) de l'assigné est respectée.
     */
    @Transactional
    public IssueDetail transition(Long projectId, Long issueId, Long userId, IssueStatus target) {
        Membership me = access.member(projectId, userId);
        Issue issue = load(projectId, issueId);
        IssueStatus from = issue.getStatus();
        if (from == target) {
            return toDetail(issue);
        }
        Instant now = Instant.now(clock);
        boolean manager = me.hasAnyRole(ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER);
        if (!manager && issue.getAssignee() != null && !issue.getAssignee().getId().equals(userId)) {
            throw new ForbiddenException("Ce ticket est assigné à " + issue.getAssignee().getFullName() + ".");
        }
        if (target == IssueStatus.IN_PROGRESS) {
            if (issue.getSprint() == null || !issue.getSprint().isActive()) {
                throw new BusinessRuleException("NOT_IN_ACTIVE_SPRINT",
                        "Seuls les tickets du sprint actif peuvent être démarrés.");
            }
            if (issue.getAssignee() == null) {
                issue.assignTo(me.getUser(), now);
            }
            long inProgress = issues.countInProgress(projectId, issue.getAssignee().getId(), issue.getId());
            int limit = me.getProject().getWipLimit();
            if (inProgress >= limit) {
                throw new BusinessRuleException("WIP_LIMIT_REACHED", issue.getAssignee().getFullName()
                        + " a déjà " + inProgress + " ticket(s) en cours (limite : " + limit + "). Terminez-en un d'abord.");
            }
        }
        issue.transitionTo(target, now);
        int delta = 0;
        if (target == IssueStatus.DONE) {
            delta = -issue.points();
        } else if (from == IssueStatus.DONE) {
            delta = issue.points();
        }
        activity.issue(issue, me.getUser(), ActivityType.STATUS_CHANGED,
                "a déplacé de « " + from.label() + " » vers « " + target.label() + " »", delta, now);
        return toDetail(issue);
    }

    /** Planifie un ticket dans un sprint, ou le renvoie au backlog (sprintId nul). */
    @Transactional
    public IssueView plan(Long projectId, Long issueId, Long userId, Long sprintId) {
        Membership me = access.requireRole(projectId, userId, "planifier un sprint",
                ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER);
        Issue issue = load(projectId, issueId);
        Sprint target = sprintId == null ? null : sprintOf(projectId, sprintId);
        Sprint current = issue.getSprint();
        if (Objects.equals(current == null ? null : current.getId(), sprintId)) {
            return IssueView.of(issue);
        }
        Instant now = Instant.now(clock);
        int remaining = issue.isDone() ? 0 : issue.points();
        if (current != null) {
            activity.issue(issue, me.getUser(), ActivityType.SCOPE_REMOVED, "a retiré le ticket de " + current.getName(),
                    -remaining, now);
        }
        issue.planInto(target, now);
        if (target != null) {
            activity.issue(issue, me.getUser(), ActivityType.SCOPE_ADDED, "a planifié le ticket dans " + target.getName(),
                    remaining, now);
        }
        return IssueView.of(issue);
    }

    /** Applique un nouvel ordre de priorité (backlog ou sprint) à partir de la liste ordonnée des tickets. */
    @Transactional
    public void rank(Long projectId, Long userId, List<Long> orderedIds) {
        access.requireRole(projectId, userId, "prioriser le backlog", ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER);
        Map<Long, Issue> byId = issues.findByProjectIdAndIdIn(projectId, orderedIds).stream()
                .collect(Collectors.toMap(Issue::getId, Function.identity()));
        if (byId.size() != new HashSet<>(orderedIds).size()) {
            throw new NotFoundException("Un ou plusieurs tickets sont introuvables dans ce projet.");
        }
        long rank = RANK_STEP;
        for (Long id : orderedIds) {
            byId.get(id).reRank(rank);
            rank += RANK_STEP;
        }
    }

    @Transactional
    public void delete(Long projectId, Long issueId, Long userId) {
        Membership me = access.requireRole(projectId, userId, "supprimer un ticket", ProjectRole.PRODUCT_OWNER);
        Issue issue = load(projectId, issueId);
        Instant now = Instant.now(clock);
        activity.issue(issue, me.getUser(), ActivityType.ISSUE_DELETED, "a supprimé « " + issue.getTitle() + " »",
                issue.isDone() ? 0 : -issue.points(), now);
        issues.delete(issue);
    }

    @Transactional
    public CommentView comment(Long projectId, Long issueId, Long userId, CommentRequest request) {
        Membership me = access.member(projectId, userId);
        Issue issue = load(projectId, issueId);
        Instant now = Instant.now(clock);
        Comment comment = comments.save(new Comment(issue, me.getUser(), request.body(), now));
        activity.issue(issue, me.getUser(), ActivityType.COMMENT_ADDED, "a commenté", null, now);
        return CommentView.of(comment);
    }

    private Issue load(Long projectId, Long issueId) {
        return issues.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable."));
    }

    private Sprint sprintOf(Long projectId, Long sprintId) {
        return sprints.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new NotFoundException("Sprint introuvable."));
    }

    private User memberOf(Long projectId, Long userId) {
        return memberships.findMembership(projectId, userId)
                .map(Membership::getUser)
                .orElseThrow(() -> new BusinessRuleException("ASSIGNEE_NOT_MEMBER",
                        "Un ticket ne peut être assigné qu'à un membre du projet."));
    }

    private IssueDetail toDetail(Issue issue) {
        List<CommentView> commentViews = issue.getId() == null ? List.of()
                : comments.findByIssueIdOrderByCreatedAtAsc(issue.getId()).stream().map(CommentView::of).toList();
        List<ActivityView> history = issue.getId() == null ? List.of()
                : activities.findByIssueIdOrderByOccurredAtAscIdAsc(issue.getId()).stream().map(ActivityView::of).toList();
        return new IssueDetail(IssueView.of(issue), issue.getDescription(),
                UserView.of(issue.getReporter()),
                issue.getSprint() == null ? null : issue.getSprint().getName(),
                issue.getCreatedAt(), issue.getResolvedAt(), IssueWorkflow.nextStatuses(issue.getStatus()),
                commentViews, history);
    }

    private static String describePoints(Integer points) {
        return points == null ? "non estimé" : points + " pt" + (points > 1 ? "s" : "");
    }
}
