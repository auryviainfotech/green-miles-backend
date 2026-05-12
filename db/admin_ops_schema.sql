USE `greenmilesbooking`;

CREATE TABLE IF NOT EXISTS `route_plans` (
  `route_plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `route_code` VARCHAR(40) NOT NULL,
  `start_city_id` BIGINT UNSIGNED NOT NULL,
  `end_city_id` BIGINT UNSIGNED NOT NULL,
  `base_fare` INT NOT NULL,
  `default_seats` INT NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`route_plan_id`),
  UNIQUE KEY `uk_route_plan_code` (`route_code`),
  KEY `idx_route_plan_start_city` (`start_city_id`),
  KEY `idx_route_plan_end_city` (`end_city_id`),
  CONSTRAINT `fk_route_plan_start_city` FOREIGN KEY (`start_city_id`) REFERENCES `cities` (`city_id`),
  CONSTRAINT `fk_route_plan_end_city` FOREIGN KEY (`end_city_id`) REFERENCES `cities` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_logs` (
  `payment_log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `booking_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `ride_id` BIGINT UNSIGNED NOT NULL,
  `amount` INT NOT NULL,
  `status` VARCHAR(30) NOT NULL,
  `method` VARCHAR(30) NOT NULL,
  `reference_code` VARCHAR(80) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`payment_log_id`),
  UNIQUE KEY `uk_payment_reference_code` (`reference_code`),
  KEY `idx_payment_booking` (`booking_id`),
  KEY `idx_payment_user` (`user_id`),
  KEY `idx_payment_ride` (`ride_id`),
  CONSTRAINT `fk_payment_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`),
  CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_payment_ride` FOREIGN KEY (`ride_id`) REFERENCES `rides` (`ride_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `driver_profiles` (
  `driver_profile_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `driver_id` BIGINT UNSIGNED NOT NULL,
  `email` VARCHAR(255) NULL,
  `address` VARCHAR(500) NULL,
  `vid_proof_number` VARCHAR(120) NULL,
  PRIMARY KEY (`driver_profile_id`),
  UNIQUE KEY `uk_driver_profiles_driver` (`driver_id`),
  CONSTRAINT `fk_driver_profiles_driver` FOREIGN KEY (`driver_id`) REFERENCES `drivers` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
