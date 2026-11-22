package com.mckevin.sprintmanager.issue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Workflow des tickets. Un ticket passe obligatoirement par la revue avant d'être terminé,
 * et un ticket terminé peut être rouvert (retour en cours).
 *
 * <pre>
 * TODO ⇄ IN_PROGRESS ⇄ IN_REVIEW → DONE
 *            ↑______________________|
 * </pre>
 */
public final class IssueWorkflow {

    private static final Map<IssueStatus, Set<IssueStatus>> TRANSITIONS = new EnumMap<>(IssueStatus.class);

    static {
        TRANSITIONS.put(IssueStatus.TODO, EnumSet.of(IssueStatus.IN_PROGRESS));
        TRANSITIONS.put(IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.TODO, IssueStatus.IN_REVIEW));
        TRANSITIONS.put(IssueStatus.IN_REVIEW, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.DONE));
        TRANSITIONS.put(IssueStatus.DONE, EnumSet.of(IssueStatus.IN_PROGRESS));
    }

    private IssueWorkflow() {
    }

    public static boolean canTransition(IssueStatus from, IssueStatus to) {
        return TRANSITIONS.get(from).contains(to);
    }

    public static Set<IssueStatus> nextStatuses(IssueStatus from) {
        return EnumSet.copyOf(TRANSITIONS.get(from));
    }
}
