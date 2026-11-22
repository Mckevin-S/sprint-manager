package com.mckevin.sprintmanager.user;

import com.mckevin.sprintmanager.shared.CurrentUser;
import com.mckevin.sprintmanager.user.AuthDtos.AuthResponse;
import com.mckevin.sprintmanager.user.AuthDtos.LoginRequest;
import com.mckevin.sprintmanager.user.AuthDtos.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;

    public AuthController(AuthService auth, CurrentUser currentUser) {
        this.auth = auth;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un compte et renvoie un jeton d'accès")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Échange e-mail et mot de passe contre un jeton d'accès")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Utilisateur connecté")
    public UserView me() {
        return auth.me(currentUser.id());
    }
}
