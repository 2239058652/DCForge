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
