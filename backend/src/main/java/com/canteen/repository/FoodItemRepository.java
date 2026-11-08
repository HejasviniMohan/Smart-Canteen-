package com.canteen.repository;

import com.canteen.entity.FoodItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @Query("""
            SELECT f FROM FoodItem f JOIN FETCH f.category
             WHERE f.deleted = false
               AND (:categoryId IS NULL OR f.category.id = :categoryId)
               AND (:available IS NULL OR f.availability = :available)
               AND (:todaySpecial IS NULL OR :todaySpecial = false OR f.isTodaySpecial = true)
             ORDER BY f.id
            """)
    List<FoodItem> search(Long categoryId, Boolean available, Boolean todaySpecial);

    long countByDeletedFalse();
}
