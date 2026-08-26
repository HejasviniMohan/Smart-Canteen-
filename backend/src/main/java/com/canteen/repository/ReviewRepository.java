package com.canteen.repository;

import com.canteen.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFoodItemId(Long foodItemId);
    List<Review> findByUserId(Long userId);
    boolean existsByUserIdAndFoodItemIdAndOrderId(Long userId, Long foodItemId, Long orderId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.foodItem.id = :foodId")
    Double averageRatingForFood(Long foodId);
}