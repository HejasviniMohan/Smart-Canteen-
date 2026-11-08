package com.canteen.service;

import com.canteen.dto.OrderDtos;
import com.canteen.dto.PageResponse;
import com.canteen.entity.*;
import com.canteen.exception.ApiException;
import com.canteen.pickup.PickupCodeService;
import com.canteen.realtime.OrderChangedEvent;
import com.canteen.repository.*;
import com.canteen.slots.SlotService;
import com.canteen.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepo;
    private final FoodItemRepository foodRepo;
    private final UserRepository userRepo;
    private final TokenGenerator tokenGen;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final SlotService slotService;
    private final PickupCodeService pickupCodes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.payment.expiry-minutes}")
    private int paymentExpiryMinutes;

    @Transactional
    public OrderDtos.OrderResponse placeOrder(String email, OrderDtos.PlaceOrderRequest req, String idempotencyKey) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (idempotencyKey != null) {
            Optional<Order> previous = orderRepo.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
            if (previous.isPresent()) {
                return toDto(previous.get(), true);
            }
        }

        boolean online = req.getPaymentMethod() == PaymentMethod.ONLINE;
        LocalDateTime slot = req.getPickupSlot();

        Order order = Order.builder()
                .user(user)
                .status(online ? OrderStatus.PENDING_PAYMENT : OrderStatus.PLACED)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(req.getPaymentMethod())
                .pickupSlot(slot)
                .businessDate(slot.toLocalDate())
                .pickupNonce(newNonce())
                .idempotencyKey(idempotencyKey)
                .notes(req.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        Map<Long, FoodItem> foods = foodRepo.findAllById(
                        req.getItems().stream().map(OrderDtos.OrderItemRequest::getFoodItemId).toList())
                .stream().collect(Collectors.toMap(FoodItem::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDtos.OrderItemRequest ir : req.getItems()) {
            FoodItem food = foods.get(ir.getFoodItemId());
            if (food == null || Boolean.TRUE.equals(food.getDeleted())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "An item in your cart is no longer on the menu");
            }
            if (Boolean.FALSE.equals(food.getAvailability())) {
                throw new ApiException(HttpStatus.CONFLICT, "Food unavailable: " + food.getName());
            }
            BigDecimal price = food.effectivePrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(ir.getQuantity()));
            total = total.add(subtotal);
            order.addItem(OrderItem.builder()
                    .order(order).foodItem(food)
                    .quantity(ir.getQuantity()).price(price).subtotal(subtotal).build());
        }
        if (req.getExpectedTotal() != null && req.getExpectedTotal().compareTo(total) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Prices have changed. Please review your cart.");
        }
        order.setTotalAmount(total);

        // Locks are always taken in this order (slot, then token counter), so these
        // short row locks cannot deadlock against another checkout.
        slotService.book(slot);
        order.setTokenNumber(tokenGen.nextToken(order.getBusinessDate()));

        Order saved = orderRepo.save(order);
        if (!online) {
            notificationService.create(user, "Order placed",
                    "Your order " + saved.getTokenNumber() + " has been placed. Pay in cash at pickup.",
                    "ORDER_PLACED");
        }
        events.publishEvent(OrderChangedEvent.of(saved));
        return toDto(saved, true);
    }

    /** Used after two identical requests raced and the database rejected the second. */
    @Transactional(readOnly = true)
    public Optional<OrderDtos.OrderResponse> findByIdempotencyKey(String email, String idempotencyKey) {
        return userRepo.findByEmail(email)
                .flatMap(u -> orderRepo.findByUserIdAndIdempotencyKey(u.getId(), idempotencyKey))
                .map(o -> toDto(o, true));
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> myOrders(String email) {
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return orderRepo.findTop50ByUserIdOrderByOrderDateDesc(u.getId()).stream()
                .map(o -> toDto(o, true)).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getById(String email, boolean isStaff, Long id) {
        Order o = getOrderOrThrow(id);
        boolean owner = o.getUser().getEmail().equalsIgnoreCase(email);
        if (!owner && !isStaff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
        return toDto(o, owner);
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> activeOrders() {
        return orderRepo.findByStatusInOrderByPickupSlotAscIdAsc(OrderStatus.ACTIVE).stream()
                .map(o -> toDto(o, false)).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.OrderResponse> allOrders(OrderStatus status, Pageable pageable) {
        return PageResponse.of(status == null
                ? orderRepo.findAllBy(pageable)
                : orderRepo.findByStatus(status, pageable), o -> toDto(o, false));
    }

    /**
     * Kitchen progress. PLACED can't be set by hand (only a payment or a new cash order
     * does that), and COMPLETED goes through collect() so its payment check always applies.
     */
    @Transactional
    public OrderDtos.OrderResponse updateStatus(Long id, OrderStatus next, String staffEmail) {
        if (next == OrderStatus.COMPLETED) {
            return collect(id, staffEmail);
        }
        Order o = getOrderOrThrow(id);
        if (next == OrderStatus.CANCELLED) {
            cancelInternal(o, "The canteen cancelled your order " + o.getTokenNumber() + ".");
            return toDto(o, false);
        }
        if (next == OrderStatus.PLACED || !o.getStatus().canTransitionTo(next)) {
            throw new ApiException(HttpStatus.CONFLICT, "Illegal transition: " + o.getStatus() + " -> " + next);
        }
        o.setStatus(next);
        notificationService.create(o.getUser(), statusTitle(next),
                "Your order " + o.getTokenNumber() + ": " + statusTitle(next).toLowerCase() + ".",
                "ORDER_STATUS");
        events.publishEvent(OrderChangedEvent.of(o));
        return toDto(o, false);
    }

    @Transactional
    public void cancelOrder(String email, Long id) {
        Order o = getOrderOrThrow(id);
        if (!o.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
        if (!o.getStatus().cancellableByStudent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Order can no longer be cancelled: it is " + o.getStatus());
        }
        cancelInternal(o, "Your order " + o.getTokenNumber() + " was cancelled.");
    }

    /** Runs every minute from a scheduled job. Frees the slot held by an abandoned checkout. */
    @Transactional
    public int expireUnpaidOrders() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(paymentExpiryMinutes);
        List<Order> expired = orderRepo.findByStatusAndOrderDateBefore(OrderStatus.PENDING_PAYMENT, cutoff);
        expired.forEach(o -> cancelInternal(o,
                "Order " + o.getTokenNumber() + " was cancelled because payment was not completed in time."));
        return expired.size();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse lookupForPickup(OrderDtos.PickupLookupRequest req) {
        if (req.getCode() != null && !req.getCode().isBlank()) {
            OptionalLong id = pickupCodes.parse(req.getCode());
            Order o = id.isPresent() ? orderRepo.findById(id.getAsLong()).orElse(null) : null;
            if (o == null || !pickupCodes.codeFor(o).equals(req.getCode().trim())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "This QR code is not valid");
            }
            return toDto(o, false);
        }
        if (req.getToken() == null || req.getToken().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Scan a QR code or enter a token");
        }
        String token = req.getToken().trim().toUpperCase();
        return orderRepo.findByBusinessDateAndTokenNumber(LocalDate.now(clock), token)
                .map(o -> toDto(o, false))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No order today with token " + token));
    }

    @Transactional
    public OrderDtos.OrderResponse cashReceived(Long id) {
        Order o = getOrderOrThrow(id);
        paymentService.recordCash(o);
        notificationService.create(o.getUser(), "Payment received",
                "Cash received for order " + o.getTokenNumber() + ".", "PAYMENT_STATUS");
        events.publishEvent(OrderChangedEvent.of(o));
        return toDto(o, false);
    }

    /** The single atomic update means that of two simultaneous scans, exactly one succeeds. */
    @Transactional
    public OrderDtos.OrderResponse collect(Long id, String staffEmail) {
        int updated = orderRepo.markCollected(id, staffEmail, LocalDateTime.now(clock));
        Order o = getOrderOrThrow(id);
        if (updated == 0) {
            throw new ApiException(HttpStatus.CONFLICT, whyNotCollectable(o));
        }
        notificationService.create(o.getUser(), "Order collected",
                "Enjoy your meal! Order " + o.getTokenNumber() + " has been collected.", "ORDER_STATUS");
        events.publishEvent(OrderChangedEvent.of(o));
        return toDto(o, false);
    }

    private String whyNotCollectable(Order o) {
        if (o.getStatus() == OrderStatus.COMPLETED) {
            return "This order was already collected";
        }
        if (o.getStatus() == OrderStatus.READY) {
            return "Payment is still pending for this order";
        }
        return "This order is not ready for pickup. It is " + o.getStatus();
    }

    private void cancelInternal(Order o, String message) {
        if (!o.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new ApiException(HttpStatus.CONFLICT, "Order cannot be cancelled: it is " + o.getStatus());
        }
        // The refund runs first: if the provider refuses, the transaction rolls back
        // and the order stays as it was, rather than cancelled without a refund.
        paymentService.refundIfPaid(o);
        o.setStatus(OrderStatus.CANCELLED);
        slotService.release(o.getPickupSlot());
        notificationService.create(o.getUser(), "Order cancelled",
                o.getPaymentStatus() == PaymentStatus.REFUNDED ? message + " Your payment is being refunded." : message,
                "ORDER_CANCELLED");
        events.publishEvent(OrderChangedEvent.of(o));
    }

    private static String statusTitle(OrderStatus s) {
        return switch (s) {
            case ACCEPTED -> "Order accepted";
            case PREPARING -> "Being prepared";
            case READY -> "Ready for pickup";
            default -> "Order updated";
        };
    }

    /** 64 random bits. It doesn't need to be secret, since the signature is what can't be forged. */
    private static String newNonce() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private OrderDtos.OrderResponse toDto(Order o, boolean forOwner) {
        boolean showCode = forOwner && OrderStatus.ACTIVE.contains(o.getStatus());
        return OrderDtos.OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUser().getId())
                .userName(o.getUser().getName())
                .userEmail(o.getUser().getEmail())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .paymentMethod(o.getPaymentMethod())
                .tokenNumber(o.getTokenNumber())
                .pickupSlot(o.getPickupSlot())
                .pickupCode(showCode ? pickupCodes.codeFor(o) : null)
                .notes(o.getNotes())
                .orderDate(o.getOrderDate())
                .updatedAt(o.getUpdatedAt())
                .collectedAt(o.getCollectedAt())
                .paymentDeadline(o.getStatus() == OrderStatus.PENDING_PAYMENT
                        ? o.getOrderDate().plusMinutes(paymentExpiryMinutes) : null)
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
