package com.canteen.service;

import com.canteen.dto.FoodDtos;
import com.canteen.entity.Category;
import com.canteen.entity.FoodItem;
import com.canteen.exception.ApiException;
import com.canteen.repository.CategoryRepository;
import com.canteen.repository.FoodItemRepository;
import com.canteen.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodItemRepository foodRepo;
    private final CategoryRepository categoryRepo;
    private final ReviewRepository reviewRepo;

    @Transactional(readOnly = true)
    public List<FoodDtos.Response> findAll(Long categoryId, Boolean available, Boolean todaySpecial) {
        Map<Long, Object[]> ratings = new HashMap<>();
        for (Object[] row : reviewRepo.ratingSummaries()) {
            ratings.put((Long) row[0], row);
        }
        return foodRepo.search(categoryId, available, todaySpecial).stream().map(f -> {
            Object[] rating = ratings.get(f.getId());
            return toDto(f, rating == null ? null : ((Number) rating[1]).doubleValue(),
                    rating == null ? 0L : ((Number) rating[2]).longValue());
        }).toList();
    }

    @Transactional(readOnly = true)
    public FoodDtos.Response findById(Long id) {
        return toDto(getOrThrow(id));
    }

    private void validateSpecialPrice(BigDecimal price, BigDecimal specialPrice) {
        if (specialPrice != null && specialPrice.compareTo(price) >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Special price must be lower than the regular price");
        }
    }

    @Transactional
    public FoodDtos.Response create(FoodDtos.CreateRequest req) {
        Category cat = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Category not found: " + req.getCategoryId()));
        validateSpecialPrice(req.getPrice(), req.getSpecialPrice());
        FoodItem f = FoodItem.builder()
                .category(cat)
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .specialPrice(req.getSpecialPrice())
                .isTodaySpecial(Boolean.TRUE.equals(req.getIsTodaySpecial()))
                .imageUrl(req.getImageUrl())
                .prepTimeMin(req.getPrepTimeMin() == null ? 10 : req.getPrepTimeMin())
                .isVeg(req.getIsVeg() == null ? Boolean.TRUE : req.getIsVeg())
                .availability(true)
                .build();
        return toDto(foodRepo.save(f));
    }

    @Transactional
    public FoodDtos.Response update(Long id, FoodDtos.UpdateRequest req) {
        FoodItem f = getOrThrow(id);
        if (req.getCategoryId() != null) {
            Category cat = categoryRepo.findById(req.getCategoryId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Category not found: " + req.getCategoryId()));
            f.setCategory(cat);
        }
        if (req.getName() != null)        f.setName(req.getName());
        if (req.getDescription() != null) f.setDescription(req.getDescription());
        if (req.getPrice() != null)        f.setPrice(req.getPrice());
        if (req.getSpecialPrice() != null) f.setSpecialPrice(req.getSpecialPrice());
        if (Boolean.TRUE.equals(req.getClearSpecialPrice())) f.setSpecialPrice(null);
        validateSpecialPrice(f.getPrice(), f.getSpecialPrice());
        if (req.getIsTodaySpecial() != null) f.setIsTodaySpecial(req.getIsTodaySpecial());
        if (req.getImageUrl() != null)    f.setImageUrl(req.getImageUrl());
        if (req.getPrepTimeMin() != null) f.setPrepTimeMin(req.getPrepTimeMin());
        if (req.getIsVeg() != null)        f.setIsVeg(req.getIsVeg());
        if (req.getAvailability() != null) f.setAvailability(req.getAvailability());
        return toDto(foodRepo.save(f));
    }

    @Transactional
    public FoodDtos.Response toggleAvailability(Long id) {
        FoodItem f = getOrThrow(id);
        f.setAvailability(!f.getAvailability());
        return toDto(foodRepo.save(f));
    }

    @Transactional
    public void delete(Long id) {
        // Past orders still reference this item, so it is hidden rather than removed.
        FoodItem f = getOrThrow(id);
        f.setDeleted(true);
        f.setAvailability(false);
        f.setIsTodaySpecial(false);
    }

    private FoodItem getOrThrow(Long id) {
        return foodRepo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Food item not found: " + id));
    }

    private FoodDtos.Response toDto(FoodItem f) {
        return toDto(f, reviewRepo.averageRatingForFood(f.getId()), reviewRepo.countByFoodItemId(f.getId()));
    }

    private FoodDtos.Response toDto(FoodItem f, Double averageRating, long reviewCount) {
        return FoodDtos.Response.builder()
                .id(f.getId())
                .categoryId(f.getCategory().getId())
                .categoryName(f.getCategory().getName())
                .name(f.getName())
                .description(f.getDescription())
                .price(f.getPrice())
                .specialPrice(f.getSpecialPrice())
                .effectivePrice(f.effectivePrice())
                .isTodaySpecial(Boolean.TRUE.equals(f.getIsTodaySpecial()))
                .availability(f.getAvailability())
                .imageUrl(f.getImageUrl())
                .prepTimeMin(f.getPrepTimeMin())
                .isVeg(f.getIsVeg())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }
}
