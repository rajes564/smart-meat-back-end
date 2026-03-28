package com.smartmeat.service;

import com.smartmeat.dto.request.StockPurchaseRequest;
import com.smartmeat.dto.response.StockPurchaseResponse;
import com.smartmeat.entity.*;
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
public class InventoryService {

    private final StockPurchaseRepository purchaseRepo;
    private final ProductRepository       productRepo;
    private final SupplierRepository      supplierRepo;
    private final SupplierLedgerRepository ledgerRepo;
    private final SecurityUtils           securityUtils;
    private final SupplierService         supplierService;

    public List<StockPurchaseResponse> getAll(LocalDate from, LocalDate to) {
        List<StockPurchase> list;
        if (from != null && to != null) list = purchaseRepo.findByDateRange(from, to);
        else                            list = purchaseRepo.findAllByOrderByPurchaseDateDescCreatedAtDesc();
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public StockPurchaseResponse addPurchase(StockPurchaseRequest req) {
        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.getProductId()));

        BigDecimal totalCost  = req.getCostPerKg().multiply(req.getQty());
        BigDecimal amountPaid = req.getAmountPaid() != null ? req.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal cashPaid    = req.getCashPaid()    != null ? req.getCashPaid()    : BigDecimal.ZERO;
        BigDecimal accountPaid = req.getAccountPaid() != null ? req.getAccountPaid() : BigDecimal.ZERO;
        // If no split provided, assume all cash
        if (cashPaid.add(accountPaid).compareTo(BigDecimal.ZERO) == 0 && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            cashPaid = amountPaid;
        }

        // Resolve supplier
        Supplier supplier = null;
        String supplierName = req.getSupplierName();
        if (req.getSupplierId() != null) {
            supplier = supplierRepo.findById(req.getSupplierId()).orElse(null);
            if (supplier != null) supplierName = supplier.getName();
        }

        StockPurchase purchase = StockPurchase.builder()
                .product(product)
                .supplier(supplier)
                .supplierName(supplierName)
                .qty(req.getQty())
                .costPerKg(req.getCostPerKg())
                .totalCost(totalCost)
                .amountPaid(amountPaid)
                .cashPaid(cashPaid)
                .accountPaid(accountPaid)
                .paymentMode(req.getPaymentMode() != null ? req.getPaymentMode() : "CASH")
                .invoiceNo(req.getInvoiceNo())
                .enteredBy(securityUtils.currentUser())
                .build();

        purchaseRepo.save(purchase);

        // ── Increase product stock ────────────────────────────────────────────
        product.setStockQty(product.getStockQty().add(req.getQty()));
        product.setStockStatus(product.computedStockStatus());
        productRepo.save(product);

        BigDecimal amountDue = totalCost.subtract(amountPaid);

        // If linked supplier — create ledger DEBIT entry + update due
        if (supplier != null) {
            SupplierLedgerEntry debit = SupplierLedgerEntry.builder()
                    .supplier(supplier)
                    .purchase(purchase)
                    .entryType("DEBIT")
                    .amount(totalCost)
                    .cashAmount(cashPaid)
                    .accountAmount(accountPaid)
                    .description("Stock purchase: " + product.getName() + " " + req.getQty() + "kg")
                    .referenceNote("Invoice: " + (req.getInvoiceNo() != null ? req.getInvoiceNo() : "—"))
                    .enteredBy(securityUtils.currentUser())
                    .entryDate(LocalDate.now())
                    .build();
            ledgerRepo.save(debit);

            supplier.setCurrentDue(supplier.getCurrentDue().add(amountDue));
            supplierRepo.save(supplier);
        }

        // Update shop cash/account balances (deduct paid amounts)
        if (cashPaid.compareTo(BigDecimal.ZERO) > 0 || accountPaid.compareTo(BigDecimal.ZERO) > 0) {
            supplierService.updateShopBalance(cashPaid.negate(), accountPaid.negate());
        }

        return toResponse(purchase);
    }

    public List<Map<String, Object>> supplierBalances() {
        return purchaseRepo.supplierBalances().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("supplierName", row[0]);
            m.put("total",        row[1]);
            m.put("paid",         row[2]);
            m.put("due",          row[3]);
            return m;
        }).collect(Collectors.toList());
    }

    private StockPurchaseResponse toResponse(StockPurchase p) {
        BigDecimal due = p.getTotalCost().subtract(p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO);
        return StockPurchaseResponse.builder()
                .id(p.getId())
                .productId(p.getProduct() != null ? p.getProduct().getId() : null)
                .productName(p.getProduct() != null ? p.getProduct().getName() : "")
                .supplierName(p.getSupplierName())
                .qty(p.getQty())
                .costPerKg(p.getCostPerKg())
                .totalCost(p.getTotalCost())
                .amountPaid(p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO)
                .amountDue(due.max(BigDecimal.ZERO))
                .invoiceNo(p.getInvoiceNo())
                .purchaseDate(p.getPurchaseDate() != null ? p.getPurchaseDate().toString() : "")
                .enteredByName(p.getEnteredBy() != null ? p.getEnteredBy().getName() : "")
                .build();
    }
}