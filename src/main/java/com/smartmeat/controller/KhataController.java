package com.smartmeat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartmeat.dto.request.KhataAccountRequest;
import com.smartmeat.dto.request.KhataEntryRequest;
import com.smartmeat.dto.response.KhataAccountResponse;
import com.smartmeat.dto.response.KhataEntryResponse;
import com.smartmeat.dto.response.KhataLedgerResponse;
import com.smartmeat.service.KhataService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//── Khata Controller ─────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/khata")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KhataController {
	
 private final KhataService khataService;

 @GetMapping
 public ResponseEntity<List<KhataAccountResponse>> getAll() {
     return ResponseEntity.ok(khataService.getAll());
 }

 @GetMapping("/{id}/ledger")
 public ResponseEntity<KhataLedgerResponse> getLedger(@PathVariable Long id) {
     return ResponseEntity.ok(khataService.getLedger(id));
 }

 @PostMapping("/accounts")
 public ResponseEntity<KhataAccountResponse> createAccount(@Valid @RequestBody KhataAccountRequest req) {
     return ResponseEntity.ok(khataService.createAccount(req));
 }

 @PostMapping("/entries")
 public ResponseEntity<KhataEntryResponse> addEntry(@Valid @RequestBody KhataEntryRequest req) {
     return ResponseEntity.ok(khataService.addEntry(req));
 }

 @GetMapping("/summary")
 public ResponseEntity<Map<String, Object>> summary() {
     return ResponseEntity.ok(khataService.summary());
 }
 



 /**
  * POST /api/khata/accounts/direct
  * Creates a Khata account directly from the admin panel.
  * Accepts name + mobile + email + creditLimit — no pre-existing user required.
  * If a user with that mobile already exists, links to them.
  * If not, creates a new CUSTOMER user first, then creates the Khata account.
  */
 @PostMapping("/accounts/direct")
 public ResponseEntity<KhataAccountResponse> createDirect(@RequestBody Map<String, Object> body) {
	 System.out.println("Controller is hitted.......");
     return ResponseEntity.ok(khataService.createAccountDirect(body));
 }


}