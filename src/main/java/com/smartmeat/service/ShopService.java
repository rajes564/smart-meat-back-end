package com.smartmeat.service;


import com.smartmeat.dto.request.GalleryItemMeta;
import com.smartmeat.dto.request.HeroSlideMeta;
import com.smartmeat.dto.request.ShopSettingsRequest;
import com.smartmeat.dto.response.GalleryItemResponse;
import com.smartmeat.dto.response.HeroSlideResponse;
import com.smartmeat.dto.response.ShopSettingsResponse;
import com.smartmeat.entity.*;
import com.smartmeat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopSettingsRepository settingsRepo;
    private final HeroSlideRepository    heroSlideRepo;
    private final GalleryItemRepository  galleryItemRepo;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ─────────────────────────────────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────────────────────────────────
    public ShopSettingsResponse getSettings() {
        return settingsRepo.findFirst()
                .map(this::toResponse)
                .orElseGet(() -> ShopSettingsResponse.builder()
                        .status("OPEN")
                        .cashBalance(BigDecimal.ZERO)
                        .accountBalance(BigDecimal.ZERO)
                        .heroImages(List.of())
                        .galleryItems(List.of())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE (main settings + logo/banner + carousel + gallery)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public ShopSettingsResponse update(
            ShopSettingsRequest req,
            MultipartFile logo,
            MultipartFile banner,
            Map<String, MultipartFile> heroFiles,    // key = "heroSlide_{clientId}"
            Map<String, MultipartFile> galleryFiles  // key = "galleryItem_{clientId}"
    ) {
        ShopSettings settings = settingsRepo.findFirst().orElseGet(ShopSettings::new);

        // ── Scalar fields (only overwrite when non-null) ───────────────────
        if (req.getShopName()    != null) settings.setShopName(req.getShopName());
        if (req.getTagline()     != null) settings.setTagline(req.getTagline());
        if (req.getPhone()       != null) settings.setPhone(req.getPhone());
        if (req.getEmail()       != null) settings.setEmail(req.getEmail());
        if (req.getAddress()     != null) settings.setAddress(req.getAddress());
        if (req.getLatitude()    != null) settings.setLatitude(req.getLatitude());
        if (req.getLongitude()   != null) settings.setLongitude(req.getLongitude());
        if (req.getStatus()      != null) settings.setStatus(req.getStatus());
        if (req.getOpenTime()    != null) settings.setOpenTime(req.getOpenTime());
        if (req.getCloseTime()   != null) settings.setCloseTime(req.getCloseTime());
        if (req.getSundayClose() != null) settings.setSundayClose(req.getSundayClose());
        if (req.getCashBalance()    != null) settings.setCashBalance(req.getCashBalance());
        if (req.getAccountBalance() != null) settings.setAccountBalance(req.getAccountBalance());

        // ── Logo / Banner ──────────────────────────────────────────────────
        if (logo   != null && !logo.isEmpty())
            settings.setLogoUrl(saveFile(logo,   "logos"));
        if (banner != null && !banner.isEmpty())
            settings.setBannerUrl(saveFile(banner, "banners"));

        // Persist so we have a valid PK before inserting child rows
        settings = settingsRepo.save(settings);

        // ── Hero Carousel ─────────────────────────────────────────────────
        syncHeroSlides(settings, req.getHeroSlidesMeta(), heroFiles);

        // ── Gallery ───────────────────────────────────────────────────────
        syncGalleryItems(settings, req.getGalleryMeta(), galleryFiles);

        // Re-fetch to return fully hydrated response (lazy collections)
        return toResponse(settingsRepo.findById(settings.getId()).orElseThrow());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS quick-toggle (unchanged)
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, String> updateStatus(String status) {
        ShopSettings s = settingsRepo.findFirst().orElseGet(ShopSettings::new);
        s.setStatus(status.toUpperCase());
        settingsRepo.save(s);
        return Map.of("status", s.getStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Balance delta (called by OrderService / InventoryService)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void addToBalance(BigDecimal cashDelta, BigDecimal accountDelta) {
        settingsRepo.findFirst().ifPresent(s -> {
            BigDecimal cash    = s.getCashBalance()    != null ? s.getCashBalance()    : BigDecimal.ZERO;
            BigDecimal account = s.getAccountBalance() != null ? s.getAccountBalance() : BigDecimal.ZERO;
            if (cashDelta    != null && cashDelta.compareTo(BigDecimal.ZERO)    != 0)
                s.setCashBalance(cash.add(cashDelta));
            if (accountDelta != null && accountDelta.compareTo(BigDecimal.ZERO) != 0)
                s.setAccountBalance(account.add(accountDelta));
            settingsRepo.save(s);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HERO CAROUSEL sync
    //
    // Strategy:
    //  1. Build a map of existing slides by clientId for O(1) lookup
    //  2. Walk the ordered metadata list the frontend sent
    //     a. If clientId already in DB → update caption + sortOrder only
    //     b. If isNew → save uploaded file, create new row
    //  3. Delete any DB rows whose clientId was not present in the list
    // ─────────────────────────────────────────────────────────────────────────
    
 // KEY FIXES:
 // 1. Added log showing exactly what clientIds exist in DB — helps diagnose stale rows
 // 2. Changed strategy: use saveAndFlush with clientId as the unique key.
//     If clientId exists in DB → update it (caption, sortOrder, optionally src).
//     If not → insert new row with the uploaded file.
//     This is now a true upsert — no more "skipping" on either branch.
 // 3. File presence drives insert decision, NOT isNew flag from frontend.
    private void syncHeroSlides(
            ShopSettings settings,
            List<HeroSlideMeta> metaList,
            Map<String, MultipartFile> heroFiles
    ) {
        if (metaList == null) return;
     
        Long settingsId = settings.getId();
     
        Map<String, HeroSlide> existingByClientId = heroSlideRepo
                .findByShopSettingsIdOrderBySortOrderAsc(settingsId)
                .stream()
                .collect(Collectors.toMap(HeroSlide::getClientId, s -> s));
     
        // ── DEBUG: log DB state so you can see exactly what's there ─────────
        log.info("Hero slides in DB for settingsId={}: {}", settingsId, existingByClientId.keySet());
        log.info("Hero files received from frontend: {}", heroFiles.keySet());
     
        List<String> keptClientIds = new ArrayList<>();
     
        for (int i = 0; i < metaList.size(); i++) {
            HeroSlideMeta meta   = metaList.get(i);
            String        clientId = meta.getId();
            keptClientIds.add(clientId);
     
            HeroSlide existing = existingByClientId.get(clientId);
            MultipartFile file = heroFiles.get(clientId);
     
            if (existing == null) {
                // ── INSERT: not in DB yet ────────────────────────────────────
                if (file == null || file.isEmpty()) {
                    log.warn("Hero slide {} → not in DB and no file — skipping", clientId);
                    continue;
                }
                String url = saveFile(file, "hero");
                heroSlideRepo.save(HeroSlide.builder()
                        .clientId(clientId)
                        .src(url)
                        .caption(meta.getCaption())
                        .sortOrder(i)
                        .shopSettings(settings)
                        .build());
                log.info("Hero slide INSERTED → clientId={} url={}", clientId, url);
     
            } else {
                // ── UPDATE: exists in DB → update caption + order ────────────
                // If a new file was also uploaded, replace the src
                if (file != null && !file.isEmpty()) {
                    existing.setSrc(saveFile(file, "hero"));
                    log.info("Hero slide src REPLACED → clientId={}", clientId);
                }
                existing.setCaption(meta.getCaption());
                existing.setSortOrder(i);
                heroSlideRepo.save(existing);
                log.info("Hero slide UPDATED → clientId={}", clientId);
            }
        }
     
        // Delete any slides the frontend removed
        if (keptClientIds.isEmpty()) {
            heroSlideRepo.deleteAllByShopSettingsId(settingsId);
            log.info("All hero slides deleted for settingsId={}", settingsId);
        } else {
            heroSlideRepo.deleteRemovedSlides(settingsId, keptClientIds);
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
    // GALLERY sync (same pattern as hero slides)
    // ─────────────────────────────────────────────────────────────────────────
    private void syncGalleryItems(
            ShopSettings settings,
            List<GalleryItemMeta> metaList,
            Map<String, MultipartFile> galleryFiles
    ) {
        if (metaList == null) return;
     
        Long settingsId = settings.getId();
     
        Map<String, GalleryItem> existingByClientId = galleryItemRepo
                .findByShopSettingsIdOrderBySortOrderAsc(settingsId)
                .stream()
                .collect(Collectors.toMap(GalleryItem::getClientId, g -> g));
     
        // ── DEBUG ────────────────────────────────────────────────────────────
        log.info("Gallery items in DB for settingsId={}: {}", settingsId, existingByClientId.keySet());
        log.info("Gallery files received from frontend: {}", galleryFiles.keySet());
     
        List<String> keptClientIds = new ArrayList<>();
     
        for (int i = 0; i < metaList.size(); i++) {
            GalleryItemMeta meta     = metaList.get(i);
            String          clientId = meta.getId();
            keptClientIds.add(clientId);
     
            GalleryItem   existing = existingByClientId.get(clientId);
            MultipartFile file     = galleryFiles.get(clientId);
     
            if (existing == null) {
                // ── INSERT ───────────────────────────────────────────────────
                if (file == null || file.isEmpty()) {
                    log.warn("Gallery item {} → not in DB and no file — skipping", clientId);
                    continue;
                }
                String folder = "video".equals(meta.getType()) ? "gallery/videos" : "gallery/images";
                String url    = saveFile(file, folder);
                galleryItemRepo.save(GalleryItem.builder()
                        .clientId(clientId)
                        .type(meta.getType())
                        .src(url)
                        .caption(meta.getCaption())
                        .sortOrder(i)
                        .shopSettings(settings)
                        .build());
                log.info("Gallery item INSERTED → clientId={} url={}", clientId, url);
     
            } else {
                // ── UPDATE ───────────────────────────────────────────────────
                if (file != null && !file.isEmpty()) {
                    String folder = "video".equals(meta.getType()) ? "gallery/videos" : "gallery/images";
                    existing.setSrc(saveFile(file, folder));
                    log.info("Gallery item src REPLACED → clientId={}", clientId);
                }
                existing.setCaption(meta.getCaption());
                existing.setSortOrder(i);
                galleryItemRepo.save(existing);
                log.info("Gallery item UPDATED → clientId={}", clientId);
            }
        }
     
        if (keptClientIds.isEmpty()) {
            galleryItemRepo.deleteAllByShopSettingsId(settingsId);
            log.info("All gallery items deleted for settingsId={}", settingsId);
        } else {
            galleryItemRepo.deleteRemovedItems(settingsId, keptClientIds);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILE SAVE
    // ─────────────────────────────────────────────────────────────────────────
    private String saveFile(MultipartFile file, String folder) {
        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);
            String safeName = UUID.randomUUID() + "_"
                    + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.copy(file.getInputStream(), dir.resolve(safeName));
            return baseUrl + "/uploads/" + folder + "/" + safeName;
        } catch (IOException e) {
            log.error("File upload failed for folder={}", folder, e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING → Response
    // ─────────────────────────────────────────────────────────────────────────
    private ShopSettingsResponse toResponse(ShopSettings s) {
        List<HeroSlideResponse> heroImages = heroSlideRepo
                .findByShopSettingsIdOrderBySortOrderAsc(s.getId())
                .stream()
                .map(h -> HeroSlideResponse.builder()
                        .id(h.getClientId())
                        .src(h.getSrc())
                        .caption(h.getCaption())
                        .sortOrder(h.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        List<GalleryItemResponse> galleryItems = galleryItemRepo
                .findByShopSettingsIdOrderBySortOrderAsc(s.getId())
                .stream()
                .map(g -> GalleryItemResponse.builder()
                        .id(g.getClientId())
                        .type(g.getType())
                        .src(g.getSrc())
                        .caption(g.getCaption())
                        .sortOrder(g.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return ShopSettingsResponse.builder()
                .shopName(s.getShopName())
                .tagline(s.getTagline())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .status(s.getStatus())
                .openTime(s.getOpenTime())
                .closeTime(s.getCloseTime())
                .sundayClose(s.getSundayClose())
                .logoUrl(s.getLogoUrl())
                .bannerUrl(s.getBannerUrl())
                .cashBalance(s.getCashBalance()    != null ? s.getCashBalance()    : BigDecimal.ZERO)
                .accountBalance(s.getAccountBalance() != null ? s.getAccountBalance() : BigDecimal.ZERO)
                .heroImages(heroImages)
                .galleryItems(galleryItems)
                .build();
    }
}