package com.smartstudy.controller;

import com.smartstudy.model.AppUser;
import com.smartstudy.repository.AppUserRepository;
import com.smartstudy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            String name = (String) body.getOrDefault("name", username);
            String photoUrl = (String) body.getOrDefault("photoUrl", "https://ui-avatars.com/api/?name=" + name.replace(" ", "+"));

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
            }
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "username already exists"));
            }

            AppUser u = authService.register(username, password, name, photoUrl);
            return ResponseEntity.ok(authService.toPublicUser(u));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            Optional<AppUser> userOpt = authService.authenticate(username, password);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(AuthService.SESSION_USER_ID, userOpt.get().getId());
            return ResponseEntity.ok(authService.toPublicUser(userOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        Object uid = session.getAttribute(AuthService.SESSION_USER_ID);
        if (uid == null) return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        Optional<AppUser> u = userRepository.findById(Long.valueOf(uid.toString()));
        return u.<ResponseEntity<?>>map(appUser -> ResponseEntity.ok(authService.toPublicUser(appUser)))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Not logged in")));
    }
}
