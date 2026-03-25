package com.smartmeat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data public class RegisterRequest {
    @NotBlank @Size(min=2, max=100) String name;
    @NotBlank @Size(min=10, max=15) String mobile;
    String email;
    @NotBlank @Size(min=6) String password;
}
