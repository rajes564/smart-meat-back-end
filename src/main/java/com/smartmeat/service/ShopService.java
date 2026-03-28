package com.smartmeat.service;

import com.smartmeat.dto.request.ShopSettingsRequest;
import com.smartmeat.dto.response.ShopSettingsResponse;
import com.smartmeat.entity.ShopSettings;
import com.smartmeat.repository.ShopSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopSettingsRepository settingsRepo;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ShopSettingsResponse getSettings() {
        return settingsRepo.findFirst()
                .map(this::toResponse)
                .orElseGet(() -> ShopSettingsResponse.builder()
                        .status("OPEN")
                        .cashBalance(BigDecimal.ZERO)
                        .accountBalance(BigDecimal.ZERO)
                        .build());
    }

    @Transactional
    public ShopSettingsResponse update(ShopSettingsRequest req, MultipartFile logo, MultipartFile banner) {
        ShopSettings settings = settingsRepo.findFirst().orElseGet(ShopSettings::new);

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

        // Save opening balances when set via settings page
        if (req.getCashBalance()    != null) settings.setCashBalance(req.getCashBalance());
        if (req.getAccountBalance() != null) settings.setAccountBalance(req.getAccountBalance());

        if (logo   != null && !logo.isEmpty())   settings.setLogoUrl(saveFile(logo,   "logos"));
        if (banner != null && !banner.isEmpty())  settings.setBannerUrl(saveFile(banner, "banners"));

        return toResponse(settingsRepo.save(settings));
    }

    public Map<String, String> updateStatus(String status) {
        ShopSettings settings = settingsRepo.findFirst().orElseGet(ShopSettings::new);
        settings.setStatus(status.toUpperCase());
        settingsRepo.save(settings);
        return Map.of("status", settings.getStatus());
    }

    /** Called by OrderService/InventoryService to credit/debit balances */
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

    private String saveFile(MultipartFile file, String folder) {
        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "_"
                    + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.copy(file.getInputStream(), dir.resolve(name));
            return baseUrl + "/uploads/" + folder + "/" + name;
        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    private ShopSettingsResponse toResponse(ShopSettings s) {
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
                .build();
    }
}