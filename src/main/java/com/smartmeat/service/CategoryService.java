package com.smartmeat.service;


import com.smartmeat.dto.response.CategoryResponse;
import com.smartmeat.entity.Category;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.CategoryRepository;
import com.smartmeat.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;

    /** Returns all active categories ordered by sort_order — used by public homepage/shop. */
    public List<CategoryResponse> getAllActive() {
        return categoryRepo.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Returns all categories (including inactive) — used by admin panel. */
    public List<CategoryResponse> getAll() {
        return categoryRepo.findAll()
                .stream()
                .sorted((a, b) -> {
                    int s = Integer.compare(
                            a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0);
                    return s != 0 ? s : a.getName().compareTo(b.getName());
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CategoryResponse create(String name, String icon, Integer sortOrder) {
        if (categoryRepo.findBySlug(toSlug(name)).isPresent()) {
            throw new BusinessException("Category already exists: " + name);
        }
        Category category = Category.builder()
                .name(name)
                .slug(toSlug(name))
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .active(true)
                .build();
        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, String name, String icon, Integer sortOrder, Boolean active) {
        Category category = findById(id);
        if (name != null)       { category.setName(name); category.setSlug(toSlug(name)); }
        if (icon != null)        category.setIcon(icon);
        if (sortOrder != null)   category.setSortOrder(sortOrder);
        if (active != null)      category.setActive(active);
        return toResponse(categoryRepo.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findById(id);
        long productCount = productRepo.countByCategory(category);
        if (productCount > 0) {
            throw new BusinessException(
                    "Cannot delete category — it has " + productCount + " product(s). " +
                    "Remove or reassign the products first.");
        }
        categoryRepo.delete(category);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Category findById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    /** Converts a display name to a URL-friendly slug, e.g. "Fresh Fish" → "fresh-fish" */
    private String toSlug(String name) {
        return name.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    public CategoryResponse toResponse(Category c) {
        long productCount = productRepo.countByCategory(c);
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getSlug())
                .icon(c.getIcon())
                .sortOrder(c.getSortOrder())
                .active(c.isActive())
                .productCount(productCount)
                .build();
    }
}