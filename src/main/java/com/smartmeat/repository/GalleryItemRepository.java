package com.smartmeat.repository;

import com.smartmeat.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
 
@Repository
public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
 
    List<GalleryItem> findByShopSettingsIdOrderBySortOrderAsc(Long shopSettingsId);
 
    java.util.Optional<GalleryItem> findByClientId(String clientId);
 
    @Modifying
    @Query("""
        DELETE FROM GalleryItem g
        WHERE g.shopSettings.id = :settingsId
          AND g.clientId NOT IN :keptClientIds
        """)
    void deleteRemovedItems(@Param("settingsId") Long settingsId,
                            @Param("keptClientIds") List<String> keptClientIds);
 
    @Modifying
    @Query("DELETE FROM GalleryItem g WHERE g.shopSettings.id = :settingsId")
    void deleteAllByShopSettingsId(@Param("settingsId") Long settingsId);
}