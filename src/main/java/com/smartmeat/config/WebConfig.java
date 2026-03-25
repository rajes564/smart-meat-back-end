package com.smartmeat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve the absolute path and ensure it ends with /
        // Spring requires the trailing slash to serve files in subdirectories
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString();
        if (!absolutePath.endsWith("/") && !absolutePath.endsWith("\\")) {
            absolutePath = absolutePath + "/";
        }
        String resourceLocation = "file:" + absolutePath;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);  // cache for 1 hour in browser

        log.info("Serving uploads from: {}", resourceLocation);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}