package com.mckevin.sprintmanager.user;

/** Représentation publique d'un utilisateur (jamais le mot de passe). */
public record UserView(Long id, String email, String fullName, String initials) {

    public static UserView of(User user) {
        return user == null ? null : new UserView(user.getId(), user.getEmail(), user.getFullName(), user.initials());
    }
}
