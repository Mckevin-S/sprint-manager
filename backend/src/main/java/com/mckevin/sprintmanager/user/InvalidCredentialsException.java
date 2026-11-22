package com.mckevin.sprintmanager.user;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-mail ou mot de passe incorrect.");
    }
}
