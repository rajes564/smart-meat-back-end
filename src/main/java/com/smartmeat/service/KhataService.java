package com.smartmeat.service;

import com.smartmeat.dto.request.KhataAccountRequest;
import com.smartmeat.dto.request.KhataEntryRequest;
import com.smartmeat.dto.response.KhataAccountResponse;
import com.smartmeat.dto.response.KhataEntryResponse;
import com.smartmeat.dto.response.KhataLedgerResponse;
import com.smartmeat.entity.KhataAccount;
import com.smartmeat.entity.KhataEntry;
import com.smartmeat.entity.User;
import com.smartmeat.enums.Role;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.KhataAccountRepository;
import com.smartmeat.repository.KhataEntryRepository;
import com.smartmeat.repository.UserRepository;
import com.smartmeat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KhataService {

    private final KhataAccountRepository accountRepo;
    private final KhataEntryRepository entryRepo;
    private final UserRepository userRepo;
    private final SecurityUtils securityUtils;
    private final ShopService shopService;

    public List<KhataAccountResponse> getAll() {
        return accountRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }

    public KhataLedgerResponse getLedger(Long accountId) {
        KhataAccount account = accountRepo.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khata account not found"));
        List<KhataEntry> entries = entryRepo.findByAccountIdOrderByCreatedAtDesc(accountId);

        BigDecimal running = BigDecimal.ZERO;
        List<KhataEntryResponse> entryResponses = new ArrayList<>();
        List<KhataEntry> reversed = new ArrayList<>(entries);
        Collections.reverse(reversed);

        for (KhataEntry e : reversed) {
            if ("DEBIT".equals(e.getEntryType())) running = running.add(e.getAmount());
            else running = running.subtract(e.getAmount());

            entryResponses.add(0, KhataEntryResponse.builder()
                    .id(e.getId())
                    .entryType(e.getEntryType())
                    .amount(e.getAmount())
                    .description(e.getDescription())
                    .referenceNote(e.getReferenceNote())
                    .entryDate(e.getEntryDate() != null ? e.getEntryDate().toString() : "")
                    .enteredByName(e.getEnteredBy() != null ? e.getEnteredBy().getName() : "")
                    .runningBalance(running)
                    .build());
        }

        return KhataLedgerResponse.builder()
                .account(toAccountResponse(account))
                .entries(entryResponses)
                .runningBalance(running)
                .build();
    }

    @Transactional
    public KhataAccountResponse createAccount(KhataAccountRequest req) {
        User customer = userRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (accountRepo.findByCustomerId(customer.getId()).isPresent()) {
            throw new BusinessException("Khata account already exists for this customer");
        }

        KhataAccount account = KhataAccount.builder()
                .customer(customer)
                .creditLimit(req.getCreditLimit() != null ? req.getCreditLimit() : BigDecimal.valueOf(10000))
                .currentDue(BigDecimal.ZERO)
                .active(true)
                .createdBy(securityUtils.currentUser())
                .build();

        return toAccountResponse(accountRepo.save(account));
    }

    /**
     * Create a Khata account directly from the admin panel.
     * If a user with the given mobile already exists → reuse them.
     * If not → create a new CUSTOMER user with a random password, then create the account.
     */
    @Transactional
    public KhataAccountResponse createAccountDirect(Map<String, Object> body) {
        String name        = (String) body.getOrDefault("customerName", "");
        String mobile      = (String) body.getOrDefault("customerMobile", "");
        String email       = (String) body.get("email");
        Object limitObj    = body.get("creditLimit");
        BigDecimal limit   = limitObj != null
                ? new BigDecimal(limitObj.toString())
                : BigDecimal.valueOf(10000);

        if (name == null || name.isBlank())
            throw new BusinessException("Customer name is required");
        if (mobile == null || mobile.length() < 10)
            throw new BusinessException("Valid 10-digit mobile is required");

        // Find or create user
        User customer = userRepo.findByMobile(mobile).orElseGet(() -> {
            User u = User.builder()
                    .name(name)
                    .mobile(mobile)
                    .email(email)
                    .password("$2a$12$dummyHashForKhataAccountCreation123456789")
                    .role(Role.CUSTOMER)
                    .active(true)
                    .build();
            return userRepo.save(u);
        });

        // Update name if it was auto-created or blank
        if (customer.getName() == null || customer.getName().isBlank()) {
            customer.setName(name);
            userRepo.save(customer);
        }

        if (accountRepo.findByCustomerId(customer.getId()).isPresent()) {
            throw new BusinessException("Khata account already exists for " + mobile);
        }

        KhataAccount account = KhataAccount.builder()
                .customer(customer)
                .creditLimit(limit)
                .currentDue(BigDecimal.ZERO)
                .totalCredit(BigDecimal.ZERO)
                .active(true)
                .createdBy(securityUtils.currentUser())
                .build();

        return toAccountResponse(accountRepo.save(account));
    }

    @Transactional
    public KhataEntryResponse addEntry(KhataEntryRequest req) {
        KhataAccount account = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Khata account not found"));

        // Resolve cash/account split
        BigDecimal cashAmt = req.getCashAmount() != null ? req.getCashAmount() : BigDecimal.ZERO;
        BigDecimal acctAmt = req.getAccountAmount() != null ? req.getAccountAmount() : BigDecimal.ZERO;
        String mode = req.getPaymentMode() != null ? req.getPaymentMode().toUpperCase() : "CASH";

        // If no explicit split provided, infer from paymentMode
        if (cashAmt.add(acctAmt).compareTo(BigDecimal.ZERO) == 0) {
            switch (mode) {
                case "CASH"  -> cashAmt = req.getAmount();
                case "UPI", "CARD" -> acctAmt = req.getAmount();
                default -> cashAmt = req.getAmount(); // fallback
            }
        }

        KhataEntry entry = KhataEntry.builder()
                .account(account)
                .entryType(req.getEntryType().toUpperCase())
                .amount(req.getAmount())
                .description(req.getDescription())
                .referenceNote(req.getReferenceNote())
                .enteredBy(securityUtils.currentUser())
                .entryDate(LocalDate.now())
                .build();
        entryRepo.save(entry);

        if ("DEBIT".equals(entry.getEntryType())) {
            // Customer owes more — no cash movement yet
            account.setCurrentDue(account.getCurrentDue().add(req.getAmount()));
        } else {
            // CREDIT: customer paying back — cash/account received
            account.setCurrentDue(account.getCurrentDue().subtract(req.getAmount()).max(BigDecimal.ZERO));
            account.setTotalCredit(account.getTotalCredit().add(req.getAmount()));
            // Credit to shop balances
            shopService.addToBalance(cashAmt, acctAmt);
        }
        accountRepo.save(account);

        return KhataEntryResponse.builder()
                .id(entry.getId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .description(entry.getDescription())
                .build();
    }

    public Map<String, Object> summary() {
        BigDecimal total = accountRepo.totalOutstanding();
        List<KhataAccountResponse> accounts = getAll();
        Map<String, Object> result = new HashMap<>();
        result.put("totalOutstanding", total);
        result.put("accountCount", accounts.size());
        result.put("accounts", accounts);
        return result;
    }

    private KhataAccountResponse toAccountResponse(KhataAccount a) {
        double usage = a.getCreditLimit() != null && a.getCreditLimit().compareTo(BigDecimal.ZERO) > 0
                ? a.getCurrentDue().divide(a.getCreditLimit(), 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0;
        return KhataAccountResponse.builder()
                .id(a.getId())
                .customerId(a.getCustomer() != null ? a.getCustomer().getId() : null)
                .customerName(a.getCustomer() != null ? a.getCustomer().getName() : "")
                .customerMobile(a.getCustomer() != null ? a.getCustomer().getMobile() : "")
                .creditLimit(a.getCreditLimit())
                .currentDue(a.getCurrentDue())
                .totalCredit(a.getTotalCredit())
                .active(a.isActive())
                .usagePercent(Math.min(usage, 100.0))
                .build();
    }
}