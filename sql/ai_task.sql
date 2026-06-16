CREATE TABLE IF NOT EXISTS `ai_task` (
    `id`              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `type`            VARCHAR(20) NOT NULL COMMENT '生成类型：text2img / img2img',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    `prompt`          VARCHAR(1000) NOT NULL COMMENT '用户输入的 prompt',
    `size`            VARCHAR(20) DEFAULT NULL COMMENT '图片尺寸',
    `images`          TEXT DEFAULT NULL COMMENT '图生图输入图片(JSON数组)',
    `object_name`     VARCHAR(500) DEFAULT NULL COMMENT 'MinIO 对象名',
    `revised_prompt`  VARCHAR(1000) DEFAULT NULL COMMENT 'AI 优化后的 prompt',
    `error_message`   VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `retry_count`     INT DEFAULT 0 COMMENT '重试次数',
    `created_at`      DATETIME NOT NULL COMMENT '创建时间',
    `updated_at`      DATETIME NOT NULL COMMENT '更新时间',

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 图像生成任务表';
