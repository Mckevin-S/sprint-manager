package com.mckevin.sprintmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration validity) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret doit contenir au moins 32 caractères");
        }
        if (validity == null) {
            validity = Duration.ofHours(8);
        }
    }
}
