package com.mckevin.sprintmanager.shared;

/** L'utilisateur est membre du projet mais son rôle ne permet pas l'action. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
