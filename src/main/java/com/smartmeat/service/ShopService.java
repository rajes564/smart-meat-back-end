package com.smartmeat.service;



import com.smartmeat.dto.*;
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
    private void syncHeroSlides(
            ShopSettings settings,
            List<HeroSlideMeta> metaList,
            Map<String, MultipartFile> heroFiles
    ) {
        if (metaList == null) return;
     
        Long settingsId = settings.getId();
     
        // ── DEBUG: log what we received so you can verify isNew is correct ──────
        metaList.forEach(m -> log.info(
            "HeroSlide received → id={} isNew={} caption='{}'", m.getId(), m.isNew(), m.getCaption()
        ));
     
        Map<String, HeroSlide> existingByClientId = heroSlideRepo
                .findByShopSettingsIdOrderBySortOrderAsc(settingsId)
                .stream()
                .collect(Collectors.toMap(HeroSlide::getClientId, s -> s));
     
        List<String> keptClientIds = new ArrayList<>();
     
        for (int i = 0; i < metaList.size(); i++) {
            HeroSlideMeta meta = metaList.get(i);
            String clientId = meta.getId();
            keptClientIds.add(clientId);
     
            if (meta.isNew()) {
                // NEW slide — save file and insert row
                MultipartFile file = heroFiles.get(clientId);
                if (file == null || file.isEmpty()) {
                    log.warn("Hero slide {} marked as new but no file found — skipping", clientId);
                    continue;
                }
                String url = saveFile(file, "hero");
                HeroSlide slide = HeroSlide.builder()
                        .clientId(clientId)
                        .src(url)
                        .caption(meta.getCaption())
                        .sortOrder(i)
                        .shopSettings(settings)
                        .build();
                heroSlideRepo.save(slide);
                log.info("Hero slide saved → clientId={} url={}", clientId, url);
     
            } else {
                // EXISTING slide — update caption + order only
                HeroSlide slide = existingByClientId.get(clientId);
                if (slide == null) {
                    log.warn("Hero slide {} not found in DB — skipping", clientId);
                    continue;
                }
                slide.setCaption(meta.getCaption());
                slide.setSortOrder(i);
                heroSlideRepo.save(slide);
            }
        }
     
        if (keptClientIds.isEmpty()) {
            heroSlideRepo.deleteAllByShopSettingsId(settingsId);
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
     
        metaList.forEach(m -> log.info(
            "GalleryItem received → id={} isNew={} type='{}' caption='{}'",
            m.getId(), m.isNew(), m.getType(), m.getCaption()
        ));
     
        Map<String, GalleryItem> existingByClientId = galleryItemRepo
                .findByShopSettingsIdOrderBySortOrderAsc(settingsId)
                .stream()
                .collect(Collectors.toMap(GalleryItem::getClientId, g -> g));
     
        List<String> keptClientIds = new ArrayList<>();
     
        for (int i = 0; i < metaList.size(); i++) {
            GalleryItemMeta meta = metaList.get(i);
            String clientId = meta.getId();
            keptClientIds.add(clientId);
     
            if (meta.isNew()) {
                MultipartFile file = galleryFiles.get(clientId);
                if (file == null || file.isEmpty()) {
                    log.warn("Gallery item {} marked as new but no file found — skipping", clientId);
                    continue;
                }
                String folder = "video".equals(meta.getType()) ? "gallery/videos" : "gallery/images";
                String url = saveFile(file, folder);
                GalleryItem item = GalleryItem.builder()
                        .clientId(clientId)
                        .type(meta.getType())
                        .src(url)
                        .caption(meta.getCaption())
                        .sortOrder(i)
                        .shopSettings(settings)
                        .build();
                galleryItemRepo.save(item);
                log.info("Gallery item saved → clientId={} url={}", clientId, url);
     
            } else {
                GalleryItem item = existingByClientId.get(clientId);
                if (item == null) {
                    log.warn("Gallery item {} not found in DB — skipping", clientId);
                    continue;
                }
                item.setCaption(meta.getCaption());
                item.setSortOrder(i);
                galleryItemRepo.save(item);
            }
        }
     
        if (keptClientIds.isEmpty()) {
            galleryItemRepo.deleteAllByShopSettingsId(settingsId);
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