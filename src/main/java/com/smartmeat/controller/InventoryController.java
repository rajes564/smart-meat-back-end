package com.smartmeat.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmeat.dto.request.StockPurchaseRequest;
import com.smartmeat.dto.response.StockPurchaseResponse;
import com.smartmeat.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//── Inventory Controller ──────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {
 private final InventoryService inventoryService;

 @GetMapping
 public ResponseEntity<List<StockPurchaseResponse>> getAll(
         @RequestParam(required = false) LocalDate from,
         @RequestParam(required = false) LocalDate to) {
     return ResponseEntity.ok(inventoryService.getAll(from, to));
 }

 @PostMapping
 public ResponseEntity<StockPurchaseResponse> addPurchase(@Valid @RequestBody StockPurchaseRequest req) {
     return ResponseEntity.ok(inventoryService.addPurchase(req));
 }

 @GetMapping("/supplier-balances")
 public ResponseEntity<List<Map<String, Object>>> supplierBalances() {
     return ResponseEntity.ok(inventoryService.supplierBalances());
 }
 
 
}