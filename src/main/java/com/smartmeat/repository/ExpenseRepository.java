package com.smartmeat.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.Expense;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByOrderByExpenseDateDescCreatedAtDesc();

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :from AND :to ORDER BY e.expenseDate DESC")
    List<Expense> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE EXTRACT(MONTH FROM e.expenseDate) = EXTRACT(MONTH FROM CURRENT_DATE)")
    BigDecimal monthTotalExpenses();
}