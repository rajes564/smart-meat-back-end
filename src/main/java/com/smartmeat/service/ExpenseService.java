package com.smartmeat.service;

import com.smartmeat.repository.ExpenseRepository;
import com.smartmeat.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmeat.dto.request.ExpenseRequest;
import com.smartmeat.dto.response.ExpenseResponse;
import com.smartmeat.entity.Expense;

//═══════════════════════════════════════════════════════════════════════════════
//EXPENSE SERVICE
//═══════════════════════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

 private final ExpenseRepository expenseRepo;
 private final SecurityUtils securityUtils;

 public List<ExpenseResponse> getAll(LocalDate from, LocalDate to) {
     List<Expense> list = (from != null && to != null)
             ? expenseRepo.findByDateRange(from, to)
             : expenseRepo.findAllByOrderByExpenseDateDescCreatedAtDesc();
     return list.stream().map(this::toResponse).collect(Collectors.toList());
 }

 @Transactional
 public ExpenseResponse add(ExpenseRequest req) {
     Expense expense = Expense.builder()
             .category(req.getCategory().toUpperCase())
             .amount(req.getAmount())
             .description(req.getDescription())
             .expenseDate(req.getExpenseDate() != null
                     ? LocalDate.parse(req.getExpenseDate()) : LocalDate.now())
             .enteredBy(securityUtils.currentUser())
             .build();
     return toResponse(expenseRepo.save(expense));
 }

 @Transactional
 public void delete(Long id) {
     expenseRepo.deleteById(id);
 }

 private ExpenseResponse toResponse(Expense e) {
     return ExpenseResponse.builder()
             .id(e.getId())
             .category(e.getCategory())
             .amount(e.getAmount())
             .description(e.getDescription())
             .expenseDate(e.getExpenseDate() != null ? e.getExpenseDate().toString() : "")
             .enteredByName(e.getEnteredBy() != null ? e.getEnteredBy().getName() : "")
             .build();
 }
}