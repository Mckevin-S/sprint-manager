package com.mckevin.sprintmanager.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mckevin.sprintmanager.demo.DemoDataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Les données de démonstration se chargent sur une base vide dédiée et produisent des rapports cohérents. */
@SpringBootTest(properties = "app.demo.enabled=true")
@AutoConfigureMockMvc
class DemoDataSeederIT {

    static final PostgreSQLContainer<?> DEMO_DB = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        DEMO_DB.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DEMO_DB::getJdbcUrl);
        registry.add("spring.datasource.username", DEMO_DB::getUsername);
        registry.add("spring.datasource.password", DEMO_DB::getPassword);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void demoProjectIsReadyToExplore() throws Exception {
        String token = json.readTree(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "camille@demo.fr", "password", DemoDataSeeder.DEMO_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        JsonNode projects = json.readTree(mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("ORN"))
                .andExpect(jsonPath("$[0].activeSprint").value("Sprint 4"))
                .andReturn().getResponse().getContentAsString());
        long projectId = projects.get(0).get("id").asLong();

        mvc.perform(get("/api/projects/{id}/reports/velocity", projectId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.sprints.length()").value(3))
                .andExpect(jsonPath("$.sprints[0].completed").value(21));

        JsonNode burndown = json.readTree(mvc.perform(get("/api/projects/{id}/reports/burndown", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(burndown.at("/sprint/committedPoints").asInt()).isEqualTo(38);
        assertThat(burndown.at("/points/0/remaining").asInt()).isEqualTo(38);
        assertThat(burndown.at("/points/6/remaining").asInt()).isEqualTo(28);
        // Jour futur : pas de valeur réelle (champ omis en JSON)
        JsonNode future = burndown.at("/points/7/remaining");
        assertThat(future.isMissingNode() || future.isNull()).isTrue();
    }
}
