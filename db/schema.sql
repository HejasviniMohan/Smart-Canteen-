-- =====================================================
-- Smart Canteen Management System
-- PostgreSQL Schema (for production deployment)
-- =====================================================

-- Drop in reverse-dependency order
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS food_items CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ===================== USERS =====================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    password        VARCHAR(255)  NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)   NOT NULL CHECK (role IN ('STUDENT','STAFF','ADMIN')),
    enabled         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_role ON users(role);

-- ===================== CATEGORIES =====================
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(80)   NOT NULL UNIQUE,
    description     VARCHAR(255),
    image_url       VARCHAR(500),
    display_order   INT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================== FOOD ITEMS =====================
CREATE TABLE food_items (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT        NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    name            VARCHAR(120)  NOT NULL,
    description     VARCHAR(500),
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    special_price   NUMERIC(10,2),
    is_today_special BOOLEAN      NOT NULL DEFAULT FALSE,
    availability    BOOLEAN       NOT NULL DEFAULT TRUE,
    image_url       VARCHAR(500),
    prep_time_min   INT           NOT NULL DEFAULT 10,
    is_veg          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_food_items_category ON food_items(category_id);
CREATE INDEX idx_food_items_avail ON food_items(availability);

-- ===================== ORDERS =====================
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    total_amount    NUMERIC(10,2) NOT NULL CHECK (total_amount >= 0),
    status          VARCHAR(20)   NOT NULL CHECK (status IN ('PLACED','ACCEPTED','PREPARING','READY','COMPLETED','CANCELLED')),
    token_number    VARCHAR(20)   NOT NULL UNIQUE,
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    notes           VARCHAR(500),
    order_date      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_date ON orders(order_date);

-- ===================== ORDER ITEMS =====================
CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    food_item_id    BIGINT        NOT NULL REFERENCES food_items(id) ON DELETE RESTRICT,
    quantity        INT           NOT NULL CHECK (quantity > 0),
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    subtotal        NUMERIC(10,2) NOT NULL CHECK (subtotal >= 0)
);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_food ON order_items(food_item_id);

-- ===================== PAYMENTS =====================
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    amount          NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    payment_method  VARCHAR(40)   NOT NULL,
    payment_status  VARCHAR(20)   NOT NULL CHECK (payment_status IN ('PENDING','PAID','FAILED','REFUNDED')),
    transaction_id  VARCHAR(120),
    razorpay_order_id       VARCHAR(120),
    razorpay_payment_id     VARCHAR(120),
    razorpay_signature      VARCHAR(255),
    payment_date    TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================== REVIEWS =====================
CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    food_item_id    BIGINT        NOT NULL REFERENCES food_items(id) ON DELETE CASCADE,
    order_id        BIGINT        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    rating          INT           NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         VARCHAR(500),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_food_review UNIQUE (user_id, food_item_id, order_id)
);
CREATE INDEX idx_reviews_food ON reviews(food_item_id);

-- ===================== NOTIFICATIONS =====================
CREATE TABLE notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(150)  NOT NULL,
    message         VARCHAR(500)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);

-- =====================================================
-- SEED DATA (passwords BCrypt hashed)
-- Admin@123, Staff@123, Student@123
-- Hashes generated with strength=10
-- =====================================================

INSERT INTO users (name, email, password, phone, role) VALUES
('Admin User',    'admin@canteen.com',    '$2a$10$qE5kVJ0aO/3vFhkLm2P4ROqUkGXLpPlJcDXVy1nYHfvKKdEQmxsaC', '9999900000', 'ADMIN'),
('Staff User',    'staff@canteen.com',    '$2a$10$D1y1iRn5pVyT5jXJyOov7OhqKqpVbp1pVOQMxGGK2PNxQ2TLwlzsa', '9999900001', 'STAFF'),
('Student User',  'student@canteen.com',  '$2a$10$lD3LGCjQqW.vYxUKPL7CDeSPqRpVu6sJ6zRJx7.eRMk8LY9YHJ25.', '9999900002', 'STUDENT');

INSERT INTO categories (name, description, display_order) VALUES
('South Indian', 'Idli, Dosa, Vada and more', 1),
('North Indian', 'Curries, Roti, Paratha', 2),
('Chinese',      'Fried rice, Noodles, Manchurian', 3),
('Snacks',       'Samosa, Sandwich, Pakora', 4),
('Beverages',    'Tea, Coffee, Juice', 5),
('Desserts',     'Sweet treats and ice cream', 6),
('Combos',       'Value meal combos', 7),
('Healthy',      'Salads, Soups, Grilled', 8);

INSERT INTO food_items (category_id, name, description, price, special_price, is_today_special, availability, prep_time_min, is_veg) VALUES
-- South Indian
(1, 'Masala Dosa',     'Crispy dosa with potato masala & chutney',  80.00, 65.00, TRUE,  TRUE, 12, TRUE),
(1, 'Idli Sambar',     'Soft steamed idlis with sambar & chutney', 50.00, NULL, FALSE, TRUE, 8,  TRUE),
(1, 'Medu Vada',       'Crispy golden vadas with chutney & sambar',  55.00, NULL, FALSE, TRUE, 10, TRUE),
(1, 'Rava Dosa',        'Crispy semolina dosa with onions & spices',  70.00, NULL, FALSE, TRUE, 12, TRUE),
(1, 'Uttapam',          'Thick pancake topped with onions & tomatoes', 60.00, NULL, FALSE, TRUE, 10, TRUE),
-- North Indian
(2, 'Paneer Butter Masala', 'Creamy tomato gravy with soft paneer cubes', 150.00, 120.00, TRUE, TRUE, 15, TRUE),
(2, 'Veg Biryani',      'Aromatic basmati rice with mixed vegetables', 130.00, 105.00, TRUE, TRUE, 18, TRUE),
(2, 'Aloo Paratha',    'Stuffed potato flatbread with butter',       70.00, NULL, FALSE, TRUE, 10, TRUE),
(2, 'Dal Makhani',      'Rich creamy black lentils slow-cooked overnight', 120.00, NULL, FALSE, TRUE, 15, TRUE),
(2, 'Chole Bhature',   'Spiced chickpeas with deep-fried bread',    90.00, NULL, FALSE, TRUE, 12, TRUE),
(2, 'North Indian Thali','Complete meal with roti, dal, sabzi, rice & dessert', 180.00, 150.00, FALSE, TRUE, 20, TRUE),
-- Chinese
(3, 'Veg Fried Rice',   'Wok-tossed rice with vegetables & soy',  100.00, 80.00, TRUE, TRUE, 12, TRUE),
(3, 'Paneer Fried Rice','Fried rice with chunky paneer pieces',   120.00, NULL, FALSE, TRUE, 14, TRUE),
(3, 'Veg Manchurian',  'Crispy veg balls in spicy Indo-Chinese sauce', 110.00, NULL, FALSE, TRUE, 15, TRUE),
(3, 'Hakka Noodles',   'Stir-fried noodles with vegetables',        100.00, NULL, FALSE, TRUE, 12, TRUE),
(3, 'Schezwan Fried Rice','Spicy fried rice with schezwan sauce',  110.00, NULL, FALSE, TRUE, 12, TRUE),
-- Snacks
(4, 'Samosa',          'Crispy triangular pastry with spiced potato filling', 20.00, 15.00, TRUE, TRUE, 5, TRUE),
(4, 'Veg Sandwich',    'Grilled sandwich with fresh vegetables & cheese', 60.00, NULL, FALSE, TRUE, 7, TRUE),
(4, 'Pakora Plate',    'Assorted vegetable fritters with mint chutney', 50.00, NULL, FALSE, TRUE, 8, TRUE),
(4, 'Aloo Tikki',      'Golden potato patties with chutneys',          30.00, NULL, FALSE, TRUE, 6, TRUE),
(4, 'Paneer Tikka',   'Marinated paneer cubes grilled to perfection',  80.00, NULL, FALSE, TRUE, 12, TRUE),
-- Beverages
(5, 'Masala Chai',     'Strong Indian tea brewed with spices & milk',  15.00, 10.00, TRUE, TRUE, 3, TRUE),
(5, 'Filter Coffee',   'Strong South Indian filter coffee',            25.00, 20.00, TRUE, FALSE, 4, TRUE),
(5, 'Fresh Lime Soda', 'Refreshing lime with soda and chat masala',   35.00, NULL, FALSE, TRUE, 3, TRUE),
(5, 'Cold Coffee',    'Chilled creamy coffee with ice cream',        60.00, NULL, FALSE, TRUE, 4, TRUE),
(5, 'Mango Lassi',    'Thick creamy mango yogurt smoothie',           50.00, NULL, FALSE, TRUE, 3, TRUE),
-- Desserts
(6, 'Gulab Jamun',    'Soft milk dumplings in rose-flavored syrup',  40.00, 30.00, TRUE, TRUE, 3, TRUE),
(6, 'Ice Cream',      'Creamy ice cream - Vanilla, Chocolate or Strawberry', 50.00, NULL, FALSE, TRUE, 2, TRUE),
(6, 'Brownie',         'Warm chocolate brownie with nuts',             70.00, NULL, FALSE, TRUE, 5, TRUE),
(6, 'Rasgulla',        'Spongy white cheese balls in light sugar syrup', 35.00, NULL, FALSE, TRUE, 3, TRUE),
(6, 'Cheesecake',      'Creamy baked cheesecake with berry topping',   90.00, NULL, FALSE, TRUE, 5, TRUE),
-- Combos (in-person only - not available online)
(7, 'South Indian Combo', 'Masala Dosa + Idli + Filter Coffee',    140.00, NULL, FALSE, TRUE, 15, TRUE),
(7, 'North Indian Combo', 'Aloo Paratha + Dal Makhani + Chai',      160.00, NULL, FALSE, TRUE, 15, TRUE),
(7, 'Chinese Combo',      'Fried Rice + Manchurian + Coke',          170.00, NULL, FALSE, TRUE, 15, TRUE),
(7, 'Street Food Combo',  'Samosa + Sandwich + Cold Coffee',         120.00, NULL, FALSE, TRUE, 10, TRUE),
(7, 'Student Meal',       'Veg Biryani + Gulab Jamun + Water',     160.00, NULL, FALSE, TRUE, 18, TRUE),
-- Healthy
(8, 'Greek Salad',    'Fresh cucumbers, tomatoes, feta & olives',    90.00, NULL, FALSE, TRUE, 5, TRUE),
(8, 'Oats Upma',      'Healthy oats cooked with vegetables',          70.00, NULL, FALSE, TRUE, 10, TRUE),
(8, 'Grilled Paneer Salad','Fresh salad with grilled paneer cubes',   110.00, NULL, FALSE, TRUE, 10, TRUE),
(8, 'Veg Soup',       'Hot & healthy mixed vegetable soup',           50.00, NULL, FALSE, TRUE, 5, TRUE),
(8, 'Fruit Bowl',     'Fresh seasonal fruits',                       80.00, NULL, FALSE, TRUE, 3, TRUE);