package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.activity.ActivityRecorder;
import com.mckevin.sprintmanager.activity.ActivityType;
import com.mckevin.sprintmanager.issue.IssueRepository;
import com.mckevin.sprintmanager.issue.IssueStatus;
import com.mckevin.sprintmanager.project.ProjectDtos.AddMemberRequest;
import com.mckevin.sprintmanager.project.ProjectDtos.CreateProjectRequest;
import com.mckevin.sprintmanager.project.ProjectDtos.MemberView;
import com.mckevin.sprintmanager.project.ProjectDtos.ProjectDetail;
import com.mckevin.sprintmanager.project.ProjectDtos.ProjectSummary;
import com.mckevin.sprintmanager.project.ProjectDtos.UpdateProjectRequest;
import com.mckevin.sprintmanager.shared.BusinessRuleException;
import com.mckevin.sprintmanager.shared.ConflictException;
import com.mckevin.sprintmanager.shared.NotFoundException;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.sprint.SprintRepository;
import com.mckevin.sprintmanager.sprint.SprintStatus;
import com.mckevin.sprintmanager.user.User;
import com.mckevin.sprintmanager.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Projets et équipe. Le créateur devient Product Owner ; un projet garde toujours au moins un Product Owner. */
@Service
public class ProjectService {

    private static final String MANAGE_TEAM = "gérer l'équipe";

    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final SprintRepository sprints;
    private final IssueRepository issues;
    private final ActivityRecorder activity;
    private final ProjectAccess access;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public ProjectService(ProjectRepository projects, MembershipRepository memberships, UserRepository users,
                          SprintRepository sprints, IssueRepository issues, ActivityRecorder activity,
                          ProjectAccess access, Clock clock) {
        this.projects = projects;
        this.memberships = memberships;
        this.users = users;
        this.sprints = sprints;
        this.issues = issues;
        this.activity = activity;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> myProjects(Long userId) {
        return memberships.findProjectsOf(userId).stream()
                .map(m -> {
                    Project p = m.getProject();
                    String activeSprint = sprints.findFirstByProjectIdAndStatus(p.getId(), SprintStatus.ACTIVE)
                            .map(Sprint::getName).orElse(null);
                    return new ProjectSummary(p.getId(), p.getKey(), p.getName(), p.getDescription(), m.getRole(),
                            memberships.countByProjectId(p.getId()), activeSprint,
                            issues.countByProjectIdAndStatusNot(p.getId(), IssueStatus.DONE));
                })
                .toList();
    }

    @Transactional
    public ProjectDetail create(Long userId, CreateProjectRequest request) {
        User creator = users.findById(userId).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        String key = request.key().trim().toUpperCase(Locale.ROOT);
        if (projects.existsByProjectKey(key)) {
            throw new ConflictException("La clé " + key + " est déjà utilisée par un autre projet.");
        }
        Instant now = Instant.now(clock);
        Project project = projects.save(new Project(key, request.name(), request.description(), now));
        Membership owner = memberships.save(new Membership(project, creator, ProjectRole.PRODUCT_OWNER, now));
        activity.project(project.getId(), creator, ActivityType.MEMBER_ADDED, "a créé le projet", now);
        return detail(owner);
    }

    @Transactional(readOnly = true)
    public ProjectDetail get(Long projectId, Long userId) {
        return detail(access.member(projectId, userId));
    }

    @Transactional
    public ProjectDetail update(Long projectId, Long userId, UpdateProjectRequest request) {
        Membership me = access.requireRole(projectId, userId, "modifier le projet", ProjectRole.PRODUCT_OWNER);
        me.getProject().rename(request.name(), request.description());
        me.getProject().changeWipLimit(request.wipLimit());
        return detail(me);
    }

    @Transactional
    public MemberView addMember(Long projectId, Long userId, AddMemberRequest request) {
        Membership me = access.requireRole(projectId, userId, MANAGE_TEAM,
                ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER);
        User invited = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NotFoundException("Aucun compte n'est associé à " + request.email()
                        + ". La personne doit d'abord créer son compte."));
        if (memberships.findMembership(projectId, invited.getId()).isPresent()) {
            throw new ConflictException(invited.getFullName() + " fait déjà partie du projet.");
        }
        Instant now = Instant.now(clock);
        Membership membership = memberships.save(new Membership(me.getProject(), invited, request.role(), now));
        activity.project(projectId, me.getUser(), ActivityType.MEMBER_ADDED,
                "a ajouté " + invited.getFullName() + " (" + request.role().label() + ")", now);
        return MemberView.of(membership);
    }

    @Transactional
    public MemberView changeRole(Long projectId, Long membershipId, Long userId, ProjectRole role) {
        Membership me = access.requireRole(projectId, userId, "changer les rôles", ProjectRole.PRODUCT_OWNER);
        Membership target = loadMembership(projectId, membershipId);
        if (target.getRole() == ProjectRole.PRODUCT_OWNER && role != ProjectRole.PRODUCT_OWNER) {
            requireAnotherProductOwner(projectId);
        }
        target.changeRole(role);
        activity.project(projectId, me.getUser(), ActivityType.MEMBER_ROLE_CHANGED,
                "a donné le rôle " + role.label() + " à " + target.getUser().getFullName(), Instant.now(clock));
        return MemberView.of(target);
    }

    @Transactional
    public void removeMember(Long projectId, Long membershipId, Long userId) {
        Membership me = access.requireRole(projectId, userId, MANAGE_TEAM, ProjectRole.PRODUCT_OWNER);
        Membership target = loadMembership(projectId, membershipId);
        if (target.getRole() == ProjectRole.PRODUCT_OWNER) {
            requireAnotherProductOwner(projectId);
        }
        memberships.delete(target);
        activity.project(projectId, me.getUser(), ActivityType.MEMBER_REMOVED,
                "a retiré " + target.getUser().getFullName() + " du projet", Instant.now(clock));
    }

    private void requireAnotherProductOwner(Long projectId) {
        if (memberships.countByProjectIdAndRole(projectId, ProjectRole.PRODUCT_OWNER) <= 1) {
            throw new BusinessRuleException("LAST_PRODUCT_OWNER",
                    "Le projet doit garder au moins un Product Owner. Nommez-en un autre d'abord.");
        }
    }

    private Membership loadMembership(Long projectId, Long membershipId) {
        return memberships.findById(membershipId)
                .filter(m -> m.getProject().getId().equals(projectId))
                .orElseThrow(() -> new NotFoundException("Membre introuvable."));
    }

    private ProjectDetail detail(Membership me) {
        Project p = me.getProject();
        List<MemberView> team = memberships.findTeam(p.getId()).stream().map(MemberView::of).toList();
        return new ProjectDetail(p.getId(), p.getKey(), p.getName(), p.getDescription(), p.getWipLimit(), me.getRole(),
                team, p.getCreatedAt());
    }
}
