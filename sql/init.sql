CREATE DATABASE IF NOT EXISTS mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `mobile` VARCHAR(20) UNIQUE COMMENT '手机号',
  `nickname` VARCHAR(64) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 商品 SPU
CREATE TABLE IF NOT EXISTS `product_spu` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `spu_code` VARCHAR(64) UNIQUE,
  `name` VARCHAR(255) NOT NULL,
  `category_id` BIGINT,
  `brand_id` BIGINT,
  `main_image` VARCHAR(255),
  `status` TINYINT DEFAULT 1 COMMENT '1上架 0下架',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 商品 SKU
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `spu_id` BIGINT NOT NULL,
  `sku_code` VARCHAR(64) UNIQUE,
  `spec` JSON COMMENT '规格组合',
  `sale_price` DECIMAL(12,2) NOT NULL,
  `market_price` DECIMAL(12,2),
  `cost_price` DECIMAL(12,2),
  `stock` INT DEFAULT 0,
  `version` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  INDEX `idx_spu_id` (`spu_id`)
);

-- 优惠券模板
CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `template_code` VARCHAR(64) UNIQUE,
  `name` VARCHAR(128) NOT NULL,
  `type` TINYINT NOT NULL COMMENT '1满减 2折扣 3无门槛 4运费券',
  `value` DECIMAL(12,2) COMMENT '面额或折扣值',
  `min_order_amount` DECIMAL(12,2) DEFAULT 0,
  `max_discount_amount` DECIMAL(12,2) DEFAULT 0,
  `total_count` INT NOT NULL,
  `remain_count` INT NOT NULL,
  `limit_per_user` INT DEFAULT 1,
  `claim_start_time` DATETIME,
  `claim_end_time` DATETIME,
  `valid_days` INT DEFAULT 0,
  `valid_start_time` DATETIME,
  `valid_end_time` DATETIME,
  `scope_type` TINYINT DEFAULT 1 COMMENT '1全平台 2类目 3商品 4商家',
  `scope_ids` JSON,
  `can_stack` TINYINT DEFAULT 0,
  `channel` TINYINT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `version` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 用户优惠券实例
CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `coupon_no` VARCHAR(64) UNIQUE NOT NULL,
  `user_id` BIGINT NOT NULL,
  `template_id` BIGINT NOT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '1未使用 2已锁定 3已使用 4已过期 5已作废',
  `order_no` VARCHAR(64),
  `claim_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `valid_start_time` DATETIME,
  `valid_end_time` DATETIME,
  `use_time` DATETIME,
  `source` TINYINT DEFAULT 1,
  `version` INT DEFAULT 0,
  INDEX `idx_user_status` (`user_id`, `status`),
  INDEX `idx_template_id` (`template_id`)
);

-- 秒杀活动
CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `activity_name` VARCHAR(128),
  `sku_id` BIGINT NOT NULL,
  `seckill_price` DECIMAL(12,2) NOT NULL,
  `total_stock` INT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 秒杀订单记录
CREATE TABLE IF NOT EXISTS `seckill_order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `activity_id` BIGINT NOT NULL,
  `order_no` VARCHAR(64) UNIQUE,
  `user_id` BIGINT NOT NULL,
  `sku_id` BIGINT,
  `quantity` INT,
  `status` TINYINT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`)
);

-- 初始化测试数据
INSERT INTO `user` (`mobile`, `nickname`) VALUES ('13800138000', '测试用户');

INSERT INTO `product_spu` (`spu_code`, `name`, `status`) VALUES ('SPU001', '测试手机', 1);
INSERT INTO `product_sku` (`spu_id`, `sku_code`, `spec`, `sale_price`, `stock`) VALUES
(1, 'SKU001', '{"颜色":"黑色","内存":"128G"}', 2999.00, 100);

INSERT INTO `coupon_template` (`template_code`, `name`, `type`, `value`, `min_order_amount`, `total_count`, `remain_count`, `limit_per_user`, `claim_start_time`, `claim_end_time`, `valid_start_time`, `valid_end_time`, `status`)
VALUES ('CT001', '新人满100减20券', 1, 20.00, 100.00, 1000, 1000, 1, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1);

INSERT INTO `seckill_activity` (`activity_name`, `sku_id`, `seckill_price`, `total_stock`, `start_time`, `end_time`)
VALUES ('618秒杀手机', 1, 1999.00, 10, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY));
