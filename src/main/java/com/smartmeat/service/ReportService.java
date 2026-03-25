package com.smartmeat.service;

import com.smartmeat.dto.request.*;
import com.smartmeat.dto.response.*;
import com.smartmeat.entity.*;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;




// ═══════════════════════════════════════════════════════════════════════════════
// REPORT SERVICE
// ═══════════════════════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final OrderRepository orderRepo;
    private final StockPurchaseRepository purchaseRepo;
    private final ExpenseRepository expenseRepo;
    private final ProductRepository productRepo;
    private final KhataAccountRepository khataRepo;
    private final OrderService orderService;

    public DashboardResponse dashboard() {
    	
    	Instant start = LocalDate.now()
    	        .atStartOfDay(ZoneId.systemDefault())
    	        .toInstant();

    	Instant end = LocalDate.now()
    	        .plusDays(1)
    	        .atStartOfDay(ZoneId.systemDefault())
    	        .toInstant();
    	
    	
        BigDecimal todaySales    = orderRepo.todayTotalSales(start,end);
        long todayCount          = orderRepo.todayTransactionCount(start,end);
        BigDecimal monthSales    = orderRepo.monthTotalSales();
        BigDecimal purchaseCost  = purchaseRepo.monthTotalCost();
        BigDecimal expenses      = expenseRepo.monthTotalExpenses();
        BigDecimal netProfit     = monthSales.subtract(purchaseCost).subtract(expenses);
        BigDecimal khataDue      = khataRepo.totalOutstanding();
        long pendingOrders       = orderRepo.countPendingOrders();

        // Weekly chart
        List<Map<String, Object>> weekly = orderRepo.weeklySalesData().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("day", row[0].toString());
            m.put("total", row[1]);
            return m;
        }).collect(Collectors.toList());

        // Low stock
        List<ProductResponse> lowStock = productRepo.findLowStockProducts().stream()
                .map(p -> ProductResponse.builder()
                        .id(p.getId()).name(p.getName())
                        .stockQty(p.getStockQty())
                        .minStockLevel(p.getMinStockLevel())
                        .stockStatus(p.computedStockStatus())
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : "")
                        .build())
                .collect(Collectors.toList());

        // Recent orders (last 10)
        List<OrderResponse> recent = orderRepo.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 10))
                .stream().map(orderService::toResponse).collect(Collectors.toList());

        return DashboardResponse.builder()
                .todaySales(todaySales)
                .todayTransactions((int) todayCount)
                .monthSales(monthSales)
                .netProfit(netProfit)
                .totalKhataDue(khataDue)
                .pendingOrders((int) pendingOrders)
                .weeklyChart(weekly)
                .lowStockProducts(lowStock)
                .recentOrders(recent)
                .build();
    }

    public CashSheetResponse cashSheet(LocalDate from, LocalDate to, String type, String category) {
        // Build a chronological list of all financial events in the date range
        List<CashSheetResponse.CashSheetEntry> entries = new ArrayList<>();
        BigDecimal runBal = BigDecimal.valueOf(12500); // Opening balance (configurable)

        // Credits: orders/sales
        if (type == null || "credit".equalsIgnoreCase(type)) {
            // Group orders by day
            orderRepo.findByDate(from, org.springframework.data.domain.Pageable.unpaged())
                    .getContent().stream()
                    .filter(o -> !o.getStatus().equals("CANCELLED"))
                    .collect(Collectors.groupingBy(o -> o.getCreatedAt().toString().substring(0, 10)))
                    .forEach((date, orders) -> {
                        BigDecimal dayTotal = orders.stream()
                                .map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                        runBal.add(dayTotal);

                        List<CashSheetResponse.CashSheetEntry.SubEntry> subs = orders.stream()
                                .map(o -> CashSheetResponse.CashSheetEntry.SubEntry.builder()
                                        .time(o.getCreatedAt().toString().substring(11, 16))
                                        .label(o.getCustomerName())
                                        .detail(o.getItems().stream()
                                                .map(i -> i.getProductName() + " " + i.getQty() + "kg")
                                                .collect(Collectors.joining(", ")))
                                        .amount(o.getTotal())
                                        .paymentMethod(o.getPaymentMethod())
                                        .build())
                                .collect(Collectors.toList());

                        entries.add(CashSheetResponse.CashSheetEntry.builder()
                                .date(date)
                                .type("CREDIT")
                                .category("SALES")
                                .description("Sales — " + orders.size() + " transactions")
                                .amount(dayTotal)
                                .runningBalance(runBal)
                                .subEntries(subs)
                                .build());
                    });
        }

        // Debits: expenses + stock purchases
        if (type == null || "debit".equalsIgnoreCase(type)) {
            expenseRepo.findByDateRange(from, to).forEach(e -> {
                runBal.subtract(e.getAmount());
                entries.add(CashSheetResponse.CashSheetEntry.builder()
                        .date(e.getExpenseDate().toString())
                        .type("DEBIT")
                        .category("EXPENSE")
                        .description(e.getCategory() + (e.getDescription() != null ? " — " + e.getDescription() : ""))
                        .amount(e.getAmount())
                        .staff(e.getEnteredBy() != null ? e.getEnteredBy().getName() : "")
                        .runningBalance(runBal)
                        .subEntries(List.of())
                        .build());
            });

            purchaseRepo.findByDateRange(from, to).forEach(p -> {
                runBal.subtract(p.getTotalCost());
                entries.add(CashSheetResponse.CashSheetEntry.builder()
                        .date(p.getPurchaseDate().toString())
                        .type("DEBIT")
                        .category("STOCK_PURCHASE")
                        .description("Stock Purchase — " + p.getSupplierName())
                        .amount(p.getTotalCost())
                        .staff(p.getEnteredBy() != null ? p.getEnteredBy().getName() : "")
                        .runningBalance(runBal)
                        .subEntries(List.of())
                        .build());
            });
        }

        // Sort by date
        entries.sort(Comparator.comparing(CashSheetResponse.CashSheetEntry::getDate));

        BigDecimal totalCredit = entries.stream().filter(e -> "CREDIT".equals(e.getType()))
                .map(CashSheetResponse.CashSheetEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebit = entries.stream().filter(e -> "DEBIT".equals(e.getType()))
                .map(CashSheetResponse.CashSheetEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal opening = BigDecimal.valueOf(12500);

        return CashSheetResponse.builder()
                .openingBalance(opening)
                .totalCredit(totalCredit)
                .totalDebit(totalDebit)
                .closingBalance(opening.add(totalCredit).subtract(totalDebit))
                .entries(entries)
                .build();
    }

    public Map<String, Object> salesSummary(String period) {
        Map<String, Object> result = new HashMap<>();
      	Instant start = LocalDate.now()
    	        .atStartOfDay(ZoneId.systemDefault())
    	        .toInstant();

    	Instant end = LocalDate.now()
    	        .plusDays(1)
    	        .atStartOfDay(ZoneId.systemDefault())
    	        .toInstant();
    	
        
        result.put("todaySales", orderRepo.todayTotalSales(start,end));
        result.put("monthSales", orderRepo.monthTotalSales());
        result.put("weeklyChart", orderRepo.weeklySalesData().stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("day", row[0]); m.put("total", row[1]);
            return m;
        }).collect(Collectors.toList()));
        return result;
    }
}
