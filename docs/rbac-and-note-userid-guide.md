# Note UserId Binding and RBAC Review Guide

这份文档用于回顾从 `note` 绑定当前登录用户 `userId`，到完整 RBAC 权限体系落地的整体改动。阅读目标不是背代码，而是理解请求从 Controller 进入后，如何经过登录认证、用户身份识别、权限加载、权限校验，最终落到业务 SQL。

## 1. 改造前的问题

改造前主要有两个问题：

1. `note` 没有真正归属于某个用户。

   虽然数据库表里有 `note.user_id`，但代码新增 note 时没有写入 `user_id`，查询、修改、删除也没有按当前用户过滤。这会导致所有用户看到的是同一批 note。

2. 权限模型只是单字段角色。

   原来 `sys_user` 里有一个 `role` 字段，登录后 JWT 里也放了一个 `role`。这种做法只能支持简单的 `ADMIN/USER` 判断，不是真正的“账号-角色-权限” RBAC。

完整 RBAC 的目标是：

```text
用户 sys_user
  -> 用户角色关系 sys_user_role
    -> 角色 sys_role
      -> 角色权限关系 sys_role_permission
        -> 权限 sys_permission
```

## 2. Note 绑定 UserId

### 2.1 实体增加 userId

文件：

```text
src/main/java/com/forge/dc/note/entity/NoteEntity.java
```

新增字段：

```java
private Long userId;
```

它对应数据库里的：

```sql
note.user_id
```

这个字段是 note 归属用户的核心字段。

### 2.2 Service 从登录上下文获取当前用户

文件：

```text
src/main/java/com/forge/dc/note/service/impl/NoteServiceImpl.java
```

新增了类似这样的逻辑：

```java
private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
        throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "unauthorized");
    }
    return loginUser.getUserId();
}
```

这表示 note 模块不再信任前端传来的 userId，而是从 Spring Security 当前认证用户中取 `userId`。

这是关键点：

```text
前端不能决定 note 属于谁
后端根据 token 解析出的当前用户决定 note 属于谁
```

### 2.3 新增 note 时写入 userId

新增 note 时：

```java
NoteEntity noteEntity = new NoteEntity();
noteEntity.setUserId(getCurrentUserId());
noteEntity.setContent(noteAddDto.getContent());
```

对应 SQL：

```sql
insert into note (
    user_id,
    content,
    created_at,
    updated_at
)
values (
    #{userId},
    #{content},
    #{createdAt},
    #{updatedAt}
)
```

这样每条 note 都会绑定当前登录用户。

### 2.4 查询、详情、修改、删除都加 userId 条件

Mapper 方法从原来的只传 `id`，改成同时传 `id` 和 `userId`：

```java
int deleteNoteById(@Param("id") Long id, @Param("userId") Long userId);

NoteEntity getNoteById(@Param("id") Long id, @Param("userId") Long userId);
```

SQL 变成：

```sql
where id = #{id}
and user_id = #{userId}
```

这带来一个重要效果：

```text
即使用户猜到了别人的 note id，也查不到、改不了、删不了。
```

分页和列表也一样：

```sql
where user_id = #{userId}
```

所以 note 模块现在是用户隔离的。

## 3. RBAC 数据库结构

文件：

```text
sql/init.sql
```

RBAC 相关表有五张。

### 3.1 sys_user

账号表，只保存账号本身：

```text
id
username
password
nickname
avatar
status
created_at
updated_at
```

注意：`sys_user` 不再依赖单个 `role` 字段做权限判断。

### 3.2 sys_role

角色表：

```text
id
role_code
role_name
status
created_at
updated_at
```

示例：

```text
ADMIN
USER
```

### 3.3 sys_permission

权限表：

```text
id
permission_code
permission_name
resource_type
path
status
created_at
updated_at
```

示例权限码：

```text
user:list
note:list
note:detail
note:add
note:update
note:delete
role:list
role:add
role:update
role:delete
permission:list
permission:add
permission:update
permission:delete
user:assign-role
role:assign-permission
```

权限码是最终用于接口鉴权的东西。

### 3.4 sys_user_role

用户和角色的关系表。

一个用户可以有多个角色：

```text
user_id -> role_id
```

### 3.5 sys_role_permission

角色和权限的关系表。

一个角色可以有多个权限：

```text
role_id -> permission_id
```

最终权限链路是：

```text
用户 id
  -> 查 sys_user_role 得到 role_id
  -> 查 sys_role 得到 role_code
  -> 查 sys_role_permission 得到 permission_id
  -> 查 sys_permission 得到 permission_code
```

## 4. RBAC 相关实体

新增实体：

```text
src/main/java/com/forge/dc/users/entity/SysRoleEntity.java
src/main/java/com/forge/dc/users/entity/SysPermissionEntity.java
src/main/java/com/forge/dc/users/entity/SysUserRoleEntity.java
src/main/java/com/forge/dc/users/entity/SysRolePermissionEntity.java
```

它们分别对应：

```text
sys_role
sys_permission
sys_user_role
sys_role_permission
```

这些实体主要用于 Mapper 查询结果承载。

## 5. 登录流程变化

### 5.1 UserServiceImpl.login

文件：

```text
src/main/java/com/forge/dc/users/service/impl/UserServiceImpl.java
```

登录流程现在是：

```text
1. 根据 username 查用户
2. 校验用户是否存在
3. 校验密码
4. 校验用户状态
5. 根据 userId 查角色列表
6. 根据 userId 查权限列表
7. 生成 token
8. 返回 token + roles + permissions
```

核心代码逻辑：

```java
List<String> roles = userMapper.findRoleCodesByUserId(user.getId());
List<String> permissions = userMapper.findPermissionCodesByUserId(user.getId());
String token = jwtUtils.generateToken(user.getId(), user.getUsername());
```

返回对象：

```text
UserLoginVO
```

里面现在包含：

```text
token
id
username
nickname
avatar
roles
permissions
```

### 5.2 JWT 不再保存 role

文件：

```text
src/main/java/com/forge/dc/common/util/JwtUtils.java
```

原来 token 里保存：

```text
userId
username
role
```

现在只保存：

```text
userId
username
```

原因是：角色和权限可能变化。如果 token 里保存完整权限，权限变更后旧 token 仍然带旧权限，不利于权限即时生效。

现在的设计是：

```text
token 负责证明“你是谁”
数据库负责告诉系统“你现在有什么权限”
```

## 6. 请求鉴权流程

文件：

```text
src/main/java/com/forge/dc/security/JwtAuthenticationFilter.java
```

每次请求时：

```text
1. 读取 Authorization 请求头
2. 检查 Bearer token
3. 校验 token 是否有效
4. 解析 userId 和 username
5. 根据 userId 查询角色列表
6. 根据 userId 查询权限列表
7. 构造 LoginUser
8. 放入 SecurityContextHolder
```

核心逻辑：

```java
List<String> roles = userMapper.findRoleCodesByUserId(userId);
List<String> permissions = userMapper.findPermissionCodesByUserId(userId);
LoginUser loginUser = new LoginUser(userId, username, roles, permissions);
```

这样 Controller 里的 `@PreAuthorize` 才能判断当前用户有没有权限。

## 7. LoginUser 的变化

文件：

```text
src/main/java/com/forge/dc/security/LoginUser.java
```

现在 `LoginUser` 保存：

```text
userId
username
roles
permissions
```

并转换成 Spring Security 能识别的 `GrantedAuthority`：

```java
roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
```

这意味着系统同时支持：

```java
hasRole('ADMIN')
```

和：

```java
hasAuthority('note:add')
```

当前项目主要使用 `hasAuthority(...)`，也就是按权限码控制接口。

## 8. Mapper 权限查询

文件：

```text
src/main/resources/mapper/users/UserMapper.xml
```

查询用户角色：

```sql
select distinct r.role_code
from sys_user_role ur
         inner join sys_role r on r.id = ur.role_id
where ur.user_id = #{userId}
  and r.status = 1
order by r.id
```

查询用户权限：

```sql
select distinct p.permission_code
from sys_user_role ur
         inner join sys_role r on r.id = ur.role_id
         inner join sys_role_permission rp on rp.role_id = r.id
         inner join sys_permission p on p.id = rp.permission_id
where ur.user_id = #{userId}
  and r.status = 1
  and p.status = 1
order by p.id
```

这是 RBAC 的核心 SQL。

## 9. 注册默认绑定 USER 角色

文件：

```text
src/main/java/com/forge/dc/users/service/impl/UserServiceImpl.java
```

注册用户后，会绑定默认角色：

```java
private static final String DEFAULT_ROLE_CODE = "USER";
```

流程：

```text
1. 新增 sys_user
2. 查询 USER 角色 id
3. 插入 sys_user_role
```

对应方法：

```java
bindDefaultRole(sysUserEntity.getId());
```

所以通过 `/users/register` 注册出来的账号默认拥有 `USER` 角色。

`USER` 角色在 SQL 里被赋予 note 相关权限：

```text
note:list
note:detail
note:add
note:update
note:delete
```

## 10. Controller 权限控制

### 10.1 NoteController

文件：

```text
src/main/java/com/forge/dc/note/controller/NoteController.java
```

接口权限：

```text
GET    /notes/list         note:list
GET    /notes/page         note:list
GET    /notes/find/{id}    note:detail
POST   /notes/add          note:add
PUT    /notes/update/{id}  note:update
DELETE /notes/delete/{id}  note:delete
```

示例：

```java
@PreAuthorize("hasAuthority('note:add')")
```

意思是：当前登录用户必须拥有 `note:add` 权限才能访问。

### 10.2 UserController

文件：

```text
src/main/java/com/forge/dc/users/controller/UserController.java
```

用户列表接口：

```text
GET /users/list -> user:list
```

普通 `USER` 默认没有 `user:list`，所以不能访问用户列表。

### 10.3 RbacController

文件：

```text
src/main/java/com/forge/dc/users/controller/RbacController.java
```

新增了 RBAC 管理接口：

```text
GET    /rbac/roles                role:list
POST   /rbac/roles                role:add
PUT    /rbac/roles/{id}           role:update
DELETE /rbac/roles/{id}           role:delete

GET    /rbac/permissions          permission:list
POST   /rbac/permissions          permission:add
PUT    /rbac/permissions/{id}     permission:update
DELETE /rbac/permissions/{id}     permission:delete

PUT    /rbac/user-roles           user:assign-role
PUT    /rbac/role-permissions     role:assign-permission
```

这些接口用于管理角色、权限、用户角色关系、角色权限关系。

## 11. RBAC 管理层代码

新增文件：

```text
src/main/java/com/forge/dc/users/service/RbacService.java
src/main/java/com/forge/dc/users/service/impl/RbacServiceImpl.java
src/main/java/com/forge/dc/users/mapper/RbacMapper.java
src/main/resources/mapper/users/RbacMapper.xml
```

它们负责：

```text
角色列表、新增、修改、删除
权限列表、新增、修改、删除
给用户分配角色
给角色分配权限
```

重点方法：

```java
assignUserRoles(UserRoleAssignDto dto)
assignRolePermissions(RolePermissionAssignDto dto)
```

当前实现是“覆盖式分配”：

```text
先删除旧关系
再插入新关系
```

例如给用户分配角色：

```java
rbacMapper.deleteUserRoles(dto.getUserId());
rbacMapper.addUserRoles(dto.getUserId(), dto.getRoleIds());
```

这意味着前端传入的是“最终角色列表”，不是“追加某个角色”。

## 12. 当前系统运行时完整链路

以新增 note 为例：

```text
1. 用户调用 /users/login
2. 后端返回 token、roles、permissions
3. 用户请求 POST /notes/add，Header 带 Authorization: Bearer token
4. JwtAuthenticationFilter 解析 token 得到 userId
5. JwtAuthenticationFilter 查数据库得到角色和权限
6. LoginUser 放入 SecurityContextHolder
7. @PreAuthorize("hasAuthority('note:add')") 检查权限
8. NoteServiceImpl 从 SecurityContextHolder 取当前 userId
9. insert note 时写入 user_id
```

以查询用户列表为例：

```text
1. 用户带 token 请求 GET /users/list
2. JwtAuthenticationFilter 加载权限
3. @PreAuthorize("hasAuthority('user:list')") 检查权限
4. 有权限则查询用户列表
5. 没权限则拒绝访问
```

## 13. Review 时建议按这个顺序看

建议你按下面顺序读代码：

1. `sql/init.sql`

   先看表结构，理解五张 RBAC 表的关系。

2. `SysUserEntity`、`SysRoleEntity`、`SysPermissionEntity`

   看实体和表字段怎么对应。

3. `UserMapper.xml`

   重点看 `findRoleCodesByUserId` 和 `findPermissionCodesByUserId`。

4. `UserServiceImpl.login`

   看登录时怎么查角色和权限，怎么返回给前端。

5. `JwtUtils`

   看 token 现在只保存用户身份。

6. `JwtAuthenticationFilter`

   看每次请求如何从 token 还原用户身份，并从数据库重新加载权限。

7. `LoginUser`

   看 roles 和 permissions 如何转换成 Spring Security 的 authorities。

8. `NoteController`、`UserController`、`RbacController`

   看 `@PreAuthorize` 如何使用权限码。

9. `NoteServiceImpl`

   看 note 如何绑定当前用户，如何防止越权访问别人的 note。

10. `RbacServiceImpl` 和 `RbacMapper.xml`

   看角色、权限、关系分配是如何落库的。

## 14. 当前还需要你注意的点

1. 数据库已经执行过 SQL 后，需要至少有一个管理员用户绑定 `ADMIN` 角色。

   否则没有人拥有 RBAC 管理接口权限。

2. 新注册用户默认只有 `USER` 角色。

   这类用户只能访问 note 相关接口，不能访问用户列表和 RBAC 管理接口。

3. 权限变更是近实时生效的。

   因为每次请求都会从数据库加载角色权限，所以修改角色权限后，用户下次请求就会用新权限。

4. 当前没有做权限缓存。

   这更简单，也更容易理解。后面性能需要时再考虑 Redis 或本地缓存。

5. 当前 RBAC 管理接口是基础版。

   它已经有增删改查和关系分配，但还可以继续补详情接口、分页、条件查询、操作日志等。

## 15. 一句话总结

这次改造后，系统的权限判断不再依赖 `sys_user.role`，而是通过：

```text
用户 -> 角色 -> 权限 -> @PreAuthorize
```

完成接口级权限控制；同时 note 模块也不再是全局数据，而是通过当前登录用户的 `userId` 做数据隔离。
