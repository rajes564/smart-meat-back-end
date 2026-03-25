package com.smartmeat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

//── Principal stored in SecurityContext ───────────────────────────────────────
@Getter
@AllArgsConstructor
public class AuthenticatedUser {
 private final Long id;
 private final String mobile;
 private final String role;
}