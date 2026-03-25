package com.smartmeat.controller;

import com.smartmeat.dto.request.ProductRequest;
import com.smartmeat.dto.response.ProductResponse;
import com.smartmeat.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // PUBLIC
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(productService.getAll(categoryId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    // ADMIN ONLY
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestPart("product") ProductRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(productService.create(req, image));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestPart("product") ProductRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(productService.update(id, req, image));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> toggleAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleAvailability(id));
    }
}
