package com.smartmeat.dto.response;

import lombok.Builder;
import lombok.Data;

//── Auth ─────────────────────────────────────────────────────────────────────
@Data @Builder public class AuthResponse {
 String token;
 String role;
 Long userId;
 String name;
 String mobile;
 String avatarUrl;
}