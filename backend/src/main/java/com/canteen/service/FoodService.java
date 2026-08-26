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
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodItemRepository foodRepo;
    private final CategoryRepository categoryRepo;
    private final ReviewRepository reviewRepo;

    @Transactional(readOnly = true)
    public List<FoodDtos.Response> findAll(Long categoryId, Boolean available, Boolean todaySpecial) {
        List<FoodItem> items;
        if (categoryId != null && available != null) {
            items = foodRepo.findByCategoryId(categoryId).stream()
                    .filter(f -> f.getAvailability().equals(available)).toList();
        } else if (categoryId != null) {
            items = foodRepo.findByCategoryId(categoryId);
        } else if (available != null) {
            items = foodRepo.findByAvailability(available);
        } else {
            items = foodRepo.findAll();
        }
        if (Boolean.TRUE.equals(todaySpecial)) {
            items = items.stream().filter(f -> Boolean.TRUE.equals(f.getIsTodaySpecial())).toList();
        }
        return items.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FoodDtos.Response findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public FoodDtos.Response create(FoodDtos.CreateRequest req) {
        Category cat = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Category not found: " + req.getCategoryId()));
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
        foodRepo.delete(getOrThrow(id));
    }

    private FoodItem getOrThrow(Long id) {
        return foodRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Food item not found: " + id));
    }

    private FoodDtos.Response toDto(FoodItem f) {
        Double avg = reviewRepo.averageRatingForFood(f.getId());
        Long count = (long) reviewRepo.findByFoodItemId(f.getId()).size();
        return FoodDtos.Response.builder()
                .id(f.getId())
                .categoryId(f.getCategory().getId())
                .categoryName(f.getCategory().getName())
                .name(f.getName())
                .description(f.getDescription())
                .price(f.getPrice() == null ? BigDecimal.ZERO : f.getPrice())
                .specialPrice(f.getSpecialPrice())
                .isTodaySpecial(Boolean.TRUE.equals(f.getIsTodaySpecial()))
                .availability(f.getAvailability())
                .imageUrl(f.getImageUrl())
                .prepTimeMin(f.getPrepTimeMin())
                .isVeg(f.getIsVeg())
                .averageRating(avg)
                .reviewCount(count)
                .build();
    }
}