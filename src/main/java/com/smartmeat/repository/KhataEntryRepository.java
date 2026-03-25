package com.smartmeat.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.KhataEntry;

@Repository
public interface KhataEntryRepository extends JpaRepository<KhataEntry, Long> {
    List<KhataEntry> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM KhataEntry e WHERE e.account.id = :accountId AND e.entryType = 'DEBIT'")
    BigDecimal totalDebitForAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM KhataEntry e WHERE e.account.id = :accountId AND e.entryType = 'CREDIT'")
    BigDecimal totalCreditForAccount(@Param("accountId") Long accountId);
}