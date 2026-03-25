package com.smartmeat.controller;

import com.smartmeat.dto.response.CategoryResponse;
import com.smartmeat.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/categories
     * Public — returns all active categories ordered by sort_order.
     * Used by the homepage shop filter tabs and product forms.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAllActive());
    }

    /**
     * GET /api/categories/all
     * Admin — returns all categories including inactive ones.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryResponse>> getAllForAdmin() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    /**
     * GET /api/categories/{id}
     * Public — returns a single category by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    /**
     * POST /api/categories
     * Admin only — create a new category.
     * Body: { "name": "Fish", "icon": "🐟", "sortOrder": 1 }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@RequestBody Map<String, Object> body) {
        String name      = (String) body.get("name");
        String icon      = (String) body.get("icon");
        Integer sortOrder = body.get("sortOrder") != null
                ? Integer.valueOf(body.get("sortOrder").toString()) : 0;

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(categoryService.create(name, icon, sortOrder));
    }

    /**
     * PUT /api/categories/{id}
     * Admin only — update an existing category.
     * Body: { "name": "Fresh Fish", "icon": "🐟", "sortOrder": 1, "active": true }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String name       = (String) body.get("name");
        String icon       = (String) body.get("icon");
        Integer sortOrder = body.get("sortOrder") != null
                ? Integer.valueOf(body.get("sortOrder").toString()) : null;
        Boolean active    = body.get("active") != null
                ? Boolean.valueOf(body.get("active").toString()) : null;

        return ResponseEntity.ok(categoryService.update(id, name, icon, sortOrder, active));
    }

    /**
     * DELETE /api/categories/{id}
     * Admin only — deletes a category (fails if products are attached).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}