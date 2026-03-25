package com.smartmeat.dto.response;

import lombok.Builder;
import lombok.Data;

//── Category ──────────────────────────────────────────────────────────────────
@Data @Builder public class CategoryResponse {
 Long id;
 String name;
 String slug;
 String icon;
 Integer sortOrder;
 boolean active;
 long productCount;
}
