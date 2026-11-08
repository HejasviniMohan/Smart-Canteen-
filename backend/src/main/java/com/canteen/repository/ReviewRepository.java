package com.canteen.repository;

import com.canteen.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user", "foodItem"})
    List<Review> findByFoodItemIdOrderByCreatedAtDesc(Long foodItemId);

    @EntityGraph(attributePaths = {"user", "foodItem"})
    List<Review> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "foodItem"})
    Page<Review> findAllBy(Pageable pageable);

    boolean existsByUserIdAndFoodItemIdAndOrderId(Long userId, Long foodItemId, Long orderId);

    /** Rows of [foodItemId, average, count], loaded once rather than per menu item. */
    @Query("SELECT r.foodItem.id, AVG(r.rating), COUNT(r) FROM Review r GROUP BY r.foodItem.id")
    List<Object[]> ratingSummaries();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.foodItem.id = :foodId")
    Double averageRatingForFood(Long foodId);

    long countByFoodItemId(Long foodId);
}
