package com.canteen.repository;

import com.canteen.entity.Order;
import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "user")
    List<Order> findTop50ByUserIdOrderByOrderDateDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Order> findByStatusInOrderByPickupSlotAscIdAsc(Collection<OrderStatus> statuses);

    @EntityGraph(attributePaths = "user")
    Page<Order> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Optional<Order> findByBusinessDateAndTokenNumber(LocalDate businessDate, String tokenNumber);

    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<Order> findByStatusAndOrderDateBefore(OrderStatus status, LocalDateTime cutoff);

    long countByOrderDateBetween(LocalDateTime from, LocalDateTime to);

    long countByStatus(OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    /**
     * Collects an order in one atomic statement. The WHERE clause is the guard: of two
     * simultaneous scans, only one can match, so an order can never be handed over twice.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
               SET o.status = com.canteen.entity.OrderStatus.COMPLETED,
                   o.collectedAt = :now, o.collectedBy = :staffEmail,
                   o.updatedAt = :now, o.version = o.version + 1
             WHERE o.id = :id
               AND o.status = com.canteen.entity.OrderStatus.READY
               AND o.paymentStatus = com.canteen.entity.PaymentStatus.PAID
            """)
    int markCollected(Long id, String staffEmail, LocalDateTime now);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :from AND :to AND o.paymentStatus = :payStatus")
    BigDecimal sumRevenueBetween(LocalDateTime from, LocalDateTime to, PaymentStatus payStatus);

    @Query("""
            SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity) AS totalQty, SUM(oi.subtotal) AS totalRev
              FROM OrderItem oi JOIN oi.order o
             WHERE o.orderDate BETWEEN :from AND :to
               AND o.paymentStatus = com.canteen.entity.PaymentStatus.PAID
             GROUP BY oi.foodItem.id, oi.foodItem.name
             ORDER BY totalQty DESC
            """)
    List<Object[]> findTopSellingFoods(LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT EXTRACT(HOUR FROM o.orderDate) AS hr, COUNT(o) AS cnt
              FROM Order o
             WHERE o.orderDate BETWEEN :from AND :to
               AND o.status <> com.canteen.entity.OrderStatus.CANCELLED
             GROUP BY EXTRACT(HOUR FROM o.orderDate)
             ORDER BY hr
            """)
    List<Object[]> findHourlyOrderCounts(LocalDateTime from, LocalDateTime to);

    @Query(nativeQuery = true, value = """
            SELECT CAST(order_date AS DATE) AS day,
                   COALESCE(SUM(total_amount) FILTER (WHERE payment_status = 'PAID'), 0) AS revenue,
                   COUNT(*) AS orders
              FROM orders
             WHERE order_date >= :from AND status <> 'CANCELLED'
             GROUP BY CAST(order_date AS DATE)
             ORDER BY day
            """)
    List<Object[]> findDailyRevenue(LocalDateTime from);
}
