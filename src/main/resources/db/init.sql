-- Smart Meat Shop Database Schema
-- PostgreSQL 15+

CREATE TYPE user_role AS ENUM ('ADMIN', 'SELLER', 'CUSTOMER');
CREATE TYPE order_status AS ENUM ('PENDING', 'ACCEPTED', 'PREPARING', 'READY', 'COLLECTED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH', 'UPI', 'CARD', 'KHATA');
CREATE TYPE stock_status AS ENUM ('IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK');
CREATE TYPE shop_status AS ENUM ('OPEN', 'CLOSED', 'TEMPORARILY_CLOSED');
CREATE TYPE expense_category AS ENUM ('ICE_PURCHASE', 'TRANSPORT', 'WORKER_WAGES', 'SHOP_RENT', 'ELECTRICITY', 'PACKAGING', 'OTHER');
CREATE TYPE transaction_type AS ENUM ('CREDIT', 'DEBIT');
CREATE TYPE transaction_category AS ENUM ('SALES', 'STOCK_PURCHASE', 'EXPENSE', 'KHATA_PAYMENT', 'KHATA_PURCHASE');

-- Users
CREATE TABLE users (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  mobile     VARCHAR(15) NOT NULL UNIQUE,
  email      VARCHAR(150) UNIQUE,
  password   VARCHAR(255) NOT NULL,
  role       user_role NOT NULL DEFAULT 'CUSTOMER',
  avatar_url VARCHAR(500),
  is_active  BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Product categories
CREATE TABLE categories (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(50) NOT NULL UNIQUE,
  slug       VARCHAR(50) NOT NULL UNIQUE,
  icon       VARCHAR(10),
  sort_order INT DEFAULT 0,
  is_active  BOOLEAN DEFAULT TRUE
);

-- Products
CREATE TABLE products (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(100) NOT NULL,
  description     TEXT,
  category_id     BIGINT REFERENCES categories(id),
  price_per_kg    DECIMAL(10,2) NOT NULL,
  cost_per_kg     DECIMAL(10,2),
  stock_qty       DECIMAL(10,3) DEFAULT 0,
  min_stock_level DECIMAL(10,3) DEFAULT 5,
  min_order_qty   DECIMAL(5,3) DEFAULT 0.5,
  order_step      DECIMAL(5,3) DEFAULT 0.5,
  stock_status    stock_status DEFAULT 'IN_STOCK',
  image_url       VARCHAR(500),
  is_available    BOOLEAN DEFAULT TRUE,
  sort_order      INT DEFAULT 0,
  created_by      BIGINT REFERENCES users(id),
  created_at      TIMESTAMPTZ DEFAULT NOW(),
  updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Shop settings (singleton row)
CREATE TABLE shop_settings (
  id            BIGSERIAL PRIMARY KEY,
  shop_name     VARCHAR(100) DEFAULT 'Smart Meat Shop',
  tagline       VARCHAR(200),
  phone         VARCHAR(15),
  email         VARCHAR(150),
  address       TEXT,
  latitude      DECIMAL(10,8),
  longitude     DECIMAL(11,8),
  status        shop_status DEFAULT 'OPEN',
  open_time     TIME DEFAULT '07:00:00',
  close_time    TIME DEFAULT '20:00:00',
  sunday_close  TIME DEFAULT '14:00:00',
  logo_url      VARCHAR(500),
  banner_url    VARCHAR(500),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Customers (extended profile linked to users with CUSTOMER role)
CREATE TABLE customers (
  id           BIGSERIAL PRIMARY KEY,
  user_id      BIGINT REFERENCES users(id) UNIQUE,
  address      TEXT,
  app_installed BOOLEAN DEFAULT FALSE,
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- Orders
CREATE TABLE orders (
  id              BIGSERIAL PRIMARY KEY,
  order_number    VARCHAR(20) NOT NULL UNIQUE,
  customer_id     BIGINT REFERENCES users(id),
  customer_name   VARCHAR(100),
  customer_mobile VARCHAR(15),
  status          order_status DEFAULT 'PENDING',
  payment_method  payment_method DEFAULT 'CASH',
  subtotal        DECIMAL(10,2) NOT NULL,
  total           DECIMAL(10,2) NOT NULL,
  notes           TEXT,
  is_online_order BOOLEAN DEFAULT FALSE,
  processed_by    BIGINT REFERENCES users(id),
  created_at      TIMESTAMPTZ DEFAULT NOW(),
  updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Order items
CREATE TABLE order_items (
  id          BIGSERIAL PRIMARY KEY,
  order_id    BIGINT REFERENCES orders(id) ON DELETE CASCADE,
  product_id  BIGINT REFERENCES products(id),
  product_name VARCHAR(100) NOT NULL,
  qty         DECIMAL(10,3) NOT NULL,
  unit_price  DECIMAL(10,2) NOT NULL,
  total       DECIMAL(10,2) NOT NULL
);

-- Khata accounts (credit accounts for bulk customers)
CREATE TABLE khata_accounts (
  id            BIGSERIAL PRIMARY KEY,
  customer_id   BIGINT REFERENCES users(id) UNIQUE,
  credit_limit  DECIMAL(10,2) DEFAULT 10000,
  current_due   DECIMAL(10,2) DEFAULT 0,
  total_credit  DECIMAL(10,2) DEFAULT 0,
  is_active     BOOLEAN DEFAULT TRUE,
  created_by    BIGINT REFERENCES users(id),
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Khata ledger entries
CREATE TABLE khata_entries (
  id             BIGSERIAL PRIMARY KEY,
  account_id     BIGINT REFERENCES khata_accounts(id),
  order_id       BIGINT REFERENCES orders(id),
  entry_type     VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
  amount         DECIMAL(10,2) NOT NULL,
  description    TEXT,
  reference_note VARCHAR(200),
  entered_by     BIGINT REFERENCES users(id),
  entry_date     DATE DEFAULT CURRENT_DATE,
  created_at     TIMESTAMPTZ DEFAULT NOW()
);

-- Stock purchases (supplier invoices)
CREATE TABLE stock_purchases (
  id            BIGSERIAL PRIMARY KEY,
  product_id    BIGINT REFERENCES products(id),
  supplier_name VARCHAR(100),
  qty           DECIMAL(10,3) NOT NULL,
  cost_per_kg   DECIMAL(10,2) NOT NULL,
  total_cost    DECIMAL(10,2) NOT NULL,
  amount_paid   DECIMAL(10,2) DEFAULT 0,
  invoice_no    VARCHAR(50),
  purchase_date DATE DEFAULT CURRENT_DATE,
  entered_by    BIGINT REFERENCES users(id),
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Expenses
CREATE TABLE expenses (
  id          BIGSERIAL PRIMARY KEY,
  category    expense_category NOT NULL,
  amount      DECIMAL(10,2) NOT NULL,
  description TEXT,
  expense_date DATE DEFAULT CURRENT_DATE,
  entered_by  BIGINT REFERENCES users(id),
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Reviews
CREATE TABLE reviews (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT REFERENCES users(id),
  order_id    BIGINT REFERENCES orders(id),
  rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     TEXT,
  tags        TEXT[],
  is_approved BOOLEAN DEFAULT TRUE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Notifications (for admin/seller)
CREATE TABLE notifications (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT REFERENCES users(id),
  title       VARCHAR(200) NOT NULL,
  message     TEXT,
  type        VARCHAR(30) DEFAULT 'INFO',
  entity_type VARCHAR(30),
  entity_id   BIGINT,
  is_read     BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Media / uploads
CREATE TABLE media (
  id          BIGSERIAL PRIMARY KEY,
  file_name   VARCHAR(255) NOT NULL,
  file_path   VARCHAR(500) NOT NULL,
  file_type   VARCHAR(50),
  file_size   BIGINT,
  entity_type VARCHAR(50),
  entity_id   BIGINT,
  uploaded_by BIGINT REFERENCES users(id),
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ── Indexes ──────────────────────────────────────────────
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_created ON orders(created_at);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_khata_entries_account ON khata_entries(account_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- ── Seed data ────────────────────────────────────────────
-- Default shop settings
INSERT INTO shop_settings (shop_name, tagline, phone, email, address, latitude, longitude)
VALUES ('Smart Meat Shop', 'Fresh Fish · Chicken · Mutton · Daily',
        '9121200123', 'smartmeatshop@gmail.com',
        'Opp. Main Market, Fish Market Road, Hyderabad, Telangana — 500001',
        17.4461, 78.4739);

-- Categories
INSERT INTO categories (name, slug, icon, sort_order) VALUES
  ('Fish',    'fish',    '🐟', 1),
  ('Chicken', 'chicken', '🍗', 2),
  ('Mutton',  'mutton',  '🐑', 3);

-- Default admin user (password: Admin@123 — bcrypt)
INSERT INTO users (name, mobile, email, password, role) VALUES
  ('Admin', '9000000000', 'admin@smartmeat.com',
   '$2a$12$r5L8oF4G0V5KQjJ2cZ7.XOoB1Fh3kQ2zSmtBLMNxXW3c8KfVbH5Aq', 'ADMIN'),
  ('Seller One', '9000000001', 'seller@smartmeat.com',
   '$2a$12$r5L8oF4G0V5KQjJ2cZ7.XOoB1Fh3kQ2zSmtBLMNxXW3c8KfVbH5Aq', 'SELLER');

-- Sample products
INSERT INTO products (name, description, category_id, price_per_kg, cost_per_kg, stock_qty, min_stock_level, image_url, is_available) VALUES
  ('Rohu Fish',       'Fresh water fish, perfect for curries',      1, 280, 210, 25, 15, '/uploads/rohu.jpg',    TRUE),
  ('Katla Fish',      'Large river fish, mild flavour',             1, 260, 195, 30, 10, '/uploads/katla.jpg',   TRUE),
  ('Pomfret',         'Coastal delicacy, excellent fried',          1, 380, 290, 20, 8,  '/uploads/pomfret.jpg', TRUE),
  ('Tilapia',         'Mild white fish, great for families',        1, 180, 130, 35, 10, '/uploads/tilapia.jpg', TRUE),
  ('Broiler Chicken', 'Farm fresh, cleaned and dressed',            2, 240, 190, 45, 10, '/uploads/broiler.jpg', TRUE),
  ('Country Chicken', 'Free range, rich natural flavour',           2, 320, 260, 18, 8,  '/uploads/country.jpg', TRUE),
  ('Curry Cut',       'Ready to cook curry pieces',                 2, 280, 220, 22, 8,  '/uploads/curry.jpg',   TRUE),
  ('Boneless Chicken','Cleaned boneless pieces',                    2, 300, 240, 15, 5,  '/uploads/boneless.jpg',TRUE),
  ('Goat Curry Cut',  'Fresh goat meat, curry cut pieces',          3, 620, 480, 8,  10, '/uploads/goat.jpg',    TRUE),
  ('Boneless Mutton', 'Premium boneless goat meat',                 3, 720, 560, 12, 8,  '/uploads/bonelessm.jpg',TRUE),
  ('Liver',           'Fresh goat liver, rich in iron',             3, 220, 160, 6,  5,  '/uploads/liver.jpg',   TRUE),
  ('Mince (Kheema)',  'Freshly minced mutton',                      3, 540, 420, 10, 5,  '/uploads/mince.jpg',   TRUE);

-- ── NEW: Suppliers ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS suppliers (
  id           BIGSERIAL PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  mobile       VARCHAR(15),
  email        VARCHAR(150),
  address      TEXT,
  products     TEXT,                        -- comma-separated product names for reference
  credit_limit DECIMAL(10,2) DEFAULT 0,
  current_due  DECIMAL(10,2) DEFAULT 0,
  is_active    BOOLEAN DEFAULT TRUE,
  created_by   BIGINT REFERENCES users(id),
  created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ── NEW: Supplier ledger entries ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supplier_ledger_entries (
  id              BIGSERIAL PRIMARY KEY,
  supplier_id     BIGINT REFERENCES suppliers(id) ON DELETE CASCADE,
  purchase_id     BIGINT REFERENCES stock_purchases(id),
  entry_type      VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT','CREDIT')),
  amount          DECIMAL(10,2) NOT NULL,
  cash_amount     DECIMAL(10,2) DEFAULT 0,      -- portion paid in cash
  account_amount  DECIMAL(10,2) DEFAULT 0,      -- portion paid via UPI/card
  description     TEXT,
  reference_note  VARCHAR(200),
  entered_by      BIGINT REFERENCES users(id),
  entry_date      DATE DEFAULT CURRENT_DATE,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── NEW: Add supplier_id + payment_mode to stock_purchases ────────────────────
ALTER TABLE stock_purchases
  ADD COLUMN IF NOT EXISTS supplier_id   BIGINT REFERENCES suppliers(id),
  ADD COLUMN IF NOT EXISTS payment_mode  VARCHAR(20) DEFAULT 'CASH',
  ADD COLUMN IF NOT EXISTS cash_paid     DECIMAL(10,2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS account_paid  DECIMAL(10,2) DEFAULT 0;

-- ── NEW: Shop cash/account balance tracking ───────────────────────────────────
ALTER TABLE shop_settings
  ADD COLUMN IF NOT EXISTS cash_balance    DECIMAL(12,2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS account_balance DECIMAL(12,2) DEFAULT 0;

-- ── NEW: Add payment_mode to expenses too ─────────────────────────────────────
ALTER TABLE expenses
  ADD COLUMN IF NOT EXISTS payment_mode  VARCHAR(20) DEFAULT 'CASH',
  ADD COLUMN IF NOT EXISTS cash_amount   DECIMAL(10,2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS account_amount DECIMAL(10,2) DEFAULT 0;