CREATE DATABASE IF NOT EXISTS dc_forge
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE dc_forge;

DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE IF NOT EXISTS sys_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(50)  NULL,
    avatar     VARCHAR(200) NULL,
    status     TINYINT      NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sys_user_username (username),
    INDEX idx_sys_user_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code  VARCHAR(50) NOT NULL,
    role_name  VARCHAR(50) NOT NULL,
    status     TINYINT     NOT NULL DEFAULT 1,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sys_role_code (role_code),
    INDEX idx_sys_role_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_permission
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(20)  NOT NULL DEFAULT 'API',
    path            VARCHAR(200) NULL,
    status          TINYINT      NOT NULL DEFAULT 1,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sys_permission_code (permission_code),
    INDEX idx_sys_permission_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    role_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    INDEX idx_sys_user_role_user_id (user_id),
    INDEX idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
    INDEX idx_sys_role_permission_role_id (role_id),
    INDEX idx_sys_role_permission_permission_id (permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS note
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT        NOT NULL,
    content    VARCHAR(2000) NOT NULL,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_note_user_id (user_id),
    INDEX idx_note_created_at (created_at),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO sys_role (role_code, role_name)
VALUES ('ADMIN', 'Administrator'),
       ('USER', 'User');

INSERT INTO sys_permission (permission_code, permission_name, resource_type, path)
VALUES ('user:list', 'List users', 'API', '/users/list'),
       ('note:list', 'List notes', 'API', '/notes/list,/notes/page'),
       ('note:detail', 'Find note detail', 'API', '/notes/find/{id}'),
       ('note:add', 'Add note', 'API', '/notes/add'),
       ('note:update', 'Update note', 'API', '/notes/update/{id}'),
       ('note:delete', 'Delete note', 'API', '/notes/delete/{id}'),
       ('role:list', 'List roles', 'API', NULL),
       ('role:add', 'Add role', 'API', NULL),
       ('role:update', 'Update role', 'API', NULL),
       ('role:delete', 'Delete role', 'API', NULL),
       ('role:assign-permission', 'Assign role permissions', 'API', '/rbac/role-permissions'),
       ('permission:list', 'List permissions', 'API', NULL),
       ('permission:add', 'Add permission', 'API', NULL),
       ('permission:update', 'Update permission', 'API', NULL),
       ('permission:delete', 'Delete permission', 'API', NULL),
       ('user:assign-role', 'Assign user roles', 'API', '/rbac/user-roles');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         INNER JOIN sys_permission p ON p.permission_code IN (
                                                              'note:list',
                                                              'note:detail',
                                                              'note:add',
                                                              'note:update',
                                                              'note:delete'
    )
WHERE r.role_code = 'USER';

-- 人员表
CREATE TABLE staff
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    type        TINYINT     NOT NULL COMMENT '0=doctor 1=nurse',
    rest_day    TINYINT     NOT NULL COMMENT '0=周日 1=周一 ... 6=周六',
    night_order INT         NOT NULL COMMENT '夜班队列序号，同type内唯一',
    is_active   TINYINT(1)  NOT NULL DEFAULT 1,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_night_order (type, night_order)
);

-- 夜班队列状态表（每种type各1条，共2条）
CREATE TABLE rota_state
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    type             TINYINT  NOT NULL COMMENT '0=doctor 1=nurse',
    current_staff_id BIGINT   NOT NULL COMMENT '下一个应该值夜班的人',
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type (type),
    FOREIGN KEY (current_staff_id) REFERENCES staff (id)
);

-- 排班表
CREATE TABLE schedule
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    staff_id   BIGINT     NOT NULL,
    shift_date DATE       NOT NULL,
    shift_type TINYINT    NOT NULL COMMENT '0=day 1=night 2=rest',
    is_swapped TINYINT(1) NOT NULL DEFAULT 0 COMMENT '夜班因冲突顺延',
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_staff_date (staff_id, shift_date),
    KEY idx_date (shift_date),
    FOREIGN KEY (staff_id) REFERENCES staff (id)
);

-- 接口权限映射表
CREATE TABLE interface_permission
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    http_method     VARCHAR(10)  NOT NULL COMMENT 'GET/POST/PUT/DELETE',
    url_pattern     VARCHAR(200) NOT NULL COMMENT '接口路径，支持 /roles/{id} 格式',
    permission_code VARCHAR(100) NOT NULL COMMENT '对应权限码，如 role:update',
    description     VARCHAR(200) COMMENT '描述',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_method_pattern (http_method, url_pattern)
) COMMENT '接口权限映射表';

-- 给admin账号插入权限 注意 id
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission;

-- 把排版模块的所有接口都加进无需权限
-- 人员管理 (StaffController)
INSERT INTO interface_permission (http_method, url_pattern, permission_code, description)
VALUES ('GET', '/api/staff', 'PERMIT_ALL', '获取所有人员'),
       ('POST', '/api/staff', 'PERMIT_ALL', '新增人员'),
       ('PUT', '/api/staff/{id}', 'PERMIT_ALL', '修改人员信息'),
       ('PUT', '/api/staff/{id}/deactivate', 'PERMIT_ALL', '停用人员'),
       ('PUT', '/api/staff/{id}/activate', 'PERMIT_ALL', '启用人员');

-- 排班管理 (ScheduleController)
INSERT INTO interface_permission (http_method, url_pattern, permission_code, description)
VALUES ('POST', '/api/schedule/generate', 'PERMIT_ALL', '生成排班'),
       ('GET', '/api/schedule', 'PERMIT_ALL', '获取月视图排班'),
       ('GET', '/api/schedule/staff/{id}', 'PERMIT_ALL', '获取某人排班'),
       ('DELETE', '/api/schedule', 'PERMIT_ALL', '删除某月排班');

-- 夜班队列状态 (RotaStateController)
INSERT INTO interface_permission (http_method, url_pattern, permission_code, description)
VALUES ('GET', '/api/rota-state', 'PERMIT_ALL', '查看夜班队列状态');

-- users 表新增字段，存 objectName 而不是 URL
ALTER TABLE sys_user
    ADD COLUMN avatar_object_name VARCHAR(200) DEFAULT NULL;

-- 排版人员增加头像
ALTER TABLE staff
    ADD COLUMN avatar_object_name VARCHAR(200) DEFAULT NULL;

-- note表的内容换成MEDIUMTEXT富文本格式存储
ALTER TABLE note
    MODIFY COLUMN content MEDIUMTEXT NOT NULL COMMENT '富文本内容（含Base64图片）';