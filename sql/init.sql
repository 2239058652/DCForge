-- 创建数据库
CREATE DATABASE IF NOT EXISTS dc_forge
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE dc_forge;

DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS sys_user;

-- 笔记表
CREATE TABLE IF NOT EXISTS note
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '笔记ID',
    user_id    BIGINT        NULL COMMENT '用户ID',
    content    VARCHAR(2000) NOT NULL COMMENT '笔记内容',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_note_user_id (user_id),
    INDEX idx_note_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '笔记表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username   VARCHAR(50)  NOT NULL COMMENT '用户名，唯一',
    password   VARCHAR(255) NOT NULL COMMENT '密码',
    nickname   VARCHAR(50)  NULL COMMENT '昵称',
    avatar     VARCHAR(200) NULL COMMENT '头像地址',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER普通用户 ADMIN管理员',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_sys_user_username (username),
    INDEX idx_sys_user_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '用户表';