package com.mckevin.sprintmanager.api;

import com.mckevin.sprintmanager.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    @Test
    void registerLoginAndReadProfile() throws Exception {
        Session lea = register("Léa Martin");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("email", lea.email().toUpperCase(), "password", "motdepasse1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.initials").value("LM"));

        getAs(lea, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Léa Martin"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        Session lea = register("Léa Martin");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("email", lea.email(), "password", "mauvais-mot"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou mot de passe incorrect."));
    }

    @Test
    void duplicateEmailAndWeakPasswordAreRejected() throws Exception {
        Session lea = register("Léa Martin");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("email", lea.email(), "fullName", "Autre", "password", "motdepasse1"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("email", unique("x") + "@test.fr", "fullName", "Autre", "password", "court"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Champs invalides"));
    }

    @Test
    void apiRequiresAValidToken() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects").header("Authorization", "Bearer jeton.invalide.signature"))
                .andExpect(status().isUnauthorized());
    }
}
