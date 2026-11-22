package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.shared.ForbiddenException;
import com.mckevin.sprintmanager.shared.NotFoundException;
import org.springframework.stereotype.Component;

/**
 * Contrôle d'accès par projet. Un non-membre reçoit 404 (le projet ne lui est pas révélé),
 * un membre sans le bon rôle reçoit 403.
 */
@Component
public class ProjectAccess {

    private final MembershipRepository memberships;

    public ProjectAccess(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    public Membership member(Long projectId, Long userId) {
        return memberships.findMembership(projectId, userId)
                .orElseThrow(() -> new NotFoundException("Projet introuvable."));
    }

    public Membership requireRole(Long projectId, Long userId, String action, ProjectRole... roles) {
        Membership membership = member(projectId, userId);
        if (!membership.hasAnyRole(roles)) {
            throw new ForbiddenException("Votre rôle (" + membership.getRole().label() + ") ne permet pas de " + action + ".");
        }
        return membership;
    }
}
