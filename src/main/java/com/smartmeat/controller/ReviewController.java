package com.smartmeat.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmeat.dto.request.ReviewRequest;
import com.smartmeat.dto.response.ReviewResponse;
import com.smartmeat.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//── Reviews Controller ────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
 private final ReviewService reviewService;

 @GetMapping("/public")
 public ResponseEntity<List<ReviewResponse>> getPublic() {
     return ResponseEntity.ok(reviewService.getApproved());
 }

 @PostMapping("/submit")
 public ResponseEntity<ReviewResponse> submit(@Valid @RequestBody ReviewRequest req) {
     return ResponseEntity.ok(reviewService.submit(req));
 }

 @GetMapping
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<List<ReviewResponse>> getAll() {
     return ResponseEntity.ok(reviewService.getAll());
 }

 @PatchMapping("/{id}/approve")
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<ReviewResponse> approve(@PathVariable Long id) {
     return ResponseEntity.ok(reviewService.approve(id));
 }

 @DeleteMapping("/{id}")
 @PreAuthorize("hasRole('ADMIN')")
 public ResponseEntity<Void> delete(@PathVariable Long id) {
     reviewService.delete(id);
     return ResponseEntity.noContent().build();
 }
}