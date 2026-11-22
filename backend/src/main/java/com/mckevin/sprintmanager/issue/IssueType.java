package com.mckevin.sprintmanager.issue;

public enum IssueType {
    STORY("Story"),
    BUG("Bug"),
    TASK("Tâche");

    private final String label;

    IssueType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
