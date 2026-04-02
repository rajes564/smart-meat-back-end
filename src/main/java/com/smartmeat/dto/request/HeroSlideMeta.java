package com.smartmeat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HeroSlideMeta {
    private String  id;       // client-side UUID
    private String  caption;
    private String  src;      // null when isNew=true (file is uploaded separately)
    private boolean isNew;
}