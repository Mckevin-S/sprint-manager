package com.mckevin.sprintmanager.project;

/**
 * Rôle Scrum d'un membre dans un projet.
 *
 * <ul>
 *   <li>Product Owner : contenu et priorités du backlog, gestion de l'équipe.</li>
 *   <li>Scrum Master : planification et cérémonies (démarrage, clôture des sprints), gestion de l'équipe.</li>
 *   <li>Développeur : réalisation des tickets.</li>
 * </ul>
 */
public enum ProjectRole {
    PRODUCT_OWNER("Product Owner"),
    SCRUM_MASTER("Scrum Master"),
    DEVELOPER("Développeur");

    private final String label;

    ProjectRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
