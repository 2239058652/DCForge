# 创建数据库
CREATE DATABASE dc_forge DEFAULT CHARACTER SET utf8mb4;

use dc_forge;

create table note
(
    id         bigint primary key auto_increment,
    content    varchar(2000) not null,
    created_at datetime      not null,
    updated_at datetime      not null
);