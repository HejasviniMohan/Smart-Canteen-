package com.canteen.service;

import com.canteen.dto.AdminDtos;
import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentStatus;
import com.canteen.entity.Role;
import com.canteen.repository.FoodItemRepository;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final FoodItemRepository foodRepo;

    @Transactional(readOnly = true)
    public AdminDtos.DashboardStats dashboard() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        long todayOrders = orderRepo.countByOrderDateBetween(startOfToday, endOfToday);
        long pending = orderRepo.countByStatus(OrderStatus.PLACED)
                + orderRepo.countByStatus(OrderStatus.ACCEPTED)
                + orderRepo.countByStatus(OrderStatus.PREPARING)
                + orderRepo.countByStatus(OrderStatus.READY);
        BigDecimal todayRev = nz(orderRepo.sumRevenueBetween(startOfToday, endOfToday, PaymentStatus.PAID));
        BigDecimal totalRev = nz(orderRepo.sumRevenueBetween(LocalDateTime.of(2000, 1, 1, 0, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0), PaymentStatus.PAID));
        return AdminDtos.DashboardStats.builder()
                .todayOrders(todayOrders)
                .pendingOrders(pending)
                .todayRevenue(todayRev)
                .totalRevenue(totalRev)
                .totalUsers(userRepo.count() - userRepo.countByRole(Role.ADMIN))
                .totalFoodItems(foodRepo.count())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.TopFood> topFoods() {
        LocalDateTime from = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        return orderRepo.findTopSellingFoods(from, to).stream().map(row ->
                AdminDtos.TopFood.builder()
                        .foodItemId((Long) row[0])
                        .foodItemName((String) row[1])
                        .totalQuantity(((Number) row[2]).longValue())
                        .totalRevenue((BigDecimal) row[3])
                        .build()).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PeakHour> peakHours() {
        LocalDateTime from = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(1).atStartOfDay();
        return orderRepo.findHourlyOrderCounts(from, to).stream().map(row ->
                AdminDtos.PeakHour.builder()
                        .hour((Integer) row[0])
                        .orderCount(((Number) row[1]).longValue())
                        .build()).toList();
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}