package com.mckevin.sprintmanager.user;

import com.mckevin.sprintmanager.shared.ConflictException;
import com.mckevin.sprintmanager.shared.NotFoundException;
import com.mckevin.sprintmanager.user.AuthDtos.AuthResponse;
import com.mckevin.sprintmanager.user.AuthDtos.LoginRequest;
import com.mckevin.sprintmanager.user.AuthDtos.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("Un compte existe déjà avec cet e-mail.");
        }
        User user = users.save(new User(request.email(), request.fullName(),
                passwordEncoder.encode(request.password()), Instant.now(clock)));
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return respond(user);
    }

    @Transactional(readOnly = true)
    public UserView me(Long userId) {
        return users.findById(userId).map(UserView::of)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }

    private AuthResponse respond(User user) {
        JwtTokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), UserView.of(user));
    }
}
