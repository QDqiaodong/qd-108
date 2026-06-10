SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配方分类表';

CREATE TABLE IF NOT EXISTS `recipe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '配方标题',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图',
  `description` varchar(500) DEFAULT NULL COMMENT '简介',
  `ingredients` text COMMENT '食材配比（JSON格式）',
  `steps` text COMMENT '制作步骤（JSON格式）',
  `bake_temp` int DEFAULT NULL COMMENT '烘焙温度(℃)',
  `bake_time` int DEFAULT NULL COMMENT '烘焙时长(分钟)',
  `difficulty` tinyint DEFAULT 1 COMMENT '难度：1-简单 2-中等 3-困难',
  `servings` int DEFAULT NULL COMMENT '份量',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `user_id` bigint NOT NULL COMMENT '发布用户ID',
  `view_count` int DEFAULT 0 COMMENT '浏览量',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  `favorite_count` int DEFAULT 0 COMMENT '收藏数',
  `trial_receipt_count` int DEFAULT 0 COMMENT '试做回执数量',
  `status` tinyint DEFAULT 1 COMMENT '状态：0-草稿 1-已发布',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_view_count` (`view_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配方表';

CREATE TABLE IF NOT EXISTS `recipe_image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint NOT NULL COMMENT '配方ID',
  `image_url` varchar(255) NOT NULL COMMENT '图片地址',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配方图片表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `recipe_id` bigint NOT NULL COMMENT '配方ID',
  `folder_id` bigint DEFAULT NULL COMMENT '收藏夹ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe` (`user_id`,`recipe_id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `favorite_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) NOT NULL COMMENT '收藏夹名称',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏夹表';

CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint NOT NULL COMMENT '配方ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

INSERT INTO `category` (`name`, `sort_order`) VALUES
('面包', 1),
('蛋糕', 2),
('饼干', 3),
('甜点', 4),
('披萨', 5),
('中式面点', 6)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `user` (`username`, `password`, `nickname`, `bio`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '烘焙达人', '热爱烘焙，分享美食')
ON DUPLICATE KEY UPDATE `username` = VALUES(`username`);

CREATE TABLE IF NOT EXISTS `achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '成就名称',
  `description` varchar(200) DEFAULT NULL COMMENT '成就描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '成就图标',
  `type` varchar(30) NOT NULL COMMENT '成就类型：publish-发布, favorite-收藏, checkin-打卡, category-分类',
  `target` int DEFAULT 1 COMMENT '目标值',
  `category_id` bigint DEFAULT NULL COMMENT '关联分类ID（分类成就用）',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

CREATE TABLE IF NOT EXISTS `user_achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `achievement_id` bigint NOT NULL COMMENT '成就ID',
  `progress` int DEFAULT 0 COMMENT '当前进度',
  `unlocked` tinyint DEFAULT 0 COMMENT '是否解锁：0-未解锁 1-已解锁',
  `unlocked_at` datetime DEFAULT NULL COMMENT '解锁时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_achievement` (`user_id`,`achievement_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_achievement_id` (`achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

CREATE TABLE IF NOT EXISTS `check_in` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `check_date` date NOT NULL COMMENT '打卡日期',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`check_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡表';

CREATE TABLE IF NOT EXISTS `bake_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_date` date NOT NULL COMMENT '计划烘焙日期',
  `recipe_name` varchar(200) DEFAULT NULL COMMENT '计划烘焙的配方名称',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `reminder_enabled` tinyint DEFAULT 0 COMMENT '是否开启提醒：0-关闭 1-开启',
  `reminder_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`,`plan_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='烘焙计划表';

INSERT INTO `achievement` (`name`, `description`, `icon`, `type`, `target`, `sort_order`) VALUES
('首次发布', '发布第一个配方', '🎉', 'publish', 1, 1),
('小有成就', '发布5个配方', '📝', 'publish', 5, 2),
('烘焙达人', '发布20个配方', '👨‍🍳', 'publish', 20, 3),
('收藏新手', '收藏10个配方', '⭐', 'favorite', 10, 4),
('收藏达人', '收藏50个配方', '🏆', 'favorite', 50, 5),
('收藏大师', '收藏100个配方', '💎', 'favorite', 100, 6),
('初来乍到', '连续打卡1天', '📅', 'checkin', 1, 7),
('坚持不懈', '连续打卡7天', '🔥', 'checkin', 7, 8),
('打卡达人', '连续打卡30天', '👑', 'checkin', 30, 9)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

INSERT INTO `achievement` (`name`, `description`, `icon`, `type`, `target`, `category_id`, `sort_order`) VALUES
('面包学徒', '发布3个面包类配方', '🍞', 'category', 3, 1, 10),
('面包大师', '发布10个面包类配方', '🥖', 'category', 10, 1, 11),
('蛋糕学徒', '发布3个蛋糕类配方', '🧁', 'category', 3, 2, 12),
('蛋糕大师', '发布10个蛋糕类配方', '🎂', 'category', 10, 2, 13),
('饼干达人', '发布5个饼干类配方', '🍪', 'category', 5, 3, 14),
('甜点大师', '发布8个甜点类配方', '🍰', 'category', 8, 4, 15),
('披萨高手', '发布5个披萨类配方', '🍕', 'category', 5, 5, 16),
('中式面点师', '发布8个中式面点配方', '🥟', 'category', 8, 6, 17)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

CREATE TABLE IF NOT EXISTS `ingredient_alias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `canonical_name` varchar(100) NOT NULL COMMENT '规范名称（统一名称）',
  `alias_name` varchar(100) NOT NULL COMMENT '别名',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alias_name` (`alias_name`),
  KEY `idx_canonical_name` (`canonical_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食材别名表';

CREATE TABLE IF NOT EXISTS `recipe_step_progress` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `recipe_id` bigint NOT NULL COMMENT '配方ID',
  `completed_steps` text COMMENT '已完成的步骤索引（JSON数组）',
  `last_step_index` int DEFAULT 0 COMMENT '最后完成的步骤索引',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_recipe` (`user_id`,`recipe_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_recipe_id` (`recipe_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配方步骤进度表';

INSERT INTO `ingredient_alias` (`canonical_name`, `alias_name`) VALUES
('高筋面粉', '高筋粉'),
('高筋面粉', '高筋面粉'),
('高筋面粉', '高粉'),
('低筋面粉', '低筋粉'),
('低筋面粉', '低筋面粉'),
('低筋面粉', '低粉'),
('中筋面粉', '中筋粉'),
('中筋面粉', '中筋面粉'),
('中筋面粉', '中粉'),
('淡奶油', '稀奶油'),
('淡奶油', '淡奶油'),
('淡奶油', '鲜奶油'),
('无盐黄油', '黄油'),
('无盐黄油', '无盐黄油'),
('无盐黄油', '牛油'),
('细砂糖', '白砂糖'),
('细砂糖', '细砂糖'),
('细砂糖', '白糖'),
('糖粉', '糖粉'),
('糖粉', '糖霜'),
('鸡蛋', '鸡蛋'),
('鸡蛋', '全蛋'),
('鸡蛋清', '蛋清'),
('鸡蛋清', '鸡蛋清'),
('鸡蛋清', '蛋白'),
('鸡蛋黄', '蛋黄'),
('鸡蛋黄', '鸡蛋黄'),
('牛奶', '牛奶'),
('牛奶', '纯牛奶'),
('牛奶', '鲜奶'),
('酵母', '干酵母'),
('酵母', '酵母粉'),
('酵母', '即发酵母'),
('盐', '食盐'),
('盐', '细盐'),
('玉米油', '玉米油'),
('玉米油', '色拉油'),
('橄榄油', '橄榄油'),
('泡打粉', '泡打粉'),
('泡打粉', '发粉'),
('小苏打', '小苏打'),
('小苏打', '碳酸氢钠'),
('可可粉', '可可粉'),
('抹茶粉', '抹茶粉'),
('巧克力', '巧克力'),
('巧克力', '黑巧克力'),
('奶油奶酪', '奶油奶酪'),
('奶油奶酪', '奶油芝士'),
('奶油奶酪', '芝士'),
('马斯卡彭奶酪', '马斯卡彭'),
('马斯卡彭奶酪', '马斯卡彭奶酪'),
('吉利丁片', '吉利丁片'),
('吉利丁片', '鱼胶片'),
('吉利丁片', '明胶片'),
('吉利丁粉', '吉利丁粉'),
('吉利丁粉', '鱼胶粉'),
('吉利丁粉', '明胶粉'),
('蔓越莓干', '蔓越莓'),
('蔓越莓干', '蔓越莓干'),
('葡萄干', '葡萄干'),
('蓝莓干', '蓝莓干'),
('杏仁粉', '杏仁粉'),
('杏仁片', '杏仁片'),
('核桃', '核桃'),
('核桃', '核桃仁'),
('腰果', '腰果'),
('白芝麻', '白芝麻'),
('黑芝麻', '黑芝麻'),
('花生', '花生'),
('花生', '花生米'),
('椰蓉', '椰蓉'),
('椰丝', '椰丝'),
('蜂蜜', '蜂蜜'),
('枫糖浆', '枫糖浆'),
('香草精', '香草精'),
('香草精', '香草荚'),
('柠檬汁', '柠檬汁'),
('柠檬皮', '柠檬皮屑'),
('奶油', '奶油'),
('奶油', '动物奶油'),
('炼乳', '炼乳'),
('芝士片', '芝士片'),
('芝士片', '奶酪片'),
('马苏里拉奶酪', '马苏里拉'),
('马苏里拉奶酪', '马苏里拉芝士'),
('马苏里拉奶酪', '马苏里拉奶酪')
ON DUPLICATE KEY UPDATE `canonical_name` = VALUES(`canonical_name`);

CREATE TABLE IF NOT EXISTS `trial_receipt` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint NOT NULL COMMENT '配方ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `success` tinyint NOT NULL DEFAULT 1 COMMENT '是否成功：0-失败 1-成功',
  `taste_rating` tinyint DEFAULT NULL COMMENT '口感评分：1-5星',
  `taste_comment` varchar(500) DEFAULT NULL COMMENT '口感评价',
  `temp_adjustment` varchar(500) DEFAULT NULL COMMENT '温度调整',
  `mold_difference` varchar(500) DEFAULT NULL COMMENT '模具差异',
  `notes` varchar(1000) DEFAULT NULL COMMENT '其他备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recipe_id` (`recipe_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试做回执表';

SET FOREIGN_KEY_CHECKS = 1;
