package com.canteen.repository;

import com.canteen.entity.Payment;
import com.canteen.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    /*
     * The browser callback and the webhook usually arrive together for the same payment.
     * The row lock makes them take turns, so the second one sees PAID and does nothing.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :gatewayOrderId")
    Optional<Payment> findByGatewayOrderIdForUpdate(String gatewayOrderId);

    @EntityGraph(attributePaths = "order")
    Page<Payment> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "order")
    Page<Payment> findByPaymentStatus(PaymentStatus status, Pageable pageable);
}
