-- ⚠️ DANGEROUS: This will DELETE the whole ecommerce_db database.
-- Use this when you previously created tables with a different schema and now foreign keys fail.

DROP DATABASE IF EXISTS ecommerce_db;

CREATE DATABASE IF NOT EXISTS ecommerce_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

