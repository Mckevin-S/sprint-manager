package com.mckevin.sprintmanager.shared;

/** Ressource absente, ou invisible pour l'utilisateur courant. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
