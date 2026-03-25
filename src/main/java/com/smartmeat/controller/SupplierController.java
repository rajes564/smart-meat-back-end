package com.smartmeat.controller;


import com.smartmeat.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(supplierService.getAll());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(supplierService.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(supplierService.update(id, body));
    }

    @GetMapping("/{id}/ledger")
    public ResponseEntity<Map<String, Object>> getLedger(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getLedger(id));
    }

    @PostMapping("/ledger")
    public ResponseEntity<Map<String, Object>> addEntry(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(supplierService.addLedgerEntry(body));
    }

    @GetMapping("/balances")
    public ResponseEntity<Map<String, Object>> getBalances() {
        return ResponseEntity.ok(supplierService.getBalances());
    }

    @PostMapping("/balances/adjust")
    public ResponseEntity<Map<String, Object>> adjustBalance(
            @RequestParam String type,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(supplierService.adjustBalance(type, amount, note));
    }
}