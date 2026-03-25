package com.smartmeat.dto.response;

import lombok.*;
import java.time.Instant;


// ── Error ─────────────────────────────────────────────────────────────────────
@Data @Builder @AllArgsConstructor @NoArgsConstructor public class ErrorResponse {
    int status;
    String error;
    String message;
    Instant timestamp;
}
