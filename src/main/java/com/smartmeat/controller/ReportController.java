package com.smartmeat.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmeat.dto.response.CashSheetResponse;
import com.smartmeat.dto.response.DashboardResponse;
import com.smartmeat.service.ReportService;

import lombok.RequiredArgsConstructor;

//── Reports / Cash Sheet Controller ──────────────────────────────────────────
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {
 private final ReportService reportService;

 @GetMapping("/cash-sheet")
 public ResponseEntity<CashSheetResponse> cashSheet(
         @RequestParam LocalDate from,
         @RequestParam LocalDate to,
         @RequestParam(required = false) String type,
         @RequestParam(required = false) String category) {
     return ResponseEntity.ok(reportService.cashSheet(from, to, type, category));
 }

 @GetMapping("/dashboard")
 public ResponseEntity<DashboardResponse> dashboard() {
     return ResponseEntity.ok(reportService.dashboard());
 }

 @GetMapping("/sales-summary")
 public ResponseEntity<Map<String, Object>> salesSummary(
         @RequestParam(defaultValue = "monthly") String period) {
     return ResponseEntity.ok(reportService.salesSummary(period));
 }
}