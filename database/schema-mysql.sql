-- BizFlow multi-business schema (MySQL 8+).
CREATE DATABASE IF NOT EXISTS bizflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bizflow;

CREATE TABLE IF NOT EXISTS user_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_user_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS businesses (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  owner_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_business_owner FOREIGN KEY (owner_id) REFERENCES user_accounts(id),
  INDEX idx_business_owner (owner_id, name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  sku VARCHAR(60) NOT NULL,
  barcode VARCHAR(80),
  name VARCHAR(180) NOT NULL,
  category VARCHAR(100),
  unit VARCHAR(30),
  variant VARCHAR(120),
  cost_price DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  selling_price DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  quantity DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  reorder_level DECIMAL(14,3) NOT NULL DEFAULT 0.000,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_business FOREIGN KEY (business_id) REFERENCES businesses(id),
  CONSTRAINT uq_product_sku_per_business UNIQUE (business_id, sku),
  CONSTRAINT uq_product_barcode_per_business UNIQUE (business_id, barcode)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS parties (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  type VARCHAR(20) NOT NULL,
  phone VARCHAR(30),
  email VARCHAR(160),
  address VARCHAR(300),
  CONSTRAINT fk_party_business FOREIGN KEY (business_id) REFERENCES businesses(id),
  INDEX idx_parties_business_type_name (business_id, type, name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchases (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  invoice_no VARCHAR(40) NOT NULL,
  supplier_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  total DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_purchase_business FOREIGN KEY (business_id) REFERENCES businesses(id),
  CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES parties(id),
  CONSTRAINT uq_purchase_invoice_per_business UNIQUE (business_id, invoice_no),
  INDEX idx_purchase_business_date (business_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  purchase_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(14,3) NOT NULL,
  unit_cost DECIMAL(14,2) NOT NULL,
  line_total DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_purchase_item_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id),
  CONSTRAINT fk_purchase_item_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sales (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  business_id BIGINT NOT NULL,
  invoice_no VARCHAR(40) NOT NULL,
  customer_id BIGINT NULL,
  created_at DATETIME NOT NULL,
  total DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  CONSTRAINT fk_sale_business FOREIGN KEY (business_id) REFERENCES businesses(id),
  CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES parties(id),
  CONSTRAINT uq_sale_invoice_per_business UNIQUE (business_id, invoice_no),
  INDEX idx_sale_business_date (business_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sale_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  sale_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(14,3) NOT NULL,
  unit_price DECIMAL(14,2) NOT NULL,
  unit_cost_at_sale DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  line_total DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sales(id),
  CONSTRAINT fk_sale_item_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB;
