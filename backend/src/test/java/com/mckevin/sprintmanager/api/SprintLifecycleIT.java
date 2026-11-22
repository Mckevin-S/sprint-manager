package com.mckevin.sprintmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mckevin.sprintmanager.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parcours Scrum complet : backlog, planification, démarrage, workflow, burndown, clôture et vélocité. */
class SprintLifecycleIT extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

    private Session po;
    private Session sm;
    private Session dev1;
    private Session dev2;
    private long projectId;

    @BeforeEach
    void team() throws Exception {
        po = register("Camille Durand");
        sm = register("Hugo Lefèvre");
        dev1 = register("Inès Moreau");
        dev2 = register("Thomas Petit");
        projectId = read(postAs(po, Map.of("key", uniqueKey(), "name", "Orion"), "/api/projects")).get("id").asLong();
        addMember(sm, "SCRUM_MASTER");
        addMember(dev1, "DEVELOPER");
        addMember(dev2, "DEVELOPER");
        putAs(po, Map.of("name", "Orion", "wipLimit", 1), "/api/projects/{id}", projectId).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Du backlog à la vélocité, en respectant les rôles et les règles Scrum")
    void fullSprint() throws Exception {
        // Backlog : seul le PO rédige les stories, tout le monde peut créer bugs et tâches
        postAs(dev1, issue("STORY", "Story interdite", 3, null), "/api/projects/{id}/issues", projectId)
                .andExpect(status().isForbidden());
        long storyA = createIssue(po, issue("STORY", "Suivi du dossier", 5, dev1.id()));
        long storyB = createIssue(po, issue("STORY", "Notifications", null, null));
        long bugC = createIssue(dev2, issue("BUG", "Erreur 500 à l'envoi", 2, dev2.id()));
        long taskD = createIssue(dev1, issue("TASK", "Audit accessibilité", 3, null));

        // Planification : réservée au Scrum Master et au PO
        long sprint1 = read(postAs(sm, Map.of(), "/api/projects/{id}/sprints", projectId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sprint 1"))).get("id").asLong();
        putAs(dev1, plan(sprint1), "/api/projects/{id}/issues/{i}/sprint", projectId, storyA).andExpect(status().isForbidden());
        for (long issueId : List.of(storyA, storyB, bugC)) {
            putAs(sm, plan(sprint1), "/api/projects/{id}/issues/{i}/sprint", projectId, issueId).andExpect(status().isOk());
        }

        // Démarrage : objectif obligatoire, stories estimées
        postAs(sm, start(null), "/api/projects/{id}/sprints/{s}/start", projectId, sprint1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SPRINT_GOAL_REQUIRED"));
        postAs(sm, start("Suivre son dossier"), "/api/projects/{id}/sprints/{s}/start", projectId, sprint1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNESTIMATED_STORIES"))
                .andExpect(jsonPath("$.detail", containsString("-2")));
        estimate(dev2, storyB, "Notifications", 3);
        postAs(sm, start("Suivre son dossier"), "/api/projects/{id}/sprints/{s}/start", projectId, sprint1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.committedPoints").value(10));

        // Un seul sprint actif à la fois
        long sprint2 = read(postAs(sm, Map.of("name", "Sprint 2"), "/api/projects/{id}/sprints", projectId)).get("id").asLong();
        putAs(sm, plan(sprint2), "/api/projects/{id}/issues/{i}/sprint", projectId, taskD).andExpect(status().isOk());
        postAs(sm, start("Autre objectif"), "/api/projects/{id}/sprints/{s}/start", projectId, sprint2)
                .andExpect(jsonPath("$.code").value("SPRINT_ALREADY_ACTIVE"));

        // Workflow : droits, étapes, limite WIP, sprint actif
        transition(dev2, storyA, "IN_PROGRESS").andExpect(status().isForbidden());
        transition(dev1, storyA, "IN_REVIEW").andExpect(jsonPath("$.code").value("TRANSITION_NOT_ALLOWED"));
        transition(dev1, storyA, "IN_PROGRESS").andExpect(status().isOk());
        transition(dev1, storyB, "IN_PROGRESS")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("WIP_LIMIT_REACHED"));
        transition(dev1, taskD, "IN_PROGRESS").andExpect(jsonPath("$.code").value("NOT_IN_ACTIVE_SPRINT"));
        transition(dev1, storyA, "IN_REVIEW").andExpect(status().isOk());
        transition(dev1, storyA, "DONE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issue.status").value("DONE"))
                .andExpect(jsonPath("$.history.length()").value(5));

        // Tableau et burndown
        getAs(dev2, "/api/projects/{id}/board", projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wipLimit").value(1))
                .andExpect(jsonPath("$.columns[3].status").value("DONE"))
                .andExpect(jsonPath("$.columns[3].points").value(5))
                .andExpect(jsonPath("$.columns[0].issues.length()").value(2));
        getAs(dev2, "/api/projects/{id}/reports/burndown", projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(15))
                .andExpect(jsonPath("$.points[0].remaining").value(5))
                .andExpect(jsonPath("$.points[0].ideal").value(10.0))
                .andExpect(jsonPath("$.points[14].ideal").value(0.0));

        // Commentaire et vue d'ensemble
        postAs(dev2, Map.of("body", "Revue faite, RAS."), "/api/projects/{id}/issues/{i}/comments", projectId, storyA)
                .andExpect(status().isCreated());
        getAs(po, "/api/projects/{id}/reports/overview", projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercent").value(50))
                .andExpect(jsonPath("$.workload.length()").value(4));

        // Clôture : les tickets non terminés partent dans le sprint suivant, remis à faire
        postAs(dev1, Map.of("moveToSprintId", sprint2), "/api/projects/{id}/sprints/{s}/close", projectId, sprint1)
                .andExpect(status().isForbidden());
        postAs(sm, Map.of("moveToSprintId", sprint2), "/api/projects/{id}/sprints/{s}/close", projectId, sprint1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprint.completedPoints").value(5))
                .andExpect(jsonPath("$.carriedOver").value(2))
                .andExpect(jsonPath("$.carriedTo").value("Sprint 2"));
        getAs(po, "/api/projects/{id}/issues/{i}", projectId, storyB)
                .andExpect(jsonPath("$.issue.sprintId").value(sprint2))
                .andExpect(jsonPath("$.issue.status").value("TODO"));

        getAs(po, "/api/projects/{id}/reports/velocity", projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprints[0].committed").value(10))
                .andExpect(jsonPath("$.sprints[0].completed").value(5))
                .andExpect(jsonPath("$.reliability").value(50));

        JsonNode feed = read(getAs(po, "/api/projects/{id}/activity", projectId).andExpect(status().isOk()));
        assertThat(feed.get("totalElements").asLong()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Priorisation et suppression du backlog réservées au Product Owner")
    void backlogManagement() throws Exception {
        long first = createIssue(po, issue("STORY", "Premier", 3, null));
        long second = createIssue(po, issue("STORY", "Deuxième", 5, null));

        putAs(po, Map.of("issueIds", List.of(second, first)), "/api/projects/{id}/issues/rank", projectId)
                .andExpect(status().isNoContent());
        getAs(dev1, "/api/projects/{id}/issues?backlog=true", projectId)
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.content[1].id").value(first));
        getAs(dev1, "/api/projects/{id}/issues?q=deux", projectId)
                .andExpect(jsonPath("$.totalElements").value(1));

        deleteAs(dev1, "/api/projects/{id}/issues/{i}", projectId, first).andExpect(status().isForbidden());
        deleteAs(po, "/api/projects/{id}/issues/{i}", projectId, first).andExpect(status().isNoContent());
        getAs(po, "/api/projects/{id}/issues/{i}", projectId, first).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Seul un membre du projet peut être assigné ; les points suivent Fibonacci")
    void assignmentAndEstimationRules() throws Exception {
        Session outsider = register("Externe");
        postAs(po, issue("TASK", "Tâche", 3, outsider.id()), "/api/projects/{id}/issues", projectId)
                .andExpect(jsonPath("$.code").value("ASSIGNEE_NOT_MEMBER"));
        postAs(po, issue("TASK", "Tâche", 4, null), "/api/projects/{id}/issues", projectId)
                .andExpect(jsonPath("$.code").value("INVALID_STORY_POINTS"));
    }

    private void addMember(Session member, String role) throws Exception {
        postAs(po, Map.of("email", member.email(), "role", role), "/api/projects/{id}/members", projectId)
                .andExpect(status().isCreated());
    }

    private long createIssue(Session author, Map<String, Object> body) throws Exception {
        return read(postAs(author, body, "/api/projects/{id}/issues", projectId).andExpect(status().isCreated()))
                .at("/issue/id").asLong();
    }

    private void estimate(Session who, long issueId, String title, int points) throws Exception {
        putAs(who, Map.of("title", title, "priority", "MEDIUM", "storyPoints", points),
                "/api/projects/{id}/issues/{i}", projectId, issueId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issue.storyPoints").value(points));
    }

    private org.springframework.test.web.servlet.ResultActions transition(Session who, long issueId, String target)
            throws Exception {
        return postAs(who, Map.of("status", target), "/api/projects/{id}/issues/{i}/transition", projectId, issueId);
    }

    private static Map<String, Object> issue(String type, String title, Integer points, Long assigneeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("title", title);
        body.put("priority", "MEDIUM");
        body.put("storyPoints", points);
        body.put("assigneeId", assigneeId);
        return body;
    }

    private static Map<String, Object> plan(long sprintId) {
        return Map.of("sprintId", sprintId);
    }

    private static Map<String, Object> start(String goal) {
        Map<String, Object> body = new HashMap<>();
        body.put("goal", goal);
        body.put("startDate", TODAY.toString());
        body.put("endDate", TODAY.plusDays(14).toString());
        return body;
    }
}
