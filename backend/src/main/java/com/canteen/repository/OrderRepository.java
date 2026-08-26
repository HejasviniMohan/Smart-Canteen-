package com.canteen.repository;

import com.canteen.entity.Order;
import com.canteen.entity.OrderStatus;
import com.canteen.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);
    List<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime from, LocalDateTime to);
    Optional<Order> findByTokenNumber(String tokenNumber);
    Optional<Order> findTopByOrderByIdDesc();
    long countByOrderDateBetween(LocalDateTime from, LocalDateTime to);
    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate BETWEEN :from AND :to AND o.paymentStatus = :payStatus")
    BigDecimal sumRevenueBetween(LocalDateTime from, LocalDateTime to, PaymentStatus payStatus);

    @Query("SELECT oi.foodItem.id, oi.foodItem.name, SUM(oi.quantity) AS totalQty, SUM(oi.subtotal) AS totalRev " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.orderDate BETWEEN :from AND :to AND o.paymentStatus = com.canteen.entity.PaymentStatus.PAID " +
           "GROUP BY oi.foodItem.id, oi.foodItem.name " +
           "ORDER BY totalQty DESC")
    List<Object[]> findTopSellingFoods(LocalDateTime from, LocalDateTime to);

    @Query("SELECT FUNCTION('HOUR', o.orderDate) AS hr, COUNT(o) AS cnt " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :from AND :to " +
           "GROUP BY FUNCTION('HOUR', o.orderDate) " +
           "ORDER BY hr")
    List<Object[]> findHourlyOrderCounts(LocalDateTime from, LocalDateTime to);

    @Query("SELECT oi.foodItem.id, AVG(r.rating) AS avgRating, COUNT(r) AS cnt " +
           "FROM Review r JOIN r.foodItem fi JOIN OrderItem oi ON oi.foodItem.id = fi.id AND oi.order.id = r.order.id " +
           "WHERE fi.id = :foodId GROUP BY oi.foodItem.id")
    Object[] findAverageRatingForFood(Long foodId);
}