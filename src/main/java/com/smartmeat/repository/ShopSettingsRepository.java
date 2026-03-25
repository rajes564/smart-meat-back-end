package com.smartmeat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.ShopSettings;

@Repository
public  interface ShopSettingsRepository extends JpaRepository<ShopSettings, Long> {
    @Query("SELECT s FROM ShopSettings s ORDER BY s.id ASC LIMIT 1")
    Optional<ShopSettings> findFirst();
}