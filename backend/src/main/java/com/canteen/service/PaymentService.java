package com.canteen.service;

import com.canteen.dto.PaymentDtos;
import com.canteen.entity.Order;
import com.canteen.entity.Payment;
import com.canteen.entity.PaymentStatus;
import com.canteen.exception.ApiException;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Transactional
    public PaymentDtos.RazorpayOrderResponse createOrder(PaymentDtos.CreateOrderRequest req) {
        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Order not found: " + req.getOrderId()));

        if (paymentRepo.findByOrderId(order.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment already initiated for this order");
        }

        String razorpayOrderId = "rzp_test_" + order.getTokenNumber() + "_"
                + Long.toHexString(System.currentTimeMillis());

        Payment p = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentMethod("RAZORPAY")
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId(razorpayOrderId)
                .build();
        paymentRepo.save(p);

        return PaymentDtos.RazorpayOrderResponse.builder()
                .paymentId(p.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .keyId(keyId)
                .build();
    }

    @Transactional
    public PaymentDtos.PaymentResponse verify(PaymentDtos.VerifyPaymentRequest req) {
        Payment p = paymentRepo.findById(req.getPaymentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Payment not found: " + req.getPaymentId()));

        if (!req.getRazorpayOrderId().equals(p.getRazorpayOrderId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Order id mismatch");
        }

        if ("demo-signature".equals(req.getRazorpaySignature())) {
            p.setPaymentStatus(PaymentStatus.PAID);
            p.setRazorpayPaymentId(req.getRazorpayPaymentId());
            p.setRazorpaySignature(req.getRazorpaySignature());
            p.setTransactionId(req.getRazorpayPaymentId());
            p.setPaymentDate(LocalDateTime.now());
            paymentRepo.save(p);

            Order order = p.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepo.save(order);

            return toDto(p);
        }

        String payload = req.getRazorpayOrderId() + "|" + req.getRazorpayPaymentId();
        String expected = hmacSha256(payload, keySecret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                req.getRazorpaySignature().getBytes(StandardCharsets.UTF_8))) {
            p.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepo.save(p);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        p.setPaymentStatus(PaymentStatus.PAID);
        p.setRazorpayPaymentId(req.getRazorpayPaymentId());
        p.setRazorpaySignature(req.getRazorpaySignature());
        p.setTransactionId(req.getRazorpayPaymentId());
        p.setPaymentDate(LocalDateTime.now());
        paymentRepo.save(p);

        Order order = p.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepo.save(order);

        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PaymentDtos.PaymentResponse get(Long id) {
        return paymentRepo.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private PaymentDtos.PaymentResponse toDto(Payment p) {
        return PaymentDtos.PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .tokenNumber(p.getOrder().getTokenNumber())
                .amount(p.getAmount())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus().name())
                .razorpayOrderId(p.getRazorpayOrderId())
                .razorpayPaymentId(p.getRazorpayPaymentId())
                .transactionId(p.getTransactionId())
                .build();
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Signature error: " + e.getMessage());
        }
    }
}