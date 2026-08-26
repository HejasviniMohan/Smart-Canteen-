package com.canteen.service;

import com.canteen.dto.OrderDtos;
import com.canteen.entity.*;
import com.canteen.exception.ApiException;
import com.canteen.repository.*;
import com.canteen.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final FoodItemRepository foodRepo;
    private final UserRepository userRepo;
    private final TokenGenerator tokenGen;
    private final NotificationService notificationService;

    private BigDecimal effectivePrice(FoodItem food) {
        if (food.getSpecialPrice() != null && food.getSpecialPrice().compareTo(BigDecimal.ZERO) > 0) {
            return food.getSpecialPrice();
        }
        return food.getPrice();
    }

    @Transactional
    public OrderDtos.OrderResponse placeOrder(String email, OrderDtos.PlaceOrderRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isCashOnPickup = "CASH_ON_PICKUP".equalsIgnoreCase(req.getPaymentMethod());

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PLACED)
                .paymentStatus(isCashOnPickup ? PaymentStatus.PENDING : PaymentStatus.PENDING)
                .tokenNumber(tokenGen.nextToken())
                .notes(req.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDtos.OrderItemRequest ir : req.getItems()) {
            FoodItem food = foodRepo.findById(ir.getFoodItemId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Food item not found: " + ir.getFoodItemId()));
            if (Boolean.FALSE.equals(food.getAvailability())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Food unavailable: " + food.getName());
            }
            BigDecimal price = effectivePrice(food);
            BigDecimal sub = price.multiply(BigDecimal.valueOf(ir.getQuantity()));
            total = total.add(sub);
            OrderItem oi = OrderItem.builder()
                    .order(order).foodItem(food)
                    .quantity(ir.getQuantity()).price(price).subtotal(sub).build();
            order.addItem(oi);
        }
        order.setTotalAmount(total);
        Order saved = orderRepo.save(order);
        notificationService.create(user, "Order Placed",
                "Your order " + saved.getTokenNumber() + " has been placed.",
                "ORDER_PLACED");
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> myOrders(String email) {
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return orderRepo.findByUserIdOrderByOrderDateDesc(u.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getById(String email, Long id) {
        Order o = getOrderOrThrow(id);
        if (!o.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
        return toDto(o);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getByToken(String token) {
        return orderRepo.findByTokenNumber(token)
                .map(this::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found with token: " + token));
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> allOrders(OrderStatus status) {
        List<Order> list = status == null
                ? orderRepo.findAll()
                : orderRepo.findByStatusOrderByOrderDateDesc(status);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public OrderDtos.OrderResponse updateStatus(Long id, OrderStatus next) {
        Order o = getOrderOrThrow(id);
        if (!o.getStatus().canTransitionTo(next)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Illegal transition: " + o.getStatus() + " -> " + next);
        }
        o.setStatus(next);
        Order saved = orderRepo.save(o);
        notificationService.create(o.getUser(), "Order " + next,
                "Your order " + saved.getTokenNumber() + " is now " + next + ".",
                "ORDER_STATUS");
        return toDto(saved);
    }

    @Transactional
    public OrderDtos.OrderResponse updatePaymentStatus(Long id, PaymentStatus status) {
        Order o = getOrderOrThrow(id);
        o.setPaymentStatus(status);
        Order saved = orderRepo.save(o);
        notificationService.create(o.getUser(), "Payment " + status,
                "Payment for order " + saved.getTokenNumber() + " is now " + status + ".",
                "PAYMENT_STATUS");
        return toDto(saved);
    }

    @Transactional
    public void cancelOrder(String email, Long id) {
        Order o = getOrderOrThrow(id);
        if (!o.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
        if (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Order cannot be cancelled in status " + o.getStatus());
        }
        o.setStatus(OrderStatus.CANCELLED);
        orderRepo.save(o);
        notificationService.create(o.getUser(), "Order Cancelled",
                "Your order " + o.getTokenNumber() + " was cancelled.",
                "ORDER_CANCELLED");
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private OrderDtos.OrderResponse toDto(Order o) {
        return OrderDtos.OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .userName(o.getUser().getName())
                .userEmail(o.getUser().getEmail())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .tokenNumber(o.getTokenNumber())
                .notes(o.getNotes())
                .orderDate(o.getOrderDate())
                .updatedAt(o.getUpdatedAt())
                .items(o.getItems().stream().map(i -> OrderDtos.OrderItemResponse.builder()
                        .id(i.getId())
                        .foodItemId(i.getFoodItem().getId())
                        .foodItemName(i.getFoodItem().getName())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .subtotal(i.getSubtotal())
                        .build()).toList())
                .build();
    }
}