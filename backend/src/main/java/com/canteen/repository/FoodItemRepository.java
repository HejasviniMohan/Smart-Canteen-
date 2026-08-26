package com.canteen.repository;

import com.canteen.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByCategoryId(Long categoryId);
    List<FoodItem> findByAvailabilityTrue();
    List<FoodItem> findByAvailability(Boolean availability);

    @Query("SELECT f FROM FoodItem f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<FoodItem> searchByName(String q);
}