package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.shared.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Entity
@Table(name = "project")
public class Project {

    public static final int DEFAULT_WIP_LIMIT = 3;
    private static final Pattern KEY_FORMAT = Pattern.compile("[A-Z][A-Z0-9]{1,5}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_key", nullable = false, unique = true, length = 6)
    private String projectKey;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "wip_limit", nullable = false)
    private int wipLimit;

    @Column(name = "next_issue_number", nullable = false)
    private int nextIssueNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Project() {
    }

    public Project(String key, String name, String description, Instant createdAt) {
        String normalizedKey = key.trim().toUpperCase(Locale.ROOT);
        if (!KEY_FORMAT.matcher(normalizedKey).matches()) {
            throw new BusinessRuleException("INVALID_PROJECT_KEY",
                    "La clé du projet doit contenir 2 à 6 caractères (lettres majuscules et chiffres, commençant par une lettre).");
        }
        this.projectKey = normalizedKey;
        this.name = name.trim();
        this.description = description;
        this.wipLimit = DEFAULT_WIP_LIMIT;
        this.nextIssueNumber = 1;
        this.createdAt = createdAt;
    }

    /** Réserve le prochain numéro de ticket (ORN-1, ORN-2...). Protégé par le verrou optimiste du projet. */
    public int allocateIssueNumber() {
        return nextIssueNumber++;
    }

    public void rename(String name, String description) {
        this.name = name.trim();
        this.description = description;
    }

    public void changeWipLimit(int wipLimit) {
        if (wipLimit < 1 || wipLimit > 10) {
            throw new BusinessRuleException("INVALID_WIP_LIMIT", "La limite de tickets en cours doit être comprise entre 1 et 10.");
        }
        this.wipLimit = wipLimit;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return projectKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getWipLimit() {
        return wipLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
