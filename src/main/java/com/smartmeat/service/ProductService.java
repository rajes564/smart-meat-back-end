package com.smartmeat.service;

import com.smartmeat.dto.request.ProductRequest;
import com.smartmeat.dto.response.ProductResponse;
import com.smartmeat.entity.Category;
import com.smartmeat.entity.Product;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.CategoryRepository;
import com.smartmeat.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SseService sseService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    public List<ProductResponse> getAll(Long categoryId, String status) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryIdAndAvailableTrue(categoryId);
        } else if ("available".equalsIgnoreCase(status)) {
            products = productRepository.findByAvailableTrueOrderBySortOrderAscNameAsc();
        } else {
            products = productRepository.findAllByOrderBySortOrderAscNameAsc();
        }
        // Update computed stock status
        products.forEach(p -> p.setStockStatus(p.computedStockStatus()));
        return products.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setStockStatus(product.computedStockStatus());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest req, MultipartFile image) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .category(category)
                .pricePerKg(req.getPricePerKg())
                .costPerKg(req.getCostPerKg())
                .stockQty(BigDecimal.ZERO)          // stock is added only via Inventory → Add Purchase
                .minStockLevel(req.getMinStockLevel() != null ? req.getMinStockLevel() : BigDecimal.valueOf(5))
                .minOrderQty(req.getMinOrderQty() != null ? req.getMinOrderQty() : BigDecimal.valueOf(0.5))
                .orderStep(req.getOrderStep() != null ? req.getOrderStep() : BigDecimal.valueOf(0.5))
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .available(true)
                .build();

        if (image != null && !image.isEmpty()) {
            product.setImageUrl(saveImage(image));
        }

        product.setStockStatus(product.computedStockStatus());
        Product saved = productRepository.save(product);
        log.info("Product created: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setCategory(category);
        product.setPricePerKg(req.getPricePerKg());
        product.setCostPerKg(req.getCostPerKg());
        // stockQty intentionally NOT updated here — use Inventory → Add Purchase
        if (req.getMinStockLevel() != null) product.setMinStockLevel(req.getMinStockLevel());
        if (req.getMinOrderQty()   != null) product.setMinOrderQty(req.getMinOrderQty());
        if (req.getOrderStep()     != null) product.setOrderStep(req.getOrderStep());
        if (req.getSortOrder()     != null) product.setSortOrder(req.getSortOrder());

        if (image != null && !image.isEmpty()) {
            deleteOldImage(product.getImageUrl());
            product.setImageUrl(saveImage(image));
        }

        product.setStockStatus(product.computedStockStatus());
        checkLowStock(product);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        deleteOldImage(product.getImageUrl());
        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse toggleAvailability(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        product.setAvailable(!product.isAvailable());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void reduceStock(Long productId, BigDecimal qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        BigDecimal newQty = product.getStockQty().subtract(qty);
        product.setStockQty(newQty.max(BigDecimal.ZERO));
        product.setStockStatus(product.computedStockStatus());
        productRepository.save(product);
        checkLowStock(product);
    }

    @Transactional
    public void increaseStock(Long productId, BigDecimal qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
        product.setStockQty(product.getStockQty().add(qty));
        product.setStockStatus(product.computedStockStatus());
        productRepository.save(product);
    }

    private void checkLowStock(Product product) {
        if ("LOW_STOCK".equals(product.getStockStatus()) || "OUT_OF_STOCK".equals(product.getStockStatus())) {
            sseService.lowStockAlert(toResponse(product));
        }
    }

    private String saveImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir, "products");
            Files.createDirectories(uploadPath);
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename()
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            Path dest = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), dest);
            return baseUrl + "/uploads/products/" + filename;
        } catch (IOException e) {
            log.error("Failed to save image", e);
            throw new RuntimeException("Image upload failed");
        }
    }

    private void deleteOldImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
            Path path = Paths.get(uploadDir, "products", filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete old image: {}", imageUrl);
        }
    }

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categoryIcon(p.getCategory() != null ? p.getCategory().getIcon() : null)
                .pricePerKg(p.getPricePerKg())
                .costPerKg(p.getCostPerKg())
                .stockQty(p.getStockQty())
                .minStockLevel(p.getMinStockLevel())
                .minOrderQty(p.getMinOrderQty())
                .orderStep(p.getOrderStep())
                .stockStatus(p.getStockStatus())
                .imageUrl(p.getImageUrl())
                .available(p.isAvailable())
                .sortOrder(p.getSortOrder())
                .build();
    }
}