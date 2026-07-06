package com.docportal.docportal.controllers;

import com.docportal.docportal.dto.LoginRequest;
import com.docportal.docportal.dto.LoginResponse;
import com.docportal.docportal.dto.UserDto;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.repositories.UserRepository;
import com.docportal.docportal.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le nom d'utilisateur et le mot de passe sont obligatoires"));
        }

        Optional<User> found = userRepository.findByUsername(request.username().trim());

        if (found.isEmpty()
                || !passwordEncoder.matches(request.password(), found.get().getPassword())
                || !Boolean.TRUE.equals(found.get().getActive())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Identifiants incorrects"));
        }

        User user = found.get();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(
                new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), UserDto.from(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserDto.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Utilisateur introuvable")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Auth stateless par JWT : le client supprime son token, rien à invalider côté serveur.
        return ResponseEntity.ok(Map.of("message", "Déconnecté"));
    }
}
