-- Green Miles courier schema for production-style courier flow.
-- Run this once on greenmilesbooking before using new courier APIs.

USE `greenmilesbooking`;

CREATE TABLE IF NOT EXISTS `courier_orders` (
  `courier_order_id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `driver_id` INT NULL,
  `awb_number` VARCHAR(32) NOT NULL,
  `qr_token` VARCHAR(64) NOT NULL,
  `pickup_address` VARCHAR(512) NOT NULL,
  `drop_address` VARCHAR(512) NOT NULL,
  `weight_kg` DECIMAL(8,2) NOT NULL,
  `distance_km` DECIMAL(10,2) NOT NULL,
  `package_category` VARCHAR(32) NOT NULL DEFAULT 'PARCEL',
  `recipient_name` VARCHAR(200) NULL,
  `recipient_phone` VARCHAR(20) NULL,
  `contents_note` VARCHAR(500) NULL,
  `pickup_slot_label` VARCHAR(120) NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `contact_email` VARCHAR(255) NOT NULL,
  `price_amount` INT NOT NULL,
  `eta_mins` INT NOT NULL DEFAULT 15,
  `status` VARCHAR(40) NOT NULL,
  `cancel_reason` VARCHAR(500) NULL,
  `assigned_at` TIMESTAMP NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`courier_order_id`),
  UNIQUE KEY `uk_courier_awb` (`awb_number`),
  UNIQUE KEY `uk_courier_qr` (`qr_token`),
  KEY `idx_courier_user` (`user_id`),
  KEY `idx_courier_driver` (`driver_id`),
  CONSTRAINT `fk_courier_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_courier_driver` FOREIGN KEY (`driver_id`) REFERENCES `drivers` (`driver_id`)
);

SET @db = DATABASE();
SET @alterColumn = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'gps_tracking' AND COLUMN_NAME = 'courier_order_id'
    ),
    'SELECT 1',
    'ALTER TABLE `gps_tracking` ADD COLUMN `courier_order_id` INT NULL AFTER `ride_id`'
  )
);
PREPARE s1 FROM @alterColumn;
EXECUTE s1;
DEALLOCATE PREPARE s1;

SET @alterFk = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
      WHERE CONSTRAINT_SCHEMA = @db AND TABLE_NAME = 'gps_tracking' AND CONSTRAINT_NAME = 'fk_gps_courier_order'
    ),
    'SELECT 1',
    'ALTER TABLE `gps_tracking` ADD CONSTRAINT `fk_gps_courier_order` FOREIGN KEY (`courier_order_id`) REFERENCES `courier_orders` (`courier_order_id`)'
  )
);
PREPARE s2 FROM @alterFk;
EXECUTE s2;
DEALLOCATE PREPARE s2;
