package com.smartmeat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//── Auth ─────────────────────────────────────────────────────────────────────
@Data public class LoginRequest {
 @NotBlank String mobile;
 @NotBlank String password;
}
