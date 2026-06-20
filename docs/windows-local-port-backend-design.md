# Windows 11 本机端口管理后端设计

## 目标

实现一个仅面向 Windows 11 本机的端口管理模块，供前端查看当前电脑的端口占用情况，并在用户确认后结束占用指定端口的进程。

必须明确：这里的“本机电脑”指 Spring Boot 后端进程正在运行的这台 Windows 11 电脑。如果后端部署到服务器，查询和关闭的就是服务器端口，不是用户浏览器所在电脑端口。

## 现有项目约束

- 后端是 Spring Boot 3 + Java 17。
- 统一响应使用 `com.forge.dc.common.result.Result`。
- 业务异常使用 `com.forge.dc.common.exception.BusinessException`。
- Controller 使用 `@RestController`、`@RequestMapping`、`@Operation`。
- 项目已有 Spring Security 和 `@PreAuthorize` 权限控制。
- 不需要新增数据库表保存端口数据，端口数据每次实时查询 Windows 系统。
- 不要为了 macOS/Linux 做跨平台抽象，本需求只做 Windows 11。
- 不引入新的第三方依赖，优先使用 JDK `ProcessBuilder` 调用 PowerShell，并用 Spring Boot 已有 Jackson 解析 JSON。

## 模块目录建议

新增包：

```text
src/main/java/com/forge/dc/modules/localport/
```

建议文件：

```text
controller/LocalPortController.java
service/LocalPortService.java
service/impl/LocalPortServiceImpl.java
dto/LocalPortQueryDto.java
dto/TerminatePortProcessDto.java
vo/LocalPortVO.java
vo/TerminatePortProcessVO.java
support/PowerShellCommandRunner.java
support/PortProcessTerminatePolicy.java
```

说明：

- `LocalPortController`：只处理 HTTP 入参、权限、返回 `Result`。
- `LocalPortService`：定义查询端口和结束进程能力。
- `LocalPortServiceImpl`：调用 PowerShell，解析结果，做业务校验。
- `PowerShellCommandRunner`：统一执行 PowerShell 命令，处理超时、stdout、stderr、退出码。
- `PortProcessTerminatePolicy`：集中判断某个进程是否允许被结束。

## 后端接口

### 查询本机端口

```http
GET /api/local-ports
```

权限建议：

```java
@PreAuthorize("hasAuthority('local-port:list')")
```

查询参数：

```text
protocol: all | tcp | udp，默认 all
state: all | listen | established | time_wait | close_wait，默认 all
port: 可选，端口号 1-65535
keyword: 可选，按进程名、PID、地址模糊过滤
onlyWithProcess: 可选 boolean，默认 false
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "protocol": "tcp",
      "localAddress": "127.0.0.1",
      "localPort": 3000,
      "remoteAddress": "0.0.0.0",
      "remotePort": null,
      "state": "Listen",
      "pid": 12345,
      "processName": "node.exe",
      "executablePath": "C:\\Program Files\\nodejs\\node.exe",
      "commandLine": "node server.js",
      "canTerminate": true,
      "terminateBlockedReason": null
    }
  ]
}
```

### 结束占用端口的进程

```http
POST /api/local-ports/terminate
```

权限建议：

```java
@PreAuthorize("hasAuthority('local-port:terminate')")
```

请求体：

```json
{
  "pid": 12345,
  "protocol": "tcp",
  "port": 3000,
  "confirm": true
}
```

字段要求：

- `pid` 必填，必须大于 0。
- `protocol` 必填，只允许 `tcp` 或 `udp`。
- `port` 必填，范围 `1-65535`。
- `confirm` 必须为 `true`，否则拒绝操作。

处理规则：

1. 重新查询当前端口列表。
2. 确认该 `pid + protocol + port` 当前仍然匹配，防止前端使用过期数据。
3. 调用 `PortProcessTerminatePolicy` 判断是否允许结束。
4. 执行 `Stop-Process -Id <pid> -Force -ErrorAction Stop`。
5. 再次查询该端口，返回是否已释放。

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pid": 12345,
    "protocol": "tcp",
    "port": 3000,
    "processName": "node.exe",
    "terminated": true,
    "portReleased": true
  }
}
```

## PowerShell 查询设计

### TCP

使用：

```powershell
Get-NetTCPConnection |
Select-Object @{Name='protocol';Expression={'tcp'}},
              LocalAddress,
              LocalPort,
              RemoteAddress,
              RemotePort,
              State,
              OwningProcess |
ConvertTo-Json -Depth 3
```

### UDP

使用：

```powershell
Get-NetUDPEndpoint |
Select-Object @{Name='protocol';Expression={'udp'}},
              LocalAddress,
              LocalPort,
              @{Name='RemoteAddress';Expression={$null}},
              @{Name='RemotePort';Expression={$null}},
              @{Name='State';Expression={'None'}},
              OwningProcess |
ConvertTo-Json -Depth 3
```

### 进程信息

用端口查询得到的 PID 去补充进程信息。可以按 PID 批量查询：

```powershell
$ids = @(12345,23456);
Get-CimInstance Win32_Process |
Where-Object { $ids -contains [int]$_.ProcessId } |
Select-Object ProcessId,Name,ExecutablePath,CommandLine |
ConvertTo-Json -Depth 3
```

注意事项：

- `ConvertTo-Json` 在只有一条记录时可能输出 JSON object，多条时输出 JSON array，Java 解析时两种都要兼容。
- `CommandLine` 和 `ExecutablePath` 可能为空，不能当作错误。
- 某些系统进程可能因为权限不足拿不到完整信息。
- PowerShell 命令必须设置超时，建议 5 秒。
- 使用 `ProcessBuilder` 传参，不要拼接来自前端的原始字符串。

## 结束进程安全策略

`PortProcessTerminatePolicy` 必须阻止危险进程。

至少禁止：

```text
pid <= 4
当前 Spring Boot 后端自己的 PID
System
Registry
Idle
smss.exe
csrss.exe
wininit.exe
winlogon.exe
services.exe
lsass.exe
svchost.exe
fontdrvhost.exe
dwm.exe
explorer.exe
```

建议规则：

- 默认只允许结束普通用户态应用进程。
- `svchost.exe` 一律禁止，很多 Windows 服务端口都在里面。
- 如果 PowerShell 返回权限不足，转换为业务错误，不要返回 500 堆栈。
- 结束进程前必须重新校验 PID 仍占用该端口。
- 日志中记录：当前登录用户、PID、端口、协议、进程名、结果。

## 权限和动态接口配置

需要新增权限码：

```text
local-port:list
local-port:terminate
```

需要给管理员角色分配以上权限。

如果继续使用 `interface_permission` 动态接口权限表，需要新增接口映射：

```text
GET  /api/local-ports             local-port:list
POST /api/local-ports/terminate   local-port:terminate
```

不要把结束进程接口配置成 `PERMIT_ALL`。

## DTO/VO 字段建议

`LocalPortQueryDto`：

```text
String protocol
String state
Integer port
String keyword
Boolean onlyWithProcess
```

`TerminatePortProcessDto`：

```text
Long pid
String protocol
Integer port
Boolean confirm
```

`LocalPortVO`：

```text
String protocol
String localAddress
Integer localPort
String remoteAddress
Integer remotePort
String state
Long pid
String processName
String executablePath
String commandLine
Boolean canTerminate
String terminateBlockedReason
```

`TerminatePortProcessVO`：

```text
Long pid
String protocol
Integer port
String processName
Boolean terminated
Boolean portReleased
```

## 错误处理

建议用 `BusinessException` 返回明确错误：

```text
400 confirm must be true
400 invalid protocol
400 invalid port
403 process is protected
404 port owner not found
500 powershell command failed
```

不要把 PowerShell stderr 原样暴露给前端，可以在 dev 环境日志中保留详细信息。

## 后端需要完成的事项

- [ ] 新增 `localport` 模块目录和 Controller/Service/DTO/VO。
- [ ] 实现 PowerShell 命令执行器，支持超时、退出码、stdout、stderr。
- [ ] 实现 TCP/UDP 端口查询，并统一成 `LocalPortVO`。
- [ ] 实现 PID 到进程信息的批量补充。
- [ ] 实现协议、状态、端口、关键字过滤。
- [ ] 实现 `canTerminate` 和 `terminateBlockedReason`。
- [ ] 实现结束进程接口。
- [ ] 结束进程前重新校验 PID 仍然占用该端口。
- [ ] 禁止结束系统关键进程和当前后端进程。
- [ ] 增加 `local-port:list`、`local-port:terminate` 权限。
- [ ] 增加动态接口权限映射，禁止 `PERMIT_ALL`。
- [ ] 给 Swagger/OpenAPI 添加接口说明。
- [ ] 增加单元测试：参数校验、策略拦截、JSON 单对象/数组解析。
- [ ] 在 Windows 11 上手动验证查询和结束普通测试进程。

## 验收标准

- 启动后端后，调用 `GET /api/local-ports` 可以看到 Windows 11 当前 TCP/UDP 端口。
- 能看到 PID、进程名、端口、协议、状态。
- 对普通测试进程，例如本地 Node/Vite/Spring 测试端口，执行 terminate 后端口释放。
- 对系统进程、当前后端进程、`svchost.exe`，terminate 接口返回禁止操作。
- 前端传入旧 PID 或旧端口关系时，后端拒绝结束进程。
- 未登录或无权限用户不能查询或结束进程。
