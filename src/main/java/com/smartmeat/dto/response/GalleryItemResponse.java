package com.smartmeat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GalleryItemResponse {
    private String id;
    private String type;
    private String src;
    private String caption;
    private int    sortOrder;
}