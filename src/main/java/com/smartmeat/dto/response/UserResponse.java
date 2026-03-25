package com.smartmeat.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

//── User ──────────────────────────────────────────────────────────────────────
@Data @Builder public class UserResponse {
 Long id;
 String name;
 String mobile;
 String email;
 String role;
 String avatarUrl;
 boolean active;
 Instant createdAt;
}
