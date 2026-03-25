package com.smartmeat.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Data;

//── Review ────────────────────────────────────────────────────────────────────
@Data @Builder public class ReviewResponse {
 Long id;
 String name;
 int rating;
 String comment;
 List<String> tags;
 boolean approved;
 Instant createdAt;
}