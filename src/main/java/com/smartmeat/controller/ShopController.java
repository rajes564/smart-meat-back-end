package com.smartmeat.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.smartmeat.dto.request.ShopSettingsRequest;
import com.smartmeat.dto.response.ShopSettingsResponse;
import com.smartmeat.service.ShopService;

import lombok.RequiredArgsConstructor;

//── Shop Settings Controller ──────────────────────────────────────────────────
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {
 private final ShopService shopService;

 @GetMapping("/settings")
 public ResponseEntity<ShopSettingsResponse> getSettings() {
     return ResponseEntity.ok(shopService.getSettings());
 }

 @PutMapping(value = "/settings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<ShopSettingsResponse> update(
         @RequestPart("settings") ShopSettingsRequest req,
         @RequestPart(value = "logo", required = false) MultipartFile logo,
         @RequestPart(value = "banner", required = false) MultipartFile banner) {
     return ResponseEntity.ok(shopService.update(req, logo, banner));
 }

 @PatchMapping("/status")
 @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
 public ResponseEntity<Map<String, String>> toggleStatus(@RequestParam String status) {
     return ResponseEntity.ok(shopService.updateStatus(status));
 }
}