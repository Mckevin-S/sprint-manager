package com.mckevin.sprintmanager.report;

import com.mckevin.sprintmanager.report.ReportDtos.BurndownView;
import com.mckevin.sprintmanager.report.ReportDtos.OverviewView;
import com.mckevin.sprintmanager.report.ReportDtos.VelocityView;
import com.mckevin.sprintmanager.shared.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/reports")
@Tag(name = "Rapports", description = "Burndown, vélocité et vue d'ensemble")
public class ReportController {

    private final ReportService reports;
    private final CurrentUser currentUser;

    public ReportController(ReportService reports, CurrentUser currentUser) {
        this.reports = reports;
        this.currentUser = currentUser;
    }

    @GetMapping("/burndown")
    @Operation(summary = "Burndown d'un sprint (par défaut le sprint actif)")
    public BurndownView burndown(@PathVariable Long projectId, @RequestParam(required = false) Long sprintId) {
        return reports.burndown(projectId, currentUser.id(), sprintId);
    }

    @GetMapping("/velocity")
    @Operation(summary = "Vélocité des six derniers sprints clôturés")
    public VelocityView velocity(@PathVariable Long projectId) {
        return reports.velocity(projectId, currentUser.id());
    }

    @GetMapping("/overview")
    @Operation(summary = "Vue d'ensemble : avancement du sprint, charge de l'équipe, santé du backlog")
    public OverviewView overview(@PathVariable Long projectId) {
        return reports.overview(projectId, currentUser.id());
    }
}
