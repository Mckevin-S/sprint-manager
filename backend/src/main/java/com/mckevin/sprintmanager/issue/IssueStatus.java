package com.mckevin.sprintmanager.issue;

public enum IssueStatus {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    IN_REVIEW("En revue"),
    DONE("Terminé");

    private final String label;

    IssueStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
