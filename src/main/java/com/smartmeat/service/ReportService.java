package com.smartmeat.service;

import com.smartmeat.dto.response.CashSheetResponse;
import com.smartmeat.dto.response.DashboardResponse;
import com.smartmeat.dto.response.OrderResponse;
import com.smartmeat.dto.response.ProductResponse;
import com.smartmeat.entity.Order;
import com.smartmeat.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final OrderRepository         orderRepo;
    private final StockPurchaseRepository purchaseRepo;
    private final ExpenseRepository       expenseRepo;
    private final ProductRepository       productRepo;
    private final KhataAccountRepository  khataRepo;
    private final ShopSettingsRepository  settingsRepo;
    private final OrderService            orderService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    public DashboardResponse dashboard() {

        ZoneId zone = ZoneId.systemDefault(); // or ZoneId.of("Asia/Kolkata") for IST
        LocalDate today = LocalDate.now(zone);

        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay   = today.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal todaySales = orderRepo.todayTotalSales(startOfDay, endOfDay);
        long       todayCount = orderRepo.todayTransactionCount(startOfDay, endOfDay);
        
        
        BigDecimal monthSales   = orderRepo.monthTotalSales();
        BigDecimal purchaseCost = purchaseRepo.monthTotalCost();
        BigDecimal expenses     = expenseRepo.monthTotalExpenses();
        BigDecimal netProfit    = monthSales.subtract(purchaseCost).subtract(expenses);
        BigDecimal khataDue     = khataRepo.totalOutstanding();
        long       pending      = orderRepo.countPendingOrders();

        List<Map<String, Object>> weekly = orderRepo.weeklySalesData().stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day",   row[0].toString());
                    m.put("total", row[1]);
                    return m;
                }).collect(Collectors.toList());

        List<ProductResponse> lowStock = productRepo.findLowStockProducts().stream()
                .map(p -> ProductResponse.builder()
                        .id(p.getId()).name(p.getName())
                        .stockQty(p.getStockQty()).minStockLevel(p.getMinStockLevel())
                        .stockStatus(p.computedStockStatus())
                        .categoryName(p.getCategory() != null ? p.getCategory().getName() : "")
                        .build())
                .collect(Collectors.toList());

        List<OrderResponse> recent = orderRepo
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream().map(orderService::toResponse).collect(Collectors.toList());

        return DashboardResponse.builder()
                .todaySales(todaySales).todayTransactions((int) todayCount)
                .monthSales(monthSales).netProfit(netProfit)
                .totalKhataDue(khataDue).pendingOrders((int) pending)
                .weeklyChart(weekly).lowStockProducts(lowStock).recentOrders(recent)
                .build();
    }

    // ── Cash Sheet / Day Book ─────────────────────────────────────────────────
    public CashSheetResponse cashSheet(LocalDate from, LocalDate to,
                                        String type, String category) {

        // Opening balance from ShopSettings (updated on every sale/purchase)
        var        s           = settingsRepo.findFirst().orElse(null);
        BigDecimal openCash    = s != null && s.getCashBalance()    != null ? s.getCashBalance()    : BigDecimal.ZERO;
        BigDecimal openAccount = s != null && s.getAccountBalance() != null ? s.getAccountBalance() : BigDecimal.ZERO;
        BigDecimal opening     = openCash.add(openAccount);

        List<CashSheetResponse.CashSheetEntry> entries = new ArrayList<>();
        boolean showCredit = type == null || "credit".equalsIgnoreCase(type);
        boolean showDebit  = type == null || "debit".equalsIgnoreCase(type);

        // ── CREDITS: Sales ────────────────────────────────────────────────────
        if (showCredit) {
            orderRepo.findByDateRange(from, to).stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getCreatedAt().toString().substring(0, 10),
                            TreeMap::new, Collectors.toList()))
                    .forEach((date, orders) -> {
                        BigDecimal dayTotal = orders.stream().map(Order::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal dayCash = orders.stream().map(o -> {
                            String m = o.getPaymentMethod();
                            if ("SPLIT".equals(m))  return o.getCashPaid() != null ? o.getCashPaid() : BigDecimal.ZERO;
                            if ("CASH".equals(m))   return o.getTotal();
                            if ("KHATA".equals(m))  return o.getCashPaid() != null ? o.getCashPaid() : BigDecimal.ZERO;
                            return BigDecimal.ZERO; // UPI / CARD
                        }).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal dayAcct = dayTotal.subtract(dayCash);

                        List<CashSheetResponse.CashSheetEntry.SubEntry> subs = orders.stream()
                                .map(o -> CashSheetResponse.CashSheetEntry.SubEntry.builder()
                                        .time(o.getCreatedAt().toString().substring(11, 16))
                                        .label(o.getOrderNumber())
                                        .detail(o.getCustomerName() + " — " + o.getItems().stream()
                                                .map(i -> i.getProductName() + " " + i.getQty() + "kg")
                                                .collect(Collectors.joining(", ")))
                                        .amount(o.getTotal())
                                        .cashAmount(o.getCashPaid()  != null ? o.getCashPaid()  : BigDecimal.ZERO)
                                        .accountAmount(o.getUpiPaid() != null ? o.getUpiPaid()  : BigDecimal.ZERO)
                                        .paymentMethod(payLabel(o))
                                        .build())
                                .collect(Collectors.toList());

                        entries.add(CashSheetResponse.CashSheetEntry.builder()
                                .date(date).type("CREDIT").category("SALES")
                                .description("Sales — " + orders.size() + " transaction" + (orders.size() != 1 ? "s" : ""))
                                .amount(dayTotal).cashAmount(dayCash).accountAmount(dayAcct)
                                .subEntries(subs).build());
                    });
        }

        // ── DEBITS: Expenses ──────────────────────────────────────────────────
        if (showDebit) {
            expenseRepo.findByDateRange(from, to).forEach(e -> {
                BigDecimal cashAmt = e.getCashAmount()    != null ? e.getCashAmount()    : BigDecimal.ZERO;
                BigDecimal acctAmt = e.getAccountAmount() != null ? e.getAccountAmount() : BigDecimal.ZERO;
                if (cashAmt.add(acctAmt).compareTo(BigDecimal.ZERO) == 0) {
                    // Fallback: infer from paymentMode
                    String mode = e.getPaymentMode() != null ? e.getPaymentMode() : "CASH";
                    cashAmt = "CASH".equals(mode) ? e.getAmount() : BigDecimal.ZERO;
                    acctAmt = "CASH".equals(mode) ? BigDecimal.ZERO : e.getAmount();
                }
                entries.add(CashSheetResponse.CashSheetEntry.builder()
                        .date(e.getExpenseDate().toString()).type("DEBIT").category("EXPENSE")
                        .description(e.getCategory().replace("_", " ")
                                + (e.getDescription() != null ? " — " + e.getDescription() : ""))
                        .amount(e.getAmount()).cashAmount(cashAmt).accountAmount(acctAmt)
                        .staff(e.getEnteredBy() != null ? e.getEnteredBy().getName() : "")
                        .subEntries(Collections.emptyList()).build());
            });

            // ── DEBITS: Stock Purchases ───────────────────────────────────────
            purchaseRepo.findByDateRange(from, to).forEach(p -> {
                BigDecimal cashPd  = p.getCashPaid()    != null ? p.getCashPaid()    : BigDecimal.ZERO;
                BigDecimal acctPd  = p.getAccountPaid() != null ? p.getAccountPaid() : BigDecimal.ZERO;
                BigDecimal paid    = cashPd.add(acctPd);
                // If no split recorded, use totalCost as the debit amount
                BigDecimal debitAmt = paid.compareTo(BigDecimal.ZERO) > 0 ? paid : p.getTotalCost();
                entries.add(CashSheetResponse.CashSheetEntry.builder()
                        .date(p.getPurchaseDate().toString()).type("DEBIT").category("STOCK_PURCHASE")
                        .description("Stock — "
                                + (p.getSupplierName() != null ? p.getSupplierName() : "Unknown")
                                + (p.getProduct() != null ? " · " + p.getProduct().getName() : ""))
                        .amount(debitAmt).cashAmount(cashPd).accountAmount(acctPd)
                        .staff(p.getEnteredBy() != null ? p.getEnteredBy().getName() : "")
                        .subEntries(Collections.emptyList()).build());
            });
        }

        // Sort by date, credits before debits on same day, then compute running balance
        entries.sort(Comparator.comparing(CashSheetResponse.CashSheetEntry::getDate)
                .thenComparing(e -> "CREDIT".equals(e.getType()) ? 0 : 1));

        BigDecimal run = opening;
        for (CashSheetResponse.CashSheetEntry e : entries) {
            run = "CREDIT".equals(e.getType()) ? run.add(e.getAmount()) : run.subtract(e.getAmount());
            e.setRunningBalance(run);
        }

        BigDecimal totalCredit = entries.stream().filter(e -> "CREDIT".equals(e.getType()))
                .map(CashSheetResponse.CashSheetEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebit  = entries.stream().filter(e -> "DEBIT".equals(e.getType()))
                .map(CashSheetResponse.CashSheetEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return CashSheetResponse.builder()
                .openingBalance(opening).openingCash(openCash).openingAccount(openAccount)
                .totalCredit(totalCredit).totalDebit(totalDebit)
                .closingBalance(opening.add(totalCredit).subtract(totalDebit))
                .entries(entries).build();
    }

    // ── Sales summary (used by Reports page) ─────────────────────────────────
    public Map<String, Object> salesSummary(String period) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        ZoneId zone = ZoneId.systemDefault(); // or ZoneId.of("Asia/Kolkata") for IST
        LocalDate today = LocalDate.now(zone);

        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay   = today.plusDays(1).atStartOfDay(zone).toInstant();

           

        result.put("todaySales",
        	    orderRepo.todayTotalSales(startOfDay, endOfDay)
        	);
        
        result.put("monthSales",  orderRepo.monthTotalSales());
        result.put("weeklyChart", orderRepo.weeklySalesData().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day",   row[0]);
            m.put("total", row[1]);
            return m;
        }).collect(Collectors.toList()));
        return result;
    }
    
    

    // ── Helper: human-readable payment label for a sale ───────────────────────
    private String payLabel(Order o) {
        String m = o.getPaymentMethod();
        if (m == null) return "CASH";
        return switch (m.toUpperCase()) {
            case "SPLIT" -> {
                BigDecimal c = o.getCashPaid()  != null ? o.getCashPaid()  : BigDecimal.ZERO;
                BigDecimal u = o.getUpiPaid()   != null ? o.getUpiPaid()   : BigDecimal.ZERO;
                yield "Cash ₹" + c.toPlainString() + " + UPI ₹" + u.toPlainString();
            }
            case "KHATA" -> {
                BigDecimal c = o.getCashPaid()  != null ? o.getCashPaid()  : BigDecimal.ZERO;
                BigDecimal u = o.getUpiPaid()   != null ? o.getUpiPaid()   : BigDecimal.ZERO;
                BigDecimal paid = c.add(u);
                yield paid.compareTo(BigDecimal.ZERO) > 0
                        ? "Khata (Paid ₹" + paid.toPlainString() + ")"
                        : "Full Khata";
            }
            default -> m;
        };
    }
}