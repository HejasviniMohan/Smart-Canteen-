package com.canteen.service;

import com.canteen.dto.PageResponse;
import com.canteen.dto.PaymentDtos;
import com.canteen.entity.*;
import com.canteen.exception.ApiException;
import com.canteen.payment.PaymentGateway;
import com.canteen.payment.RazorpayGateway;
import com.canteen.realtime.OrderChangedEvent;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.PaymentEventRepository;
import com.canteen.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final PaymentEventRepository eventRepo;
    private final OrderRepository orderRepo;
    private final PaymentGateway gateway;
    private final NotificationService notifications;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    /** Safe to call again after a failed attempt: the same payment record is reused. */
    @Transactional
    public PaymentDtos.CheckoutResponse createOrder(String email, Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        requireOwner(order, email);
        if (order.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw new ApiException(HttpStatus.CONFLICT, "This order is paid in cash at pickup");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ApiException(HttpStatus.CONFLICT, "This order is no longer awaiting payment");
        }

        Payment payment = paymentRepo.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            String gatewayOrderId = gateway.createOrder(order.getBusinessDate() + "-" + order.getTokenNumber(),
                    order.getTotalAmount());
            payment = paymentRepo.save(Payment.builder()
                    .order(order)
                    .amount(order.getTotalAmount())
                    .gateway(gateway.name())
                    .paymentMethod(gateway.name().toUpperCase())
                    .paymentStatus(PaymentStatus.PENDING)
                    .razorpayOrderId(gatewayOrderId)
                    .build());
        } else {
            payment.setPaymentStatus(PaymentStatus.PENDING);
        }

        return PaymentDtos.CheckoutResponse.builder()
                .paymentId(payment.getId())
                .gateway(gateway.name())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .amountInPaise(RazorpayGateway.toPaise(payment.getAmount()))
                .currency("INR")
                .keyId(gateway.publicKey())
                .build();
    }

    /** Called by the browser after checkout. The signature proves it came from the provider. */
    @Transactional
    public PaymentDtos.PaymentResponse verify(String email, PaymentDtos.VerifyPaymentRequest req) {
        Payment p = paymentRepo.findByIdForUpdate(req.getPaymentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwner(p.getOrder(), email);

        if (!req.getRazorpayOrderId().equals(p.getRazorpayOrderId())
                || !gateway.isValidCheckoutSignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(),
                        req.getRazorpaySignature())) {
            // Leaves the payment untouched: a bad request must not block a later genuine payment.
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payment could not be verified");
        }
        settle(p, req.getRazorpayPaymentId(), req.getRazorpaySignature(), null);
        return toDto(p);
    }

    /**
     * Server-to-server confirmation from the provider. It marks the order paid even if the
     * student's browser closed before it could call verify().
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature, String eventId) {
        if (!gateway.isValidWebhookSignature(rawBody, signature)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }
        JsonNode root;
        try {
            root = json.readTree(rawBody);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid webhook body");
        }
        String type = root.path("event").asText();
        String id = eventId != null && !eventId.isBlank()
                ? eventId
                : com.canteen.payment.HmacSigner.signHex(rawBody, "payment-event-id");
        if (eventRepo.existsById(id)) {
            log.debug("Ignoring duplicate webhook {}", id);
            return;
        }
        eventRepo.save(PaymentEvent.builder().eventId(id).eventType(type)
                .receivedAt(LocalDateTime.now(clock)).build());

        JsonNode entity = root.path("payload").path("payment").path("entity");
        String gatewayOrderId = entity.path("order_id").asText(null);
        String gatewayPaymentId = entity.path("id").asText(null);
        if (gatewayOrderId == null || gatewayPaymentId == null) {
            return;
        }
        Payment p = paymentRepo.findByGatewayOrderIdForUpdate(gatewayOrderId).orElse(null);
        if (p == null) {
            log.warn("Webhook {} for unknown gateway order {}", type, gatewayOrderId);
            return;
        }

        switch (type) {
            case "payment.captured", "order.paid" -> {
                if (entity.path("amount").asLong() != RazorpayGateway.toPaise(p.getAmount())) {
                    log.error("Webhook amount mismatch for payment {}: not settling", p.getId());
                    return;
                }
                settle(p, gatewayPaymentId, null, entity.path("method").asText(null));
            }
            case "payment.failed" -> {
                if (p.getPaymentStatus() == PaymentStatus.PENDING) {
                    p.setPaymentStatus(PaymentStatus.FAILED);
                }
            }
            default -> log.debug("Ignoring webhook type {}", type);
        }
    }

    /** Idempotent: the browser callback and the webhook both land here, in either order. */
    private void settle(Payment p, String gatewayPaymentId, String signature, String method) {
        if (method != null) {
            p.setPaymentMethod(method.toUpperCase());
        }
        if (p.getPaymentStatus() == PaymentStatus.PAID || p.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        p.setPaymentStatus(PaymentStatus.PAID);
        p.setRazorpayPaymentId(gatewayPaymentId);
        p.setRazorpaySignature(signature);
        p.setTransactionId(gatewayPaymentId);
        p.setPaymentDate(now);

        Order order = p.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);

        // Money arrived after the order had already expired or been cancelled: send it back.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            refund(order, p);
            notifications.create(order.getUser(), "Payment refunded",
                    "Order " + order.getTokenNumber() + " had expired, so your payment is being refunded.",
                    "PAYMENT_STATUS");
            events.publishEvent(OrderChangedEvent.of(order));
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PLACED);
            notifications.create(order.getUser(), "Order confirmed",
                    "Payment received. Your order " + order.getTokenNumber() + " is with the kitchen.",
                    "ORDER_PLACED");
        }
        events.publishEvent(OrderChangedEvent.of(order));
    }

    @Transactional
    public void markFailed(String email, Long paymentId) {
        Payment p = paymentRepo.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwner(p.getOrder(), email);
        if (p.getPaymentStatus() == PaymentStatus.PENDING) {
            p.setPaymentStatus(PaymentStatus.FAILED);
        }
    }

    /** Refunds an online order that was paid. Called as part of cancelling it. */
    @Transactional
    public void refundIfPaid(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) return;
        Payment p = paymentRepo.findByOrderId(order.getId()).orElse(null);
        if (p != null) refund(order, p);
    }

    private void refund(Order order, Payment p) {
        gateway.refund(p.getRazorpayPaymentId(), p.getAmount());
        p.setPaymentStatus(PaymentStatus.REFUNDED);
        p.setRefundedAt(LocalDateTime.now(clock));
        order.setPaymentStatus(PaymentStatus.REFUNDED);
    }

    /** Staff took cash at the counter, so there is a payment record just like online orders. */
    @Transactional
    public void recordCash(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.CASH_ON_PICKUP) {
            throw new ApiException(HttpStatus.CONFLICT, "This order is not a cash order");
        }
        if (order.getStatus() != OrderStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "Cash is taken when the order is ready for pickup");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return;
        }
        paymentRepo.save(Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .gateway("cash")
                .paymentMethod("CASH")
                .paymentStatus(PaymentStatus.PAID)
                .paymentDate(LocalDateTime.now(clock))
                .build());
        order.setPaymentStatus(PaymentStatus.PAID);
    }

    @Transactional(readOnly = true)
    public String gatewayOrderIdForOwner(String email, Long paymentId) {
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        requireOwner(p.getOrder(), email);
        return p.getRazorpayOrderId();
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse get(Long id, String email, boolean isStaff) {
        Payment p = paymentRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!isStaff) requireOwner(p.getOrder(), email);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentDtos.PaymentResponse> list(PaymentStatus status, Pageable pageable) {
        return PageResponse.of(status == null
                ? paymentRepo.findAllBy(pageable)
                : paymentRepo.findByPaymentStatus(status, pageable), this::toDto);
    }

    private void requireOwner(Order order, String email) {
        if (!order.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
    }

    private PaymentDtos.PaymentResponse toDto(Payment p) {
        return PaymentDtos.PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .tokenNumber(p.getOrder().getTokenNumber())
                .amount(p.getAmount())
                .gateway(p.getGateway())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus().name())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .transactionId(p.getTransactionId())
                .paymentDate(p.getPaymentDate())
                .refundedAt(p.getRefundedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
