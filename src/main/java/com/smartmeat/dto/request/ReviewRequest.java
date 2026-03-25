package com.smartmeat.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//── Review ────────────────────────────────────────────────────────────────────
@Data public class ReviewRequest {
 Long orderId;
 @NotNull @Min(1) @Max(5) Integer rating;
 String comment;
 List<String> tags;
 String name; // for guest reviews
}
