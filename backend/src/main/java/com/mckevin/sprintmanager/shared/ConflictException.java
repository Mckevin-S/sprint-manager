package com.mckevin.sprintmanager.shared;

/** État incompatible avec la demande : doublon, modification concurrente... */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
