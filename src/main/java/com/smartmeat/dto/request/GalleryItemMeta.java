package com.smartmeat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//─────────────────────────────────────────────────────────────────────────────
//Nested DTO for gallery items
//─────────────────────────────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GalleryItemMeta {
 private String  id;
 private String  type;     // "image" | "video"
 private String  caption;
 private String  src;
 private boolean isNew;
}