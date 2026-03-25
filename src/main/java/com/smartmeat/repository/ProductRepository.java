package com.smartmeat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.Category;
import com.smartmeat.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderBySortOrderAscNameAsc();
    List<Product> findByAvailableTrueOrderBySortOrderAscNameAsc();
    List<Product> findByCategoryIdAndAvailableTrue(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.stockQty <= p.minStockLevel AND p.available = true")
    List<Product> findLowStockProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.available = true")
    long countAvailable();
    
    long countByCategory(Category category);

}