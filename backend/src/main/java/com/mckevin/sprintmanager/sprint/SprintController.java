package com.mckevin.sprintmanager.sprint;

import com.mckevin.sprintmanager.shared.CurrentUser;
import com.mckevin.sprintmanager.sprint.SprintDtos.BoardView;
import com.mckevin.sprintmanager.sprint.SprintDtos.CloseReport;
import com.mckevin.sprintmanager.sprint.SprintDtos.CloseSprintRequest;
import com.mckevin.sprintmanager.sprint.SprintDtos.CreateSprintRequest;
import com.mckevin.sprintmanager.sprint.SprintDtos.SprintView;
import com.mckevin.sprintmanager.sprint.SprintDtos.StartSprintRequest;
import com.mckevin.sprintmanager.sprint.SprintDtos.UpdateSprintRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@Tag(name = "Sprints", description = "Planification, démarrage, tableau et clôture")
public class SprintController {

    private final SprintService service;
    private final CurrentUser currentUser;

    public SprintController(SprintService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/sprints")
    @Operation(summary = "Sprints du projet avec leurs statistiques")
    public List<SprintView> list(@PathVariable Long projectId) {
        return service.list(projectId, currentUser.id());
    }

    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un sprint planifié")
    public SprintView create(@PathVariable Long projectId, @Valid @RequestBody CreateSprintRequest request) {
        return service.create(projectId, currentUser.id(), request);
    }

    @PutMapping("/sprints/{sprintId}")
    @Operation(summary = "Renomme un sprint ou change son objectif")
    public SprintView update(@PathVariable Long projectId, @PathVariable Long sprintId,
                             @Valid @RequestBody UpdateSprintRequest request) {
        return service.update(projectId, sprintId, currentUser.id(), request);
    }

    @PostMapping("/sprints/{sprintId}/start")
    @Operation(summary = "Démarre le sprint (Scrum Master ou Product Owner)")
    public SprintView start(@PathVariable Long projectId, @PathVariable Long sprintId,
                            @Valid @RequestBody StartSprintRequest request) {
        return service.start(projectId, sprintId, currentUser.id(), request);
    }

    @PostMapping("/sprints/{sprintId}/close")
    @Operation(summary = "Clôture le sprint et reporte les tickets non terminés")
    public CloseReport close(@PathVariable Long projectId, @PathVariable Long sprintId,
                             @RequestBody(required = false) CloseSprintRequest request) {
        return service.close(projectId, sprintId, currentUser.id(), request == null ? null : request.moveToSprintId());
    }

    @DeleteMapping("/sprints/{sprintId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprime un sprint planifié ; ses tickets retournent au backlog")
    public void delete(@PathVariable Long projectId, @PathVariable Long sprintId) {
        service.delete(projectId, sprintId, currentUser.id());
    }

    @GetMapping("/board")
    @Operation(summary = "Tableau Kanban du sprint actif")
    public BoardView board(@PathVariable Long projectId) {
        return service.board(projectId, currentUser.id());
    }
}
