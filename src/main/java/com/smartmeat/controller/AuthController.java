package com.smartmeat.controller;

import com.smartmeat.dto.request.LoginRequest;
import com.smartmeat.dto.request.RegisterRequest;
import com.smartmeat.dto.response.AuthResponse;
import com.smartmeat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
    	System.out.println("----------------------");
    	System.out.println(req);
    	System.out.println("----------------------");
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("X-Refresh-Token") String token) {
        return ResponseEntity.ok(authService.refresh(token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(authService.currentUser());
    }
}
