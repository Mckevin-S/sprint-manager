package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.issue.IssueDtos.CommentRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.CommentView;
import com.mckevin.sprintmanager.issue.IssueDtos.CreateIssueRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.IssueDetail;
import com.mckevin.sprintmanager.issue.IssueDtos.IssueView;
import com.mckevin.sprintmanager.issue.IssueDtos.PlanRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.RankRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.TransitionRequest;
import com.mckevin.sprintmanager.issue.IssueDtos.UpdateIssueRequest;
import com.mckevin.sprintmanager.issue.IssueService.IssueFilter;
import com.mckevin.sprintmanager.shared.CurrentUser;
import com.mckevin.sprintmanager.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/projects/{projectId}/issues")
@Tag(name = "Tickets", description = "Backlog, workflow, estimation et commentaires")
public class IssueController {

    private final IssueService service;
    private final CurrentUser currentUser;

    public IssueController(IssueService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Recherche paginée, triée par priorité de backlog")
    @SuppressWarnings("java:S107")
    public PageResponse<IssueView> search(@PathVariable Long projectId,
                                          @RequestParam(required = false) Long sprintId,
                                          @RequestParam(required = false) Boolean backlog,
                                          @RequestParam(required = false) IssueStatus status,
                                          @RequestParam(required = false) IssueType type,
                                          @RequestParam(required = false) Long assigneeId,
                                          @RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return service.search(projectId, currentUser.id(),
                new IssueFilter(sprintId, backlog, status, type, assigneeId, q), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un ticket (story réservée au Product Owner)")
    public IssueDetail create(@PathVariable Long projectId, @Valid @RequestBody CreateIssueRequest request) {
        return service.create(projectId, currentUser.id(), request);
    }

    @GetMapping("/{issueId}")
    @Operation(summary = "Détail d'un ticket avec commentaires et historique")
    public IssueDetail detail(@PathVariable Long projectId, @PathVariable Long issueId) {
        return service.detail(projectId, issueId, currentUser.id());
    }

    @PutMapping("/{issueId}")
    @Operation(summary = "Modifie le contenu, l'estimation ou l'assignation")
    public IssueDetail update(@PathVariable Long projectId, @PathVariable Long issueId,
                              @Valid @RequestBody UpdateIssueRequest request) {
        return service.update(projectId, issueId, currentUser.id(), request);
    }

    @PostMapping("/{issueId}/transition")
    @Operation(summary = "Fait avancer le ticket dans le workflow")
    public IssueDetail transition(@PathVariable Long projectId, @PathVariable Long issueId,
                                  @Valid @RequestBody TransitionRequest request) {
        return service.transition(projectId, issueId, currentUser.id(), request.status());
    }

    @PutMapping("/{issueId}/sprint")
    @Operation(summary = "Planifie le ticket dans un sprint, ou le renvoie au backlog (sprintId nul)")
    public IssueView plan(@PathVariable Long projectId, @PathVariable Long issueId, @RequestBody PlanRequest request) {
        return service.plan(projectId, issueId, currentUser.id(), request.sprintId());
    }

    @PutMapping("/rank")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Réordonne les tickets selon la liste fournie")
    public void rank(@PathVariable Long projectId, @Valid @RequestBody RankRequest request) {
        service.rank(projectId, currentUser.id(), request.issueIds());
    }

    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un ticket (Product Owner)")
    public void delete(@PathVariable Long projectId, @PathVariable Long issueId) {
        service.delete(projectId, issueId, currentUser.id());
    }

    @PostMapping("/{issueId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajoute un commentaire")
    public CommentView comment(@PathVariable Long projectId, @PathVariable Long issueId,
                               @Valid @RequestBody CommentRequest request) {
        return service.comment(projectId, issueId, currentUser.id(), request);
    }
}
