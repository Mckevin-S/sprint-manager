package com.mckevin.sprintmanager.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base des tests d'intégration : l'application complète sur une vraie base PostgreSQL (Testcontainers),
 * partagée par toutes les classes de test. Chaque test crée ses propres utilisateurs et projets
 * avec des identifiants uniques, ce qui rend les tests indépendants de leur ordre d'exécution.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Utilisateur de test connecté : son jeton est ajouté aux requêtes. */
    public record Session(Long id, String email, String token) {
    }

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Clé de projet unique : 2 lettres puis 4 caractères aléatoires. */
    protected static String uniqueKey() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
    }

    protected Session register(String fullName) throws Exception {
        String email = unique("user-") + "@test.fr";
        JsonNode body = read(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("email", email, "fullName", fullName, "password", "motdepasse1"))))
                .andExpect(status().isCreated()));
        return new Session(body.at("/user/id").asLong(), email, body.get("token").asText());
    }

    protected ResultActions call(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token()));
    }

    protected ResultActions getAs(Session s, String url, Object... vars) throws Exception {
        return call(s, get(url, vars));
    }

    protected ResultActions postAs(Session s, Object body, String url, Object... vars) throws Exception {
        return call(s, post(url, vars).contentType(MediaType.APPLICATION_JSON).content(write(body)));
    }

    protected ResultActions putAs(Session s, Object body, String url, Object... vars) throws Exception {
        return call(s, put(url, vars).contentType(MediaType.APPLICATION_JSON).content(write(body)));
    }

    protected ResultActions deleteAs(Session s, String url, Object... vars) throws Exception {
        return call(s, delete(url, vars));
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected String write(Object body) throws Exception {
        return json.writeValueAsString(body);
    }
}
