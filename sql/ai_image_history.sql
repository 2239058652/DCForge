CREATE TABLE IF NOT EXISTS `ai_image_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '生成类型：text2img / img2img',
    `prompt` VARCHAR(1000) NOT NULL COMMENT '用户输入的 prompt',
    `revised_prompt` VARCHAR(1000) DEFAULT NULL COMMENT 'AI 优化后的 prompt',
    `source_image_url` VARCHAR(2000) DEFAULT NULL COMMENT '原始图片URL（图生图输入）',
    `object_name` VARCHAR(500) NOT NULL COMMENT 'MinIO 对象名',
    `size` VARCHAR(20) DEFAULT NULL COMMENT '图片尺寸',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 生成图片历史记录';
