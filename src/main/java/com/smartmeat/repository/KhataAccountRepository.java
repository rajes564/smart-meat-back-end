package com.smartmeat.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.KhataAccount;

@Repository
public interface KhataAccountRepository extends JpaRepository<KhataAccount, Long> {
    Optional<KhataAccount> findByCustomerId(Long customerId);
    List<KhataAccount> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(a.currentDue), 0) FROM KhataAccount a WHERE a.active = true")
    BigDecimal totalOutstanding();
}
