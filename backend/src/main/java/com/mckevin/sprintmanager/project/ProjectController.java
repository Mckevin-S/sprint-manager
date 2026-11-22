package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.activity.ActivityRepository;
import com.mckevin.sprintmanager.activity.ActivityView;
import com.mckevin.sprintmanager.project.ProjectDtos.AddMemberRequest;
import com.mckevin.sprintmanager.project.ProjectDtos.ChangeRoleRequest;
import com.mckevin.sprintmanager.project.ProjectDtos.CreateProjectRequest;
import com.mckevin.sprintmanager.project.ProjectDtos.MemberView;
import com.mckevin.sprintmanager.project.ProjectDtos.ProjectDetail;
import com.mckevin.sprintmanager.project.ProjectDtos.ProjectSummary;
import com.mckevin.sprintmanager.project.ProjectDtos.UpdateProjectRequest;
import com.mckevin.sprintmanager.shared.CurrentUser;
import com.mckevin.sprintmanager.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projets", description = "Projets, équipe et journal d'activité")
public class ProjectController {

    private final ProjectService projects;
    private final ProjectAccess access;
    private final ActivityRepository activities;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projects, ProjectAccess access, ActivityRepository activities,
                             CurrentUser currentUser) {
        this.projects = projects;
        this.access = access;
        this.activities = activities;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Projets dont l'utilisateur est membre")
    public List<ProjectSummary> mine() {
        return projects.myProjects(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un projet ; le créateur en devient Product Owner")
    public ProjectDetail create(@Valid @RequestBody CreateProjectRequest request) {
        return projects.create(currentUser.id(), request);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Détail du projet et de l'équipe")
    public ProjectDetail get(@PathVariable Long projectId) {
        return projects.get(projectId, currentUser.id());
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Modifie le nom, la description et la limite WIP (Product Owner)")
    public ProjectDetail update(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest request) {
        return projects.update(projectId, currentUser.id(), request);
    }

    @PostMapping("/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute un membre par son e-mail")
    public MemberView addMember(@PathVariable Long projectId, @Valid @RequestBody AddMemberRequest request) {
        return projects.addMember(projectId, currentUser.id(), request);
    }

    @PutMapping("/{projectId}/members/{membershipId}")
    @Operation(summary = "Change le rôle d'un membre (Product Owner)")
    public MemberView changeRole(@PathVariable Long projectId, @PathVariable Long membershipId,
                                 @Valid @RequestBody ChangeRoleRequest request) {
        return projects.changeRole(projectId, membershipId, currentUser.id(), request.role());
    }

    @DeleteMapping("/{projectId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire un membre (Product Owner)")
    public void removeMember(@PathVariable Long projectId, @PathVariable Long membershipId) {
        projects.removeMember(projectId, membershipId, currentUser.id());
    }

    @GetMapping("/{projectId}/activity")
    @Transactional(readOnly = true)
    @Operation(summary = "Journal d'activité du projet, du plus récent au plus ancien")
    public PageResponse<ActivityView> activity(@PathVariable Long projectId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "30") int size) {
        access.member(projectId, currentUser.id());
        return PageResponse.of(activities.findByProjectIdOrderByOccurredAtDescIdDesc(projectId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), ActivityView::of);
    }
}
