package com.smartmeat.repository;

import com.smartmeat.entity.HeroSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeroSlideRepository extends JpaRepository<HeroSlide, Long> {
	 
    List<HeroSlide> findByShopSettingsIdOrderBySortOrderAsc(Long shopSettingsId);
 
    /** Find by client-side UUID — used to detect existing slides during save */
    java.util.Optional<HeroSlide> findByClientId(String clientId);
 
    /** Bulk-delete all slides belonging to a settings row that are NOT in the kept list */
    @Modifying
    @Query("""
        DELETE FROM HeroSlide h
        WHERE h.shopSettings.id = :settingsId
          AND h.clientId NOT IN :keptClientIds
        """)
    void deleteRemovedSlides(@Param("settingsId") Long settingsId,
                             @Param("keptClientIds") List<String> keptClientIds);
 
    /** Overload for the case where ALL slides are removed */
    @Modifying
    @Query("DELETE FROM HeroSlide h WHERE h.shopSettings.id = :settingsId")
    void deleteAllByShopSettingsId(@Param("settingsId") Long settingsId);
}
 