package com.mckevin.sprintmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.mckevin.sprintmanager.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectTeamIT extends AbstractIntegrationTest {

    @Test
    void creatorIsProductOwnerAndCanBuildTheTeam() throws Exception {
        Session po = register("Camille Durand");
        Session dev = register("Inès Moreau");
        String key = uniqueKey();

        JsonNode project = read(postAs(po, Map.of("key", key.toLowerCase(), "name", "Orion"), "/api/projects")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.myRole").value("PRODUCT_OWNER")));
        long projectId = project.get("id").asLong();

        postAs(po, Map.of("email", dev.email(), "role", "DEVELOPER"), "/api/projects/{id}/members", projectId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleLabel").value("Développeur"));

        getAs(dev, "/api/projects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value(key))
                .andExpect(jsonPath("$[0].memberCount").value(2));

        postAs(po, Map.of("email", dev.email(), "role", "DEVELOPER"), "/api/projects/{id}/members", projectId)
                .andExpect(status().isConflict());
        postAs(po, Map.of("key", key, "name", "Doublon"), "/api/projects").andExpect(status().isConflict());
    }

    @Test
    void outsidersCannotSeeTheProjectAndDevelopersCannotManageIt() throws Exception {
        Session po = register("Camille Durand");
        Session dev = register("Thomas Petit");
        Session outsider = register("Personne Externe");
        long projectId = read(postAs(po, Map.of("key", uniqueKey(), "name", "Orion"), "/api/projects")).get("id").asLong();
        postAs(po, Map.of("email", dev.email(), "role", "DEVELOPER"), "/api/projects/{id}/members", projectId);

        getAs(outsider, "/api/projects/{id}", projectId).andExpect(status().isNotFound());
        putAs(dev, Map.of("name", "Renommé", "wipLimit", 2), "/api/projects/{id}", projectId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Votre rôle (Développeur) ne permet pas de modifier le projet."));
        putAs(po, Map.of("name", "Orion v2", "wipLimit", 2), "/api/projects/{id}", projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wipLimit").value(2));
    }

    @Test
    void projectAlwaysKeepsAProductOwner() throws Exception {
        Session po = register("Camille Durand");
        JsonNode project = read(postAs(po, Map.of("key", uniqueKey(), "name", "Orion"), "/api/projects"));
        long projectId = project.get("id").asLong();
        long membershipId = project.at("/members/0/membershipId").asLong();

        putAs(po, Map.of("role", "DEVELOPER"), "/api/projects/{id}/members/{m}", projectId, membershipId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("LAST_PRODUCT_OWNER"));
        deleteAs(po, "/api/projects/{id}/members/{m}", projectId, membershipId)
                .andExpect(status().isUnprocessableEntity());
    }
}
