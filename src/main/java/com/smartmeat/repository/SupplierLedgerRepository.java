package com.smartmeat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.SupplierLedgerEntry;

@Repository
public interface SupplierLedgerRepository extends JpaRepository<SupplierLedgerEntry, Long> {
    List<SupplierLedgerEntry> findBySupplierId(Long supplierId);
    List<SupplierLedgerEntry> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
}
