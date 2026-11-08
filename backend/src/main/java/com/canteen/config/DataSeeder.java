package com.canteen.config;

import com.canteen.entity.*;
import com.canteen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Demo accounts have published passwords, so this runs only with app.seed.demo=true.
 * It adds past orders so the admin charts have something to show in a demo.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final FoodItemRepository foodRepo;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepo;
    private final PaymentRepository paymentRepo;
    private final ReviewRepository reviewRepo;
    private final Clock clock;

    // Photos are served by the frontend from public/images/foods, so they can't break the way
    // hotlinked images do. Sources and licence: frontend/public/images/CREDITS.md.
    private static String img(String slug) {
        return "/images/foods/" + slug + ".webp";
    }

    @Override
    public void run(String... args) {
        if (userRepo.existsByEmail("student@canteen.com")) {
            log.info("Data already seeded. Skipping.");
            return;
        }
        log.info("Seeding initial data...");

        User admin = User.builder()
                .name("Admin User").email("admin@canteen.com")
                .password(passwordEncoder.encode("Admin@123"))
                .phone("9999900000").role(Role.ADMIN).build();
        User staff = User.builder()
                .name("Staff User").email("staff@canteen.com")
                .password(passwordEncoder.encode("Staff@123"))
                .phone("9999900001").role(Role.STAFF).build();
        User student = User.builder()
                .name("Student User").email("student@canteen.com")
                .password(passwordEncoder.encode("Student@123"))
                .phone("9999900002").role(Role.STUDENT).build();
        userRepo.saveAll(List.of(admin, staff, student));

        Category southIndian = categoryRepo.save(Category.builder()
                .name("South Indian").description("Idli, Dosa, Vada and more").displayOrder(1).build());
        Category northIndian = categoryRepo.save(Category.builder()
                .name("North Indian").description("Curries, Roti, Paratha").displayOrder(2).build());
        Category chinese = categoryRepo.save(Category.builder()
                .name("Chinese").description("Fried rice, noodles, Manchurian").displayOrder(3).build());
        Category snacks = categoryRepo.save(Category.builder()
                .name("Snacks").description("Samosa, Sandwich, Pakora").displayOrder(4).build());
        Category beverages = categoryRepo.save(Category.builder()
                .name("Beverages").description("Tea, Coffee, Juice").displayOrder(5).build());
        Category desserts = categoryRepo.save(Category.builder()
                .name("Desserts").description("Sweet treats and ice cream").displayOrder(6).build());
        Category combos = categoryRepo.save(Category.builder()
                .name("Combos").description("Value meals and thalis").displayOrder(7).build());
        Category healthy = categoryRepo.save(Category.builder()
                .name("Healthy").description("Salads, oats, grilled items").displayOrder(8).build());

        seedFood(southIndian, "Masala Dosa", "Crispy dosa with potato masala & chutney", "80.00", "65.00", true, true,
                img("masala-dosa"));
        seedFood(southIndian, "Idli Sambar", "Soft steamed idlis with sambar & coconut chutney", "50.00", null, true, false,
                img("idli-sambar"));
        seedFood(southIndian, "Medu Vada", "Crispy golden vadas with chutney & sambar", "55.00", null, true, false,
                img("medu-vada"));
        seedFood(southIndian, "Rava Dosa", "Crispy semolina dosa with onions & spices", "70.00", null, true, false,
                img("rava-dosa"));
        seedFood(southIndian, "Uttapam", "Thick pancake topped with onions & tomatoes", "60.00", null, true, false,
                img("uttapam"));

        seedFood(northIndian, "Paneer Butter Masala", "Creamy tomato gravy with soft paneer cubes", "150.00", "120.00", true, true,
                img("paneer-butter-masala"));
        seedFood(northIndian, "Veg Biryani", "Aromatic basmati rice with mixed vegetables", "130.00", "105.00", true, true,
                img("veg-biryani"));
        seedFood(northIndian, "Aloo Paratha", "Stuffed potato flatbread with butter", "70.00", null, true, false,
                img("aloo-paratha"));
        seedFood(northIndian, "Dal Makhani", "Rich creamy black lentils slow-cooked overnight", "120.00", null, true, false,
                img("dal-makhani"));
        seedFood(northIndian, "Chole Bhature", "Spiced chickpeas with deep-fried bread", "90.00", null, true, false,
                img("chole-bhature"));
        seedFood(northIndian, "North Indian Thali", "Complete meal with roti, dal, sabzi, rice & dessert", "180.00", "150.00", true, false,
                img("north-indian-thali"));

        seedFood(chinese, "Veg Fried Rice", "Wok-tossed rice with vegetables & soy", "100.00", "80.00", true, true,
                img("veg-fried-rice"));
        seedFood(chinese, "Paneer Fried Rice", "Fried rice with chunky paneer pieces", "120.00", null, true, false,
                img("paneer-fried-rice"));
        seedFood(chinese, "Veg Manchurian", "Crispy vegetable balls in spicy Indo-Chinese sauce", "110.00", null, true, false,
                img("veg-manchurian"));
        seedFood(chinese, "Hakka Noodles", "Stir-fried noodles with vegetables", "100.00", null, true, false,
                img("hakka-noodles"));
        seedFood(chinese, "Schezwan Fried Rice", "Spicy fried rice with schezwan sauce", "110.00", null, true, false,
                img("schezwan-fried-rice"));

        seedFood(snacks, "Samosa", "Crispy triangular pastry with spiced potato filling", "20.00", "15.00", true, true,
                img("samosa"));
        seedFood(snacks, "Veg Sandwich", "Grilled sandwich with fresh vegetables & cheese", "60.00", null, true, false,
                img("veg-sandwich"));
        seedFood(snacks, "Pakora Plate", "Assorted vegetable fritters with mint chutney", "50.00", null, true, false,
                img("pakora-plate"));
        seedFood(snacks, "Aloo Tikki", "Golden potato patties with chutneys", "30.00", null, true, false,
                img("aloo-tikki"));
        seedFood(snacks, "Paneer Tikka", "Marinated paneer cubes grilled to perfection", "80.00", null, true, false,
                img("paneer-tikka"));

        seedFood(beverages, "Masala Chai", "Strong Indian tea brewed with spices & milk", "15.00", "10.00", true, true,
                img("masala-chai"));
        seedFood(beverages, "Filter Coffee", "Strong South Indian filter coffee", "25.00", "20.00", true, false,
                img("filter-coffee"));
        seedFood(beverages, "Fresh Lime Soda", "Refreshing lime with soda and chat masala", "35.00", null, true, false,
                img("fresh-lime-soda"));
        seedFood(beverages, "Cold Coffee", "Chilled creamy coffee with ice cream", "60.00", null, true, false,
                img("cold-coffee"));
        seedFood(beverages, "Mango Lassi", "Thick creamy mango yogurt smoothie", "50.00", null, true, false,
                img("mango-lassi"));

        seedFood(desserts, "Gulab Jamun", "Soft milk dumplings soaked in rose-flavored syrup", "40.00", "30.00", true, true,
                img("gulab-jamun"));
        seedFood(desserts, "Ice Cream", "Creamy ice cream - Vanilla, Chocolate or Strawberry", "50.00", null, true, false,
                img("ice-cream"));
        seedFood(desserts, "Brownie", "Warm chocolate brownie with nuts", "70.00", null, true, false,
                img("brownie"));
        seedFood(desserts, "Rasgulla", "Spongy white cheese balls in light sugar syrup", "35.00", null, true, false,
                img("rasgulla"));
        seedFood(desserts, "Cheesecake", "Creamy baked cheesecake with berry topping", "90.00", null, true, false,
                img("cheesecake"));

        seedFood(combos, "South Indian Combo", "Masala Dosa + Idli + Filter Coffee", "140.00", null, true, false,
                img("south-indian-combo"));
        seedFood(combos, "North Indian Combo", "Aloo Paratha + Dal Makhani + Chai", "160.00", null, true, false,
                img("north-indian-combo"));
        seedFood(combos, "Chinese Combo", "Fried Rice + Manchurian + Coke", "170.00", null, true, false,
                img("chinese-combo"));
        seedFood(combos, "Street Food Combo", "Samosa + Sandwich + Cold Coffee", "120.00", null, true, false,
                img("street-food-combo"));
        seedFood(combos, "Student Meal", "Veg Biryani + Gulab Jamun + Water", "160.00", null, true, false,
                img("student-meal"));

        seedFood(healthy, "Greek Salad", "Fresh cucumbers, tomatoes, feta & olives", "90.00", null, true, false,
                img("greek-salad"));
        seedFood(healthy, "Oats Upma", "Healthy oats cooked with vegetables", "70.00", null, true, false,
                img("oats-upma"));
        seedFood(healthy, "Grilled Paneer Salad", "Fresh salad with grilled paneer cubes", "110.00", null, true, false,
                img("grilled-paneer-salad"));
        seedFood(healthy, "Veg Soup", "Hot & healthy mixed vegetable soup", "50.00", null, true, false,
                img("veg-soup"));
        seedFood(healthy, "Fruit Bowl", "Fresh seasonal fruits", "80.00", null, true, false,
                img("fruit-bowl"));

        seedHistory(student);

        log.info("Seeded {} users, {} categories, {} food items.",
                userRepo.count(), categoryRepo.count(), foodRepo.count());
    }

    /** Fixed random seed, so a fresh demo always looks the same. */
    private void seedHistory(User student) {
        Random random = new Random(42);
        List<FoodItem> foods = foodRepo.findAll();
        LocalDate today = LocalDate.now(clock);
        int[] busyHours = {8, 9, 11, 12, 12, 13, 13, 13, 14, 16, 17};
        String[] comments = {"Fresh and hot.", "Ready exactly on time.", "Good portion for the price.", "Would order again."};

        for (int daysAgo = 14; daysAgo >= 1; daysAgo--) {
            LocalDate day = today.minusDays(daysAgo);
            int ordersThatDay = 4 + random.nextInt(9);
            for (int n = 1; n <= ordersThatDay; n++) {
                LocalDateTime placed = day.atTime(busyHours[random.nextInt(busyHours.length)], random.nextInt(60));
                LocalDateTime slot = placed.withMinute(0).plusHours(1);
                boolean online = random.nextInt(4) > 0;

                com.canteen.entity.Order order = com.canteen.entity.Order.builder()
                        .user(student)
                        .status(OrderStatus.COMPLETED)
                        .paymentStatus(PaymentStatus.PAID)
                        .paymentMethod(online ? PaymentMethod.ONLINE : PaymentMethod.CASH_ON_PICKUP)
                        .tokenNumber(String.format("A%03d", n))
                        .businessDate(day)
                        .pickupSlot(slot)
                        .pickupNonce(java.util.UUID.randomUUID().toString().replace("-", ""))
                        .orderDate(placed)
                        .updatedAt(slot)
                        .collectedAt(slot)
                        .collectedBy("staff@canteen.com")
                        .totalAmount(BigDecimal.ZERO)
                        .build();

                BigDecimal total = BigDecimal.ZERO;
                List<FoodItem> picked = new ArrayList<>();
                for (int i = 0, items = 1 + random.nextInt(3); i < items; i++) {
                    FoodItem food = foods.get(random.nextInt(foods.size()));
                    if (picked.contains(food)) continue;
                    picked.add(food);
                    int quantity = 1 + random.nextInt(2);
                    BigDecimal subtotal = food.effectivePrice().multiply(BigDecimal.valueOf(quantity));
                    total = total.add(subtotal);
                    order.addItem(OrderItem.builder().order(order).foodItem(food).quantity(quantity)
                            .price(food.effectivePrice()).subtotal(subtotal).build());
                }
                order.setTotalAmount(total);
                orderRepo.save(order);

                paymentRepo.save(Payment.builder()
                        .order(order).amount(total)
                        .gateway(online ? "sandbox" : "cash")
                        .paymentMethod(online ? "UPI" : "CASH")
                        .paymentStatus(PaymentStatus.PAID)
                        .paymentDate(online ? placed : slot)
                        .createdAt(placed)
                        .build());

                if (random.nextInt(3) == 0) {
                    reviewRepo.save(Review.builder()
                            .user(student).order(order).foodItem(picked.get(0))
                            .rating(3 + random.nextInt(3))
                            .comment(comments[random.nextInt(comments.length)])
                            .createdAt(slot.plusHours(1))
                            .build());
                }
            }
        }
    }

    private void seedFood(Category c, String name, String desc, String price, String specialPrice, boolean veg, boolean todaySpecial, String imageUrl) {
        foodRepo.save(FoodItem.builder()
                .category(c).name(name).description(desc)
                .price(new BigDecimal(price))
                .specialPrice(specialPrice != null ? new BigDecimal(specialPrice) : null)
                .isTodaySpecial(todaySpecial)
                .prepTimeMin(10).isVeg(veg).availability(true)
                .imageUrl(imageUrl).build());
    }
}