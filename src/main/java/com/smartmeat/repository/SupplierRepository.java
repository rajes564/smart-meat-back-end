package com.smartmeat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByActiveTrueOrderByNameAsc();
    List<Supplier> findAllByOrderByNameAsc();
    Optional<Supplier> findByMobile(String mobile);
}
 