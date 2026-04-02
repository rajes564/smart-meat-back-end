package com.smartmeat.controller;


import com.smartmeat.dto.request.ShopSettingsRequest;
import com.smartmeat.dto.response.ShopSettingsResponse;
import com.smartmeat.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {
 
    private final ShopService shopService;
 
    @GetMapping("/settings")
    public ResponseEntity<ShopSettingsResponse> getSettings() {
        return ResponseEntity.ok(shopService.getSettings());
    }
 
    // IMPORTANT: Do NOT declare @RequestPart for logo/banner alongside
    // MultipartHttpServletRequest — Spring's resolver reads the multipart
    // stream for @RequestPart eagerly, then MultipartHttpServletRequest tries
    // to read it again → stream already consumed → ERR_CONNECTION_RESET.
    //
    // Fix: bind ONLY the JSON "settings" part via @RequestPart, then read
    // all file parts (logo, banner, heroSlide_*, galleryItem_*) from the
    // MultipartHttpServletRequest. One stream read, no conflict.
    @PutMapping(value = "/settings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShopSettingsResponse> update(
            @RequestPart("settings") ShopSettingsRequest req,
            MultipartHttpServletRequest multipartRequest
    ) {
        log.info("Shop settings update — parts: {}", multipartRequest.getFileMap().keySet());
 
        MultipartFile logo   = multipartRequest.getFile("logo");
        MultipartFile banner = multipartRequest.getFile("banner");
 
        Map<String, MultipartFile> heroFiles    = new HashMap<>();
        Map<String, MultipartFile> galleryFiles = new HashMap<>();
 
        multipartRequest.getFileMap().forEach((partName, file) -> {
            if (partName.startsWith("heroSlide_")) {
                heroFiles.put(partName.substring("heroSlide_".length()), file);
            } else if (partName.startsWith("galleryItem_")) {
                galleryFiles.put(partName.substring("galleryItem_".length()), file);
            }
        });
 
        return ResponseEntity.ok(
                shopService.update(req, logo, banner, heroFiles, galleryFiles)
        );
    }
 
    @PatchMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<Map<String, String>> toggleStatus(@RequestParam String status) {
        return ResponseEntity.ok(shopService.updateStatus(status));
    }
}
 