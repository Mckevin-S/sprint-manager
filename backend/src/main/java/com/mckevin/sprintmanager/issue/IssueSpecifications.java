package com.mckevin.sprintmanager.issue;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Critères de recherche combinables pour la liste des tickets. */
final class IssueSpecifications {

    private IssueSpecifications() {
    }

    static Specification<Issue> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    static Specification<Issue> inSprint(Long sprintId) {
        return (root, query, cb) -> cb.equal(root.get("sprint").get("id"), sprintId);
    }

    static Specification<Issue> inBacklog() {
        return (root, query, cb) -> cb.isNull(root.get("sprint"));
    }

    static Specification<Issue> hasStatus(IssueStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Issue> hasType(IssueType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    static Specification<Issue> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }

    static Specification<Issue> textContains(String text) {
        String pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }
}
