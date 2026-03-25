package com.smartmeat.security;

import com.smartmeat.dto.response.AuthenticatedUser;
import com.smartmeat.entity.User;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;



// ── Helper to get current user entity ────────────────────────────────────────
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public AuthenticatedUser principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (p instanceof AuthenticatedUser au) return au;
        return null;
    }

    public User currentUser() {
        AuthenticatedUser ap = principal();
        
        if (ap == null) throw new RuntimeException("Not authenticated");
        
        return userRepository.findById(ap.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Long currentUserId() {
        AuthenticatedUser ap = principal();
        return ap != null ? ap.getId() : null;
    }

    public boolean isAdmin() {
        AuthenticatedUser ap = principal();
        return ap != null && "ADMIN".equals(ap.getRole());
    }

    public boolean isSeller() {
        AuthenticatedUser ap = principal();
        return ap != null && "SELLER".equals(ap.getRole());
    }
}
