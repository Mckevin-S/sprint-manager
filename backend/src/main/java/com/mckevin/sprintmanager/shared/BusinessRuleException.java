package com.mckevin.sprintmanager.shared;

/**
 * Violation d'une règle métier (workflow, limite WIP, sprint vide...).
 * Le code est stable et peut être interprété par le front ; le message est destiné à l'utilisateur.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
