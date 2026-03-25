package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

//── Dashboard ─────────────────────────────────────────────────────────────────
@Data @Builder public class DashboardResponse {
 BigDecimal todaySales;
 int todayTransactions;
 BigDecimal monthSales;
 BigDecimal netProfit;
 BigDecimal totalKhataDue;
 int pendingOrders;
 List<Map<String, Object>> weeklyChart;
 List<ProductResponse> lowStockProducts;
 List<OrderResponse> recentOrders;
}