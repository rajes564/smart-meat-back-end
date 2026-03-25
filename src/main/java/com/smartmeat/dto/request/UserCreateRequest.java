package com.smartmeat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//── User Management ───────────────────────────────────────────────────────────
@Data public class UserCreateRequest {
 @NotBlank String name;
 @NotBlank String mobile;
 String email;
 @NotBlank String password;
 @NotBlank String role;
}