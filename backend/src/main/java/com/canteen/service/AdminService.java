package com.canteen.service;

import com.canteen.dto.AdminDtos;
import com.canteen.dto.AuthDtos;
import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentStatus;
import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.exception.ApiException;
import com.canteen.repository.FoodItemRepository;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final FoodItemRepository foodRepo;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AdminDtos.DashboardStats dashboard() {
        LocalDateTime startOfToday = LocalDate.now(clock).atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);
        return AdminDtos.DashboardStats.builder()
                .todayOrders(orderRepo.countByOrderDateBetween(startOfToday, endOfToday))
                .pendingOrders(orderRepo.countByStatusIn(OrderStatus.ACTIVE))
                .todayRevenue(nz(orderRepo.sumRevenueBetween(startOfToday, endOfToday, PaymentStatus.PAID)))
                .totalRevenue(nz(orderRepo.sumRevenueBetween(LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.of(2100, 1, 1, 0, 0), PaymentStatus.PAID)))
                .totalUsers(userRepo.count() - userRepo.countByRole(Role.ADMIN))
                .totalFoodItems(foodRepo.countByDeletedFalse())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.TopFood> topFoods(int days) {
        LocalDateTime from = LocalDate.now(clock).minusDays(days - 1L).atStartOfDay();
        LocalDateTime to = LocalDate.now(clock).plusDays(1).atStartOfDay();
        return orderRepo.findTopSellingFoods(from, to, PageRequest.of(0, 10)).stream().map(row ->
                AdminDtos.TopFood.builder()
                        .foodItemId((Long) row[0])
                        .foodItemName((String) row[1])
                        .totalQuantity(((Number) row[2]).longValue())
                        .totalRevenue((BigDecimal) row[3])
                        .build()).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PeakHour> peakHours(int days) {
        LocalDateTime from = LocalDate.now(clock).minusDays(days - 1L).atStartOfDay();
        LocalDateTime to = LocalDate.now(clock).plusDays(1).atStartOfDay();
        return orderRepo.findHourlyOrderCounts(from, to).stream().map(row ->
                AdminDtos.PeakHour.builder()
                        .hour(((Number) row[0]).intValue())
                        .orderCount(((Number) row[1]).longValue())
                        .build()).toList();
    }

    /** Daily revenue, including days with no orders so the chart has no misleading gaps. */
    @Transactional(readOnly = true)
    public List<AdminDtos.RevenuePoint> revenueTrend(int days) {
        LocalDate first = LocalDate.now(clock).minusDays(days - 1L);
        Map<LocalDate, Object[]> byDay = new HashMap<>();
        for (Object[] row : orderRepo.findDailyRevenue(first.atStartOfDay())) {
            byDay.put(LocalDate.parse(row[0].toString()), row);
        }
        List<AdminDtos.RevenuePoint> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = first.plusDays(i);
            Object[] row = byDay.get(day);
            points.add(AdminDtos.RevenuePoint.builder()
                    .date(day)
                    .revenue(row == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()))
                    .orders(row == null ? 0 : ((Number) row[2]).longValue())
                    .build());
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<AuthDtos.UserSummary> users() {
        return userRepo.findAll().stream().map(AuthService::toSummary).toList();
    }

    @Transactional
    public AuthDtos.UserSummary createUser(AuthDtos.CreateUserRequest req) {
        String email = AuthService.normalizeEmail(req.getEmail());
        if (userRepo.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = userRepo.save(User.builder()
                .name(req.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone() == null || req.getPhone().isBlank() ? null : req.getPhone().trim())
                .role(req.getRole())
                .enabled(true)
                .build());
        return AuthService.toSummary(user);
    }

    @Transactional
    public AuthDtos.UserSummary setEnabled(Long id, boolean enabled, String actingAdminEmail) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (!enabled && user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot disable your own account");
        }
        user.setEnabled(enabled);
        return AuthService.toSummary(user);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
