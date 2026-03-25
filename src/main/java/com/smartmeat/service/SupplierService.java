package com.smartmeat.service;


import com.smartmeat.entity.*;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.*;
import com.smartmeat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository       supplierRepo;
    private final SupplierLedgerRepository ledgerRepo;
    private final StockPurchaseRepository  purchaseRepo;
    private final ShopSettingsRepository   settingsRepo;
    private final SecurityUtils            securityUtils;

    // ── Supplier CRUD ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAll() {
        return supplierRepo.findAllByOrderByNameAsc().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        Supplier s = Supplier.builder()
                .name(str(body, "name"))
                .mobile(str(body, "mobile"))
                .email(str(body, "email"))
                .address(str(body, "address"))
                .products(str(body, "products"))
                .creditLimit(decimal(body, "creditLimit", BigDecimal.ZERO))
                .currentDue(BigDecimal.ZERO)
                .active(true)
                .createdBy(securityUtils.currentUser())
                .build();
        if (s.getName() == null || s.getName().isBlank())
            throw new BusinessException("Supplier name is required");
        return toMap(supplierRepo.save(s));
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Supplier s = supplierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        if (body.containsKey("name"))        s.setName(str(body, "name"));
        if (body.containsKey("mobile"))      s.setMobile(str(body, "mobile"));
        if (body.containsKey("email"))       s.setEmail(str(body, "email"));
        if (body.containsKey("address"))     s.setAddress(str(body, "address"));
        if (body.containsKey("products"))    s.setProducts(str(body, "products"));
        if (body.containsKey("creditLimit")) s.setCreditLimit(decimal(body, "creditLimit", BigDecimal.ZERO));
        if (body.containsKey("active"))      s.setActive(Boolean.parseBoolean(body.get("active").toString()));
        return toMap(supplierRepo.save(s));
    }

    // ── Supplier Ledger ───────────────────────────────────────────────────────

    public Map<String, Object> getLedger(Long supplierId) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        List<SupplierLedgerEntry> entries =
                ledgerRepo.findBySupplierIdOrderByCreatedAtDesc(supplierId);

        // Build running balance
        List<SupplierLedgerEntry> reversed = new ArrayList<>(entries);
        Collections.reverse(reversed);
        BigDecimal running = BigDecimal.ZERO;
        List<Map<String, Object>> entryMaps = new ArrayList<>();
        for (SupplierLedgerEntry e : reversed) {
            if ("DEBIT".equals(e.getEntryType()))  running = running.add(e.getAmount());
            else                                    running = running.subtract(e.getAmount());
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("id",              e.getId());
            em.put("entryType",       e.getEntryType());
            em.put("amount",          e.getAmount());
            em.put("cashAmount",      e.getCashAmount());
            em.put("accountAmount",   e.getAccountAmount());
            em.put("description",     e.getDescription());
            em.put("referenceNote",   e.getReferenceNote());
            em.put("entryDate",       e.getEntryDate() != null ? e.getEntryDate().toString() : "");
            em.put("runningBalance",  running);
            em.put("enteredBy",       e.getEnteredBy() != null ? e.getEnteredBy().getName() : "");
            entryMaps.add(0, em);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supplier",       toMap(supplier));
        result.put("entries",        entryMaps);
        result.put("runningBalance", running);
        return result;
    }

    @Transactional
    public Map<String, Object> addLedgerEntry(Map<String, Object> body) {
        Long supplierId  = Long.parseLong(body.get("supplierId").toString());
        String entryType = body.get("entryType").toString().toUpperCase();
        BigDecimal amount       = decimal(body, "amount", BigDecimal.ZERO);
        BigDecimal cashAmt      = decimal(body, "cashAmount",    BigDecimal.ZERO);
        BigDecimal accountAmt   = decimal(body, "accountAmount", BigDecimal.ZERO);
        String description      = str(body, "description");

        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        SupplierLedgerEntry entry = SupplierLedgerEntry.builder()
                .supplier(supplier)
                .entryType(entryType)
                .amount(amount)
                .cashAmount(cashAmt)
                .accountAmount(accountAmt)
                .description(description)
                .referenceNote(str(body, "referenceNote"))
                .enteredBy(securityUtils.currentUser())
                .entryDate(LocalDate.now())
                .build();
        ledgerRepo.save(entry);

        // Update supplier due balance
        if ("DEBIT".equals(entryType)) {
            supplier.setCurrentDue(supplier.getCurrentDue().add(amount));
        } else {
            supplier.setCurrentDue(
                supplier.getCurrentDue().subtract(amount).max(BigDecimal.ZERO));
            // Credit (payment received) → deduct from cash/account balances
            updateShopBalance(cashAmt.negate(), accountAmt.negate());
        }
        supplierRepo.save(supplier);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id",        entry.getId());
        res.put("entryType", entry.getEntryType());
        res.put("amount",    entry.getAmount());
        res.put("newDue",    supplier.getCurrentDue());
        return res;
    }

    // ── Shop balance management ───────────────────────────────────────────────

    public Map<String, Object> getBalances() {
        ShopSettings s = settingsRepo.findFirst().orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cashBalance",    s != null ? s.getCashBalance()    : BigDecimal.ZERO);
        m.put("accountBalance", s != null ? s.getAccountBalance() : BigDecimal.ZERO);
        return m;
    }

    @Transactional
    public void updateShopBalance(BigDecimal cashDelta, BigDecimal accountDelta) {
        settingsRepo.findFirst().ifPresent(s -> {
            if (cashDelta    != null) s.setCashBalance(s.getCashBalance().add(cashDelta));
            if (accountDelta != null) s.setAccountBalance(s.getAccountBalance().add(accountDelta));
            settingsRepo.save(s);
        });
    }

    @Transactional
    public Map<String, Object> adjustBalance(String type, BigDecimal amount, String note) {
        ShopSettings s = settingsRepo.findFirst()
                .orElseThrow(() -> new BusinessException("Shop settings not found"));
        if ("cash".equalsIgnoreCase(type))    s.setCashBalance(amount);
        else                                   s.setAccountBalance(amount);
        settingsRepo.save(s);
        return getBalances();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Map<String, Object> toMap(Supplier s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          s.getId());
        m.put("name",        s.getName());
        m.put("mobile",      s.getMobile());
        m.put("email",       s.getEmail());
        m.put("address",     s.getAddress());
        m.put("products",    s.getProducts());
        m.put("creditLimit", s.getCreditLimit());
        m.put("currentDue",  s.getCurrentDue());
        m.put("active",      s.isActive());
        return m;
    }

    private String str(Map<String, Object> b, String k) {
        Object v = b.get(k);
        return v != null ? v.toString().trim() : null;
    }

    private BigDecimal decimal(Map<String, Object> b, String k, BigDecimal def) {
        Object v = b.get(k);
        if (v == null) return def;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return def; }
    }
}