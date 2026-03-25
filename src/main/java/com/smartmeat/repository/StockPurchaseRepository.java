package com.smartmeat.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.StockPurchase;

@Repository
public interface StockPurchaseRepository extends JpaRepository<StockPurchase, Long> {
    List<StockPurchase> findAllByOrderByPurchaseDateDescCreatedAtDesc();

    @Query("SELECT s FROM StockPurchase s WHERE s.purchaseDate BETWEEN :from AND :to ORDER BY s.purchaseDate DESC")
    List<StockPurchase> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
        SELECT supplier_name,
               COALESCE(SUM(total_cost), 0) as total,
               COALESCE(SUM(amount_paid), 0) as paid,
               COALESCE(SUM(total_cost - amount_paid), 0) as due
        FROM stock_purchases
        GROUP BY supplier_name
        HAVING SUM(total_cost - amount_paid) > 0
        ORDER BY due DESC
        """, nativeQuery = true)
    List<Object[]> supplierBalances();

    @Query("SELECT COALESCE(SUM(s.totalCost), 0) FROM StockPurchase s WHERE EXTRACT(MONTH FROM s.purchaseDate) = EXTRACT(MONTH FROM CURRENT_DATE)")
    BigDecimal monthTotalCost();
}