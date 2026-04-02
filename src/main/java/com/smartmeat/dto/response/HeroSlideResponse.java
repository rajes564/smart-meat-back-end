package com.smartmeat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HeroSlideResponse {
    private String id;       // clientId
    private String src;
    private String caption;
    private int    sortOrder;
}