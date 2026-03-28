package com.smartmeat.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.Order;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = :date ORDER BY o.createdAt DESC")
    Page<Order> findByDate(@Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND DATE(o.createdAt) = :date ORDER BY o.createdAt DESC")
    Page<Order> findByStatusAndDate(@Param("status") String status, @Param("date") LocalDate date, Pageable pageable);

 

    @Query("SELECT COUNT(o) FROM Order o " +
    	       "WHERE o.createdAt >= :startOfDay " +
    	       "AND o.createdAt < :endOfDay " +
    	       "AND o.status <> 'CANCELLED'")
    	long todayTransactionCount(@Param("startOfDay") Instant startOfDay,
    	                           @Param("endOfDay") Instant endOfDay);

    	@Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
    	       "WHERE o.createdAt >= :startOfDay " +
    	       "AND o.createdAt < :endOfDay " +
    	       "AND o.status <> 'CANCELLED'")
    	BigDecimal todayTotalSales(@Param("startOfDay") Instant startOfDay,
    	                           @Param("endOfDay") Instant endOfDay);


    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o " +
           "WHERE EXTRACT(MONTH FROM o.createdAt) = EXTRACT(MONTH FROM CURRENT_DATE) " +
           "AND o.status <> 'CANCELLED'")
    BigDecimal monthTotalSales();

    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING','ACCEPTED','PREPARING') ORDER BY o.createdAt ASC")
    List<Order> findActiveOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING'")
    long countPendingOrders();

    @Query(value = """
        SELECT COALESCE(
            MAX(CAST(SPLIT_PART(order_number, '-', 3) AS BIGINT)),
            100
        )
        FROM orders
        WHERE order_number ~ '^ORD-[0-9]{6}-[0-9]+$'
        """, nativeQuery = true)
    Long findMaxOrderSeq();

    @Query(value = """
        SELECT DATE(created_at) as day, COALESCE(SUM(total), 0) as total
        FROM orders
        WHERE created_at >= NOW() - INTERVAL '7 days'
          AND status != 'CANCELLED'
        GROUP BY DATE(created_at)
        ORDER BY day ASC
        """, nativeQuery = true)
    List<Object[]> weeklySalesData();

    @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) BETWEEN :from AND :to AND o.status != 'CANCELLED' ORDER BY o.createdAt ASC")
    List<Order> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
