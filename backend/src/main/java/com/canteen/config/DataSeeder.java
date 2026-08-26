package com.canteen.config;

import com.canteen.entity.*;
import com.canteen.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile({"dev", "default"})
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final FoodItemRepository foodRepo;
    private final PasswordEncoder passwordEncoder;

    private static final String IMG = "https://images.unsplash.com/photo-";

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
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
                IMG + "1668236543090-82eba5ee5976?w=400&q=80");
        seedFood(southIndian, "Idli Sambar", "Soft steamed idlis with sambar & coconut chutney", "50.00", null, true, false,
                IMG + "1589301760014-d929f3979dbc?w=400&q=80");
        seedFood(southIndian, "Medu Vada", "Crispy golden vadas with chutney & sambar", "55.00", null, true, false,
                IMG + "1630383249896-6e5725f3215e?w=400&q=80");
        seedFood(southIndian, "Rava Dosa", "Crispy semolina dosa with onions & spices", "70.00", null, true, false,
                IMG + "1626777551878-d1b40e3e46b3?w=400&q=80");
        seedFood(southIndian, "Uttapam", "Thick pancake topped with onions & tomatoes", "60.00", null, true, false,
                IMG + "1645177628172-a94c1f96e6db?w=400&q=80");

        seedFood(northIndian, "Paneer Butter Masala", "Creamy tomato gravy with soft paneer cubes", "150.00", "120.00", true, true,
                IMG + "1585937421612-70a008356fbe?w=400&q=80");
        seedFood(northIndian, "Veg Biryani", "Aromatic basmati rice with mixed vegetables", "130.00", "105.00", true, true,
                IMG + "1563379091339-03b21ab4a4f8?w=400&q=80");
        seedFood(northIndian, "Aloo Paratha", "Stuffed potato flatbread with butter", "70.00", null, true, false,
                IMG + "1596560549533-d0a1f8f8c48a?w=400&q=80");
        seedFood(northIndian, "Dal Makhani", "Rich creamy black lentils slow-cooked overnight", "120.00", null, true, false,
                IMG + "1546549032-9571f012c2bd?w=400&q=80");
        seedFood(northIndian, "Chole Bhature", "Spiced chickpeas with deep-fried bread", "90.00", null, true, false,
                IMG + "1623065425894-9d44d4e6a480?w=400&q=80");
        seedFood(northIndian, "North Indian Thali", "Complete meal with roti, dal, sabzi, rice & dessert", "180.00", "150.00", true, false,
                IMG + "1547592180-85f173990554?w=400&q=80");

        seedFood(chinese, "Veg Fried Rice", "Wok-tossed rice with vegetables & soy", "100.00", "80.00", true, true,
                IMG + "1603133872878-684f208fb84b?w=400&q=80");
        seedFood(chinese, "Paneer Fried Rice", "Fried rice with chunky paneer pieces", "120.00", null, true, false,
                IMG + "1604908177453-7462950a6a3b?w=400&q=80");
        seedFood(chinese, "Veg Manchurian", "Crispy vegetable balls in spicy Indo-Chinese sauce", "110.00", null, true, false,
                IMG + "1569058245601-5bb8b29d5d64?w=400&q=80");
        seedFood(chinese, "Hakka Noodles", "Stir-fried noodles with vegetables", "100.00", null, true, false,
                IMG + "1618449840664-34d1b39aqu6c?w=400&q=80");
        seedFood(chinese, "Schezwan Fried Rice", "Spicy fried rice with schezwan sauce", "110.00", null, true, false,
                IMG + "1603133872878-684f208fb84b?w=400&q=80");

        seedFood(snacks, "Samosa", "Crispy triangular pastry with spiced potato filling", "20.00", "15.00", true, true,
                IMG + "1601050690117-94f5f6fa8bd7?w=400&q=80");
        seedFood(snacks, "Veg Sandwich", "Grilled sandwich with fresh vegetables & cheese", "60.00", null, true, false,
                IMG + "1528735602780-2552fd46c7af?w=400&q=80");
        seedFood(snacks, "Pakora Plate", "Assorted vegetable fritters with mint chutney", "50.00", null, true, false,
                IMG + "1630383249896-6e5725f3215e?w=400&q=80");
        seedFood(snacks, "Aloo Tikki", "Golden potato patties with chutneys", "30.00", null, true, false,
                IMG + "1606493447075-6c8f0b21e1a9?w=400&q=80");
        seedFood(snacks, "Paneer Tikka", "Marinated paneer cubes grilled to perfection", "80.00", null, true, false,
                IMG + "1565557623262-b51c2513a641?w=400&q=80");

        seedFood(beverages, "Masala Chai", "Strong Indian tea brewed with spices & milk", "15.00", "10.00", true, true,
                IMG + "1544785974-6dfe24e0f4be?w=400&q=80");
        seedFood(beverages, "Filter Coffee", "Strong South Indian filter coffee", "25.00", "20.00", true, false,
                IMG + "1509042239860-f550ce710b93?w=400&q=80");
        seedFood(beverages, "Fresh Lime Soda", "Refreshing lime with soda and chat masala", "35.00", null, true, false,
                IMG + "1556679343-c7306c1976bc?w=400&q=80");
        seedFood(beverages, "Cold Coffee", "Chilled creamy coffee with ice cream", "60.00", null, true, false,
                IMG + "1485808191679-5f86510681a2?w=400&q=80");
        seedFood(beverages, "Mango Lassi", "Thick creamy mango yogurt smoothie", "50.00", null, true, false,
                IMG + "1556679343-c7306c1976bc?w=400&q=80");

        seedFood(desserts, "Gulab Jamun", "Soft milk dumplings soaked in rose-flavored syrup", "40.00", "30.00", true, true,
                IMG + "1666190094745-d7505329e8bc?w=400&q=80");
        seedFood(desserts, "Ice Cream", "Creamy ice cream - Vanilla, Chocolate or Strawberry", "50.00", null, true, false,
                IMG + "1497034825429-c09f8c2a4b76?w=400&q=80");
        seedFood(desserts, "Brownie", "Warm chocolate brownie with nuts", "70.00", null, true, false,
                IMG + "1606313564200-e75d5e30f1a5?w=400&q=80");
        seedFood(desserts, "Rasgulla", "Spongy white cheese balls in light sugar syrup", "35.00", null, true, false,
                IMG + "1653357702817-d3e66f4f5951?w=400&q=80");
        seedFood(desserts, "Cheesecake", "Creamy baked cheesecake with berry topping", "90.00", null, true, false,
                IMG + "1524351199678-941a58a3df50?w=400&q=80");

        seedFood(combos, "South Indian Combo", "Masala Dosa + Idli + Filter Coffee", "140.00", null, true, false,
                IMG + "1567337710282-8fc1e2f3bc1a?w=400&q=80");
        seedFood(combos, "North Indian Combo", "Aloo Paratha + Dal Makhani + Chai", "160.00", null, true, false,
                IMG + "1547592180-85f173990554?w=400&q=80");
        seedFood(combos, "Chinese Combo", "Fried Rice + Manchurian + Coke", "170.00", null, true, false,
                IMG + "1603133872878-684f208fb84b?w=400&q=80");
        seedFood(combos, "Street Food Combo", "Samosa + Sandwich + Cold Coffee", "120.00", null, true, false,
                IMG + "1601050690117-94f5f6fa8bd7?w=400&q=80");
        seedFood(combos, "Student Meal", "Veg Biryani + Gulab Jamun + Water", "160.00", null, true, false,
                IMG + "1563379091339-03b21ab4a4f8?w=400&q=80");

        seedFood(healthy, "Greek Salad", "Fresh cucumbers, tomatoes, feta & olives", "90.00", null, true, false,
                IMG + "1540420773420-d2f2b83b6f8e?w=400&q=80");
        seedFood(healthy, "Oats Upma", "Healthy oats cooked with vegetables", "70.00", null, true, false,
                IMG + "1626777551878-d1b40e3e46b3?w=400&q=80");
        seedFood(healthy, "Grilled Paneer Salad", "Fresh salad with grilled paneer cubes", "110.00", null, true, false,
                IMG + "1565557623262-b51c2513a641?w=400&q=80");
        seedFood(healthy, "Veg Soup", "Hot & healthy mixed vegetable soup", "50.00", null, true, false,
                IMG + "1547592180-85f173990554?w=400&q=80");
        seedFood(healthy, "Fruit Bowl", "Fresh seasonal fruits", "80.00", null, true, false,
                IMG + "1490474418585-ba9bad8fd0ea?w=400&q=80");

        log.info("Seeded {} users, {} categories, {} food items.",
                userRepo.count(), categoryRepo.count(), foodRepo.count());
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