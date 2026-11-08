package com.canteen.service;

import com.canteen.dto.PageResponse;
import com.canteen.dto.ReviewDtos;
import com.canteen.entity.*;
import com.canteen.exception.ApiException;
import com.canteen.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final OrderRepository orderRepo;
    private final FoodItemRepository foodRepo;
    private final UserRepository userRepo;

    @Transactional
    public ReviewDtos.Response create(String email, ReviewDtos.CreateRequest req) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your order");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "Can only review after order is completed");
        }
        FoodItem food = foodRepo.findById(req.getFoodItemId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Food not found"));

        boolean wasOrdered = order.getItems().stream()
                .anyMatch(i -> i.getFoodItem().getId().equals(food.getId()));
        if (!wasOrdered) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This item was not part of the order");
        }

        if (reviewRepo.existsByUserIdAndFoodItemIdAndOrderId(user.getId(), food.getId(), order.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Already reviewed this food in this order");
        }

        Review r = Review.builder()
                .user(user).foodItem(food).order(order)
                .rating(req.getRating()).comment(req.getComment()).build();
        reviewRepo.save(r);
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.Response> forFood(Long foodId) {
        return reviewRepo.findByFoodItemIdOrderByCreatedAtDesc(foodId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.Response> myReviews(String email) {
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
        return reviewRepo.findByUserId(u.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDtos.Response> all(Pageable pageable) {
        return PageResponse.of(reviewRepo.findAllBy(pageable), this::toDto);
    }

    @Transactional
    public void delete(Long id) {
        if (!reviewRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Review not found");
        }
        reviewRepo.deleteById(id);
    }

    private ReviewDtos.Response toDto(Review r) {
        return ReviewDtos.Response.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .foodItemId(r.getFoodItem().getId())
                .foodItemName(r.getFoodItem().getName())
                .orderId(r.getOrder().getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}