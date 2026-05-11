# DCForge 后续开发需求指导文档

> 项目定位：一个用于练习 Java 后端开发的 Spring Boot 项目。当前阶段目标不是追求复杂功能堆叠，而是通过一个完整但可控的练习项目，逐步掌握 Spring Boot、MyBatis、MySQL、Redis、Spring Security、JWT、Swagger、参数校验、异常处理、分层设计等后端核心能力。

---

## 1. 当前项目状态

### 1.1 已完成能力

当前项目已经完成了第一阶段的核心基础能力：

1. Spring Boot 项目已能正常启动。
2. MySQL 已接入。
3. Redis 已接入，但暂未深度使用。
4. Swagger 已接入，并已处理 Spring Security 对 Swagger 的放行问题。
5. MyBatis 已接入，支持 XML 与注解混合使用。
6. 已完成 `note` 模块的基础 CRUD。
7. 已完成分页查询。
8. 已完成基于 `content` 的分页模糊查询。
9. 已完成统一返回结构 `Result<T>`。
10. 已完成统一分页返回结构 `PageResult<T>`。
11. 已完成业务异常 `BusinessException`。
12. 已完成全局异常处理 `GlobalExceptionHandler`。
13. 已完成参数校验失败的统一处理。
14. 已初步形成 DTO、Entity、VO 分层意识。

### 1.2 当前已形成的项目基础风格

当前项目已经具备这些基础约定：

| 层级 | 作用 |
|---|---|
| Controller | 接收请求，调用 Service，返回 Result |
| Service | 处理业务逻辑，做数据转换，判断业务失败 |
| Mapper | 操作数据库，执行 SQL |
| Entity | 对应数据库数据结构 |
| DTO | 接收前端请求参数 |
| VO | 返回给前端的数据结构 |
| Result | 统一接口响应 |
| PageResult | 统一分页响应 |
| GlobalExceptionHandler | 统一异常返回 |

这些约定后面要继续保持，不要一会儿直接返回 Entity，一会儿返回 VO，一会儿 Controller 里处理业务判断。

---

## 2. 项目总体发展路线

本项目建议分为五个阶段推进。

### 阶段一：基础接口与工程骨架

目标：掌握一个接口从请求进入到响应返回的完整链路。

当前基本已完成。

包含能力：

1. note 增删改查。
2. 分页查询。
3. 模糊查询。
4. 参数校验。
5. 统一返回。
6. 全局异常。
7. MyBatis XML 与注解使用。
8. Swagger 接口调试。

### 阶段二：用户模块与密码加密

目标：建立用户体系，为登录认证做准备。

需要完成：

1. 用户表 `sys_user`。
2. 用户注册接口。
3. 用户登录接口。
4. 用户名唯一校验。
5. 密码加密存储。
6. 密码登录比对。
7. 用户状态校验。

### 阶段三：Spring Security + JWT

目标：让系统具备真正的登录态和接口保护能力。

需要完成：

1. 登录成功生成 JWT。
2. 请求携带 Token。
3. JWT 解析用户信息。
4. 自定义认证过滤器。
5. Security 放行登录、注册、Swagger。
6. 保护 note 接口。
7. 当前登录用户与 note 关联。
8. 用户只能操作自己的 note。

### 阶段四：Redis 实战接入

目标：让 Redis 不只是“连上”，而是真的服务于业务。

可以选择实现：

1. 登录 Token 黑名单。
2. 验证码缓存。
3. 用户信息缓存。
4. 热点 note 缓存。
5. 简单限流。

初学阶段推荐先做：

1. 登录验证码缓存。
2. Token 黑名单。

### 阶段五：项目完善与工程化

目标：让项目逐渐接近真实后端项目。

可逐步补充：

1. 多环境配置。
2. 日志规范。
3. 接口权限控制。
4. 操作人字段。
5. 软删除。
6. 数据库索引优化。
7. 接口文档补全。
8. 单元测试。
9. Docker 本地环境。
10. 代码重构与模块拆分。

---

## 3. 当前下一步主线：用户模块

当前最合理的下一步不是继续扩展 note 模块，而是开始用户模块。

原因：

1. note 模块已经完成基础 CRUD 与分页查询。
2. 继续增加 note 的边角功能，学习收益开始下降。
3. 用户模块是 Security、JWT、权限、数据归属的前置。
4. 后续 note 的 `user_id` 字段需要真正接入当前登录用户。

---

## 4. 数据库设计指导

### 4.1 当前推荐 SQL

当前建议保留两张表：

1. `sys_user`
2. `note`

建议 SQL：

```sql
CREATE DATABASE IF NOT EXISTS dc_forge
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE dc_forge;

DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE IF NOT EXISTS sys_user
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username   VARCHAR(50)  NOT NULL COMMENT '用户名，唯一',
    password   VARCHAR(255) NOT NULL COMMENT '密码，BCrypt加密后存储',
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

CREATE TABLE IF NOT EXISTS note
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '笔记ID',
    user_id    BIGINT        NULL COMMENT '用户ID，后续接入登录后改为 NOT NULL',
    content    VARCHAR(2000) NOT NULL COMMENT '笔记内容',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_note_user_id (user_id),
    INDEX idx_note_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '笔记表';
```

### 4.2 为什么 `sys_user.password` 建议用 `VARCHAR(255)`

BCrypt 加密后的密码通常长度约 60 个字符，`VARCHAR(100)` 也够，但 `VARCHAR(255)` 更稳。

后续如果更换加密算法、增加前缀、兼容 Spring Security 密码格式，空间更充足。

### 4.3 为什么当前不加外键

当前阶段不建议马上给 `note.user_id` 加外键。

原因：

1. 新手阶段调试更方便。
2. 删除数据和重建表更简单。
3. 先把业务逻辑跑通更重要。
4. 后续理解外键约束后再加更合适。

后续可以再做：

```sql
ALTER TABLE note
ADD CONSTRAINT fk_note_user
FOREIGN KEY (user_id) REFERENCES sys_user(id);
```

但不是当前重点。

---

## 5. 用户模块第一版需求

### 5.1 用户注册

#### 5.1.1 接口说明

接口：

```http
POST /users/register
```

请求体：

```json
{
  "username": "dc",
  "password": "User@123",
  "nickname": "dc"
}
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

#### 5.1.2 业务规则

1. `username` 必填。
2. `password` 必填。
3. `username` 不能重复。
4. 密码不能明文入库。
5. 注册时默认 `status = 1`。
6. 注册时默认 `role = USER`。
7. `createdAt` 和 `updatedAt` 由后端设置，或由数据库默认值处理。

#### 5.1.3 需要创建的类

建议结构：

```text
com.forge.dc.user
├─ controller
│  └─ UserController.java
├─ service
│  ├─ UserService.java
│  └─ impl/UserServiceImpl.java
├─ mapper
│  └─ UserMapper.java
├─ entity
│  └─ SysUserEntity.java
├─ dto
│  └─ UserRegisterDto.java
└─ vo
   └─ UserInfoVo.java
```

#### 5.1.4 DTO 设计

`UserRegisterDto` 建议字段：

```java
private String username;
private String password;
private String nickname;
```

校验建议：

1. `username` 使用 `@NotBlank`。
2. `password` 使用 `@NotBlank`。
3. 可以先不做复杂密码规则。
4. 后续再加 `@Size(min = 6, max = 20)`。

#### 5.1.5 Mapper 方法

第一版建议包含：

```java
SysUserEntity findByUsername(String username);
int insertUser(SysUserEntity user);
```

其中：

1. `findByUsername` 用于注册前查重。
2. `insertUser` 用于新增用户。

#### 5.1.6 Service 逻辑

注册流程：

```text
1. 接收 UserRegisterDto
2. 根据 username 查询用户是否存在
3. 如果存在，抛 BusinessException
4. 使用 BCryptPasswordEncoder 加密密码
5. 创建 SysUserEntity
6. 设置 username、password、nickname、role、status、时间
7. 调用 mapper 插入用户
8. 判断影响行数
9. 成功返回
```

#### 5.1.7 错误场景

| 场景 | 返回建议 |
|---|---|
| username 为空 | 400，用户名不能为空 |
| password 为空 | 400，密码不能为空 |
| 用户名已存在 | 400，用户名已存在 |
| 数据库插入失败 | 500，注册失败 |

---

## 6. 密码加密设计

### 6.1 为什么必须加密密码

用户密码不能明文存储。

如果数据库泄露，明文密码会直接暴露用户账户风险。

因此注册时必须：

```text
前端传入明文密码
↓
后端使用 BCrypt 加密
↓
数据库只保存密文
```

登录时不是解密密码，而是：

```text
用户输入明文密码
↓
拿数据库里的密文
↓
使用 matches 进行比对
```

### 6.2 需要配置的 Bean

后续应增加：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

放置位置建议：

```text
com.forge.dc.config.security.SecurityConfig
```

或者单独建：

```text
com.forge.dc.config.password.PasswordConfig
```

### 6.3 使用方式

注册时：

```java
String encodedPassword = passwordEncoder.encode(rawPassword);
```

登录时：

```java
passwordEncoder.matches(rawPassword, encodedPassword);
```

---

## 7. 用户登录需求

### 7.1 接口说明

接口：

```http
POST /users/login
```

请求体：

```json
{
  "username": "dc",
  "password": "User@123"
}
```

第一阶段返回可以先简单一点：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "dc",
    "nickname": "dc",
    "role": "USER"
  }
}
```

第二阶段接入 JWT 后再返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "xxx.yyy.zzz",
    "userInfo": {
      "id": 1,
      "username": "dc",
      "nickname": "dc",
      "role": "USER"
    }
  }
}
```

### 7.2 第一版登录规则

1. 用户名必填。
2. 密码必填。
3. 用户不存在时返回登录失败。
4. 密码不匹配时返回登录失败。
5. 用户状态为禁用时不允许登录。
6. 登录成功后返回用户基本信息。

### 7.3 登录流程

```text
1. 接收 UserLoginDto
2. 根据 username 查询用户
3. 用户不存在，抛 BusinessException
4. 判断 status 是否为启用
5. 使用 passwordEncoder.matches 比对密码
6. 密码错误，抛 BusinessException
7. 返回 UserLoginVo
```

---

## 8. JWT 认证阶段设计

### 8.1 JWT 什么时候做

不要在用户注册前做 JWT。

正确顺序：

```text
先做用户表
↓
再做注册
↓
再做密码加密
↓
再做登录
↓
最后做 JWT
```

### 8.2 JWT 要解决什么问题

登录成功后，后端要告诉前端：

```text
你是谁
你登录成功了
后续请求怎么证明你还是你
```

JWT 就是这个证明。

### 8.3 JWT 登录流程

```text
1. 用户调用登录接口
2. 后端校验用户名密码
3. 校验成功后生成 token
4. 前端保存 token
5. 后续请求在 Header 中携带 token
6. 后端过滤器解析 token
7. 解析成功后放行请求
8. 解析失败返回 401
```

### 8.4 请求头格式

后续建议使用：

```http
Authorization: Bearer token内容
```

Swagger 小锁也应该使用 Bearer Token 配置。

---

## 9. Spring Security 后续改造目标

### 9.1 当前 Security 状态

当前阶段主要是：

1. 放行 Swagger。
2. 放行 note 接口，便于调试。
3. 关闭 CSRF。
4. 初步理解 SecurityFilterChain。

### 9.2 后续目标

当 JWT 接入后，应改成：

#### 放行接口

```text
/users/register
/users/login
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
```

#### 保护接口

```text
/notes/**
```

### 9.3 后续请求逻辑

```text
请求进入
↓
JWT 过滤器检查 Authorization
↓
没有 token，访问受保护接口则 401
↓
有 token，解析用户信息
↓
放入 SecurityContext
↓
Controller 处理请求
```

---

## 10. note 模块后续与用户关联

### 10.1 当前状态

当前 note 表已有：

```sql
user_id BIGINT NULL
```

但当前 note 接口还没有真正关联用户。

### 10.2 接入登录后的目标

新增 note 时，不应该由前端传 `userId`。

应该由后端从 token 中获取当前用户 ID：

```text
当前登录用户 id
↓
写入 note.user_id
```

### 10.3 查询 note 时的目标

用户只能看到自己的 note。

原来的 SQL：

```sql
select * from note order by id desc
```

后续要变成：

```sql
select * from note where user_id = 当前登录用户id order by id desc
```

分页、详情、修改、删除都应加用户隔离。

### 10.4 为什么要做用户隔离

如果不做用户隔离，用户 A 可能通过接口访问或修改用户 B 的 note。

例如：

```http
DELETE /notes/delete/10
```

如果 id 为 10 的 note 属于另一个用户，也可能被删除。

所以后续删除应变成：

```sql
delete from note where id = ? and user_id = ?
```

修改也应变成：

```sql
update note set content = ?, updated_at = ? where id = ? and user_id = ?
```

---

## 11. Redis 后续使用建议

Redis 先不要急着乱用。

建议按业务需要引入。

### 11.1 推荐第一个 Redis 练习：验证码

可以做一个简单验证码登录前置练习。

流程：

```text
1. 用户请求验证码
2. 后端生成随机验证码
3. 验证码存入 Redis，有效期 5 分钟
4. 用户注册或登录时提交验证码
5. 后端从 Redis 读取并比对
6. 比对成功后删除验证码
```

### 11.2 推荐第二个 Redis 练习：Token 黑名单

JWT 本身是无状态的，签发后在过期前通常一直有效。

如果要做退出登录，可以：

```text
1. 用户点击退出
2. 后端把当前 token 加入 Redis 黑名单
3. Redis 过期时间设置为 token 剩余有效期
4. 后续请求如果 token 在黑名单中，则拒绝访问
```

### 11.3 不建议现在做的 Redis 功能

先不要做：

1. 复杂缓存一致性。
2. 分布式锁。
3. 高并发限流。
4. 消息队列替代。

这些以后再学。

---

## 12. 接口路径规范建议

当前 note 接口类似：

```text
GET    /notes/list
GET    /notes/page
GET    /notes/find/{id}
POST   /notes/add
PUT    /notes/update/{id}
DELETE /notes/delete/{id}
```

这能用，但后续可以逐渐改成更 REST 风格：

```text
GET    /notes          分页查询
GET    /notes/{id}     查询详情
POST   /notes          新增
PUT    /notes/{id}     修改
DELETE /notes/{id}     删除
```

对新手来说，当前路径可以先保留。

等你更熟悉后，再统一 REST 风格。

---

## 13. 当前代码重构建议

### 13.1 抽 Entity 转 VO 方法

当前 `NoteEntity -> NoteListVo` 转换应抽成私有方法：

```java
private NoteListVo toNoteListVo(NoteEntity noteEntity)
```

好处：

1. 列表查询复用。
2. 详情查询复用。
3. 减少重复代码。
4. 后续字段增加时只改一处。

### 13.2 统一错误码

`ResultCode` 建议增加：

```java
NOT_FOUND(404, "not found"),
USERNAME_EXISTS(400, "username already exists"),
LOGIN_FAILED(400, "username or password error"),
USER_DISABLED(403, "user disabled")
```

但不要一口气加太多。

当前至少建议加：

```java
NOT_FOUND(404, "not found")
```

用于：

1. note 不存在。
2. user 不存在。

### 13.3 DTO 命名区分

建议保持：

```text
NoteAddDto      新增 note
NoteUpdateDto   修改 note
NotePageDto     分页查询 note
UserRegisterDto 用户注册
UserLoginDto    用户登录
```

不要用一个 DTO 同时承担多个用途。

---

## 14. 后续测试路线

每完成一个接口，都至少测三类情况。

### 14.1 正常情况

例如新增 note：

```json
{
  "content": "测试内容"
}
```

应返回成功。

### 14.2 参数非法

例如：

```json
{
  "content": ""
}
```

应返回参数校验错误。

### 14.3 业务异常

例如删除不存在的 note：

```http
DELETE /notes/delete/999999
```

应返回：

```json
{
  "code": 404,
  "message": "note不存在",
  "data": null
}
```

### 14.4 系统异常

例如故意写错 SQL 表名，观察是否进入全局异常处理。

注意：系统异常只在开发期测试，不要长期保留错误 SQL。

---

## 15. 学习重点路线

### 15.1 当前最应该掌握

1. Controller、Service、Mapper 分层。
2. DTO、Entity、VO 区别。
3. MyBatis XML 映射。
4. 动态 SQL。
5. 影响行数判断业务成功失败。
6. 全局异常处理。
7. 参数校验。
8. Swagger 调试。

### 15.2 接下来要掌握

1. 用户注册。
2. BCrypt 密码加密。
3. 用户登录。
4. Spring Security 基础认证流程。
5. JWT 生成与校验。
6. Redis 基础业务使用。

### 15.3 不建议现在深入的内容

1. Spring Cloud。
2. OAuth2。
3. 复杂 RBAC 权限模型。
4. 微服务。
5. 分布式事务。
6. 高并发缓存一致性。
7. 消息队列。

这些会让学习路线变散。

---

## 16. 下一步具体任务清单

### 任务 1：整理 note 模块

完成以下小重构：

1. 抽 `toNoteListVo`。
2. 增加 `NOT_FOUND` 错误码。
3. 把 note 不存在的异常统一改成 `NOT_FOUND`。
4. 确认 `NoteAddDto` 和 `NoteUpdateDto` 分开。
5. 确认分页模糊查询 `total` 和 `records` 使用相同条件。

### 任务 2：创建 user 模块基础结构

创建包：

```text
com.forge.dc.user.controller
com.forge.dc.user.service
com.forge.dc.user.service.impl
com.forge.dc.user.mapper
com.forge.dc.user.entity
com.forge.dc.user.dto
com.forge.dc.user.vo
```

### 任务 3：编写用户实体

创建：

```text
SysUserEntity
```

字段对应：

```text
id
username
password
nickname
avatar
status
role
createdAt
updatedAt
```

### 任务 4：编写注册 DTO

创建：

```text
UserRegisterDto
```

字段：

```text
username
password
nickname
```

校验：

```text
username 必填
password 必填
```

### 任务 5：编写 UserMapper

先实现：

```text
findByUsername
insertUser
```

### 任务 6：接入 PasswordEncoder

配置：

```text
BCryptPasswordEncoder
```

注册时使用加密密码入库。

### 任务 7：编写注册接口

接口：

```http
POST /users/register
```

完成用户名查重和密码加密。

### 任务 8：编写登录接口

接口：

```http
POST /users/login
```

先返回用户信息，不急着返回 JWT。

### 任务 9：接入 JWT

完成：

1. JWT 工具类。
2. 登录成功生成 token。
3. Swagger 小锁调试。
4. Security JWT 过滤器。
5. 保护 note 接口。

### 任务 10：note 绑定用户

完成：

1. 新增 note 时写入当前用户 id。
2. 查询 note 时只查当前用户。
3. 修改、删除、详情都校验 user_id。

---

## 17. 推荐完成顺序

严格建议按以下顺序做：

```text
1. 整理 note 模块
2. 创建 user 表和 user 包结构
3. 做用户注册
4. 接入密码加密
5. 做用户登录
6. 登录成功返回用户信息
7. 接 JWT
8. Security 保护接口
9. note 绑定当前用户
10. Redis 做验证码或 token 黑名单
```

不要跳着做 JWT。

---

## 18. 当前阶段验收标准

当你完成用户注册和登录前，先确保 note 模块满足：

1. 新增 note 正常。
2. 删除 note 正常。
3. 删除不存在 note 返回业务错误。
4. 修改 note 正常。
5. 修改不存在 note 返回业务错误。
6. 查询详情正常。
7. 查询不存在详情返回业务错误。
8. 分页正常。
9. 模糊分页正常。
10. 参数校验异常返回统一结构。
11. 系统异常返回统一结构。

当你完成 user 模块第一版后，应满足：

1. 用户可以注册。
2. 用户名重复不能注册。
3. 密码入库不是明文。
4. 用户可以登录。
5. 密码错误不能登录。
6. 禁用用户不能登录。
7. 登录返回统一结构。

---

## 19. 最重要的学习原则

### 19.1 不追求一次写完

每次只做一个小目标。

例如：

```text
今天只做注册
明天只做登录
后天只做 JWT
```

不要同时做注册、登录、JWT、权限、Redis。

### 19.2 每一步都先跑通

每新增一个方法，都要测试。

不要连续写很多代码后再启动项目。

### 19.3 先理解业务语义，再写代码

例如删除：

```text
rows = 0 是数据不存在，不是数据库异常
SQL 报错才是系统异常
```

这种理解比背代码更重要。

### 19.4 优先掌握调试

后端想像前端一样看对象，重点靠：

1. Debug 断点。
2. 日志输出。
3. Swagger 返回 JSON。
4. 数据库表数据。

---

## 20. 总结

接下来主线非常清楚：

```text
note 模块已经完成基础训练
↓
开始 user 模块
↓
完成注册和登录
↓
接入密码加密
↓
接入 JWT
↓
用 Security 保护 note 接口
↓
note 数据绑定当前用户
↓
再引入 Redis 做真实业务
```

当前不要急着扩展太多 note 功能，也不要直接跳复杂权限系统。

最合适的下一步是：

```text
整理 note 模块后，开始用户注册。
```
