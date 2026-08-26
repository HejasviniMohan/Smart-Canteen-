package com.canteen.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AdminDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardStats {
        private long todayOrders;
        private long pendingOrders;
        private BigDecimal todayRevenue;
        private BigDecimal totalRevenue;
        private long totalUsers;
        private long totalFoodItems;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopFood {
        private Long foodItemId;
        private String foodItemName;
        private Long totalQuantity;
        private BigDecimal totalRevenue;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RevenuePoint {
        private LocalDate date;
        private BigDecimal revenue;
        private long orders;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PeakHour {
        private Integer hour;
        private Long orderCount;
    }
}