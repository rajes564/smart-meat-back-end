package com.smartmeat.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartmeat.dto.request.ExpenseRequest;
import com.smartmeat.dto.response.ExpenseResponse;
import com.smartmeat.service.ExpenseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//── Expenses Controller ───────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExpenseController {
	
	
 private final ExpenseService expenseService;

 @GetMapping
 public ResponseEntity<List<ExpenseResponse>> getAll(
         @RequestParam(required = false) LocalDate from,
         @RequestParam(required = false) LocalDate to) {
     return ResponseEntity.ok(expenseService.getAll(from, to));
 }

 @PostMapping
 public ResponseEntity<ExpenseResponse> add(@Valid @RequestBody ExpenseRequest req) {
     return ResponseEntity.ok(expenseService.add(req));
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<Void> delete(@PathVariable Long id) {
     expenseService.delete(id);
     return ResponseEntity.noContent().build();
 }
 
 
 
}