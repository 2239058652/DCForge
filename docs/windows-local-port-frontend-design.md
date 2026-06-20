# Windows 11 本机端口管理前端设计

## 目标

提供一个前端页面，用于查看 Windows 11 本机端口占用，并在用户明确确认后调用后端接口结束占用端口的进程。

前端只负责展示和发起请求，不能直接读取或关闭本机端口。端口数据全部来自后端 `GET /api/local-ports`。

## 页面入口

建议新增菜单：

```text
系统工具 / 本机端口
```

页面名称：

```text
本机端口
```

如果前端已有权限菜单体系，该页面需要绑定权限：

```text
local-port:list
```

结束进程按钮需要单独判断权限：

```text
local-port:terminate
```

## 页面布局

页面分为三部分：

```text
顶部筛选区
端口列表表格
结束进程确认弹窗
```

不要做成营销页，不需要说明性大段文字。这个页面是工具页，信息密度要高。

## 顶部筛选区

控件：

```text
协议：全部 / TCP / UDP
状态：全部 / Listen / Established / TimeWait / CloseWait
端口：数字输入框
关键字：输入框，支持进程名、PID、地址
只看有进程：开关
刷新按钮
```

交互规则：

- 进入页面自动查询一次。
- 点击刷新重新查询。
- 筛选变化可以立即查询，也可以点击查询按钮，按项目现有习惯实现。
- 刷新期间显示 loading。
- 查询失败时显示错误提示，不清空旧数据，避免页面突然空白。

## 表格字段

建议列：

```text
协议
本地地址
本地端口
远程地址
远程端口
状态
PID
进程名
可执行路径
命令行
操作
```

展示规则：

- `protocol` 显示为 `TCP` / `UDP`。
- `state` 原样显示，例如 `Listen`、`Established`、`TimeWait`。
- UDP 没有远程地址和远程端口时显示 `-`。
- `commandLine` 可能很长，表格内单行省略，鼠标悬停或展开时看完整内容。
- `canTerminate = false` 时，操作列按钮置灰，并用 tooltip 显示 `terminateBlockedReason`。

## 结束进程交互

操作按钮文案：

```text
结束进程
```

不要写成“关闭端口”，避免误导。端口释放是结束进程的结果。

点击后弹出危险确认框，必须展示：

```text
协议
端口
PID
进程名
可执行路径
命令行
```

确认文案建议：

```text
确认结束进程 node.exe（PID 12345）？
它当前占用 TCP 3000 端口。结束进程可能中断正在运行的服务。
```

确认按钮：

```text
结束进程
```

取消按钮：

```text
取消
```

调用接口：

```http
POST /api/local-ports/terminate
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

成功后：

- 显示成功提示。
- 自动刷新端口列表。
- 如果返回 `portReleased = false`，提示“进程已处理，但端口仍未释放，请刷新后确认”。

失败后：

- 显示后端返回的业务错误。
- 保留当前列表。
- 不要自动重试结束进程。

## 接口类型

前端类型建议：

```ts
type PortProtocol = 'all' | 'tcp' | 'udp'

interface LocalPortQuery {
  protocol?: PortProtocol
  state?: string
  port?: number
  keyword?: string
  onlyWithProcess?: boolean
}

interface LocalPortItem {
  protocol: 'tcp' | 'udp'
  localAddress: string
  localPort: number
  remoteAddress?: string | null
  remotePort?: number | null
  state: string
  pid: number
  processName?: string | null
  executablePath?: string | null
  commandLine?: string | null
  canTerminate: boolean
  terminateBlockedReason?: string | null
}

interface TerminatePortProcessRequest {
  pid: number
  protocol: 'tcp' | 'udp'
  port: number
  confirm: true
}

interface TerminatePortProcessResult {
  pid: number
  protocol: 'tcp' | 'udp'
  port: number
  processName?: string | null
  terminated: boolean
  portReleased: boolean
}
```

## API 封装建议

新增 API 文件时，按前端项目现有目录命名；如果没有现有约定，可用：

```text
src/api/localPorts.ts
```

函数：

```ts
getLocalPorts(query: LocalPortQuery): Promise<LocalPortItem[]>
terminateLocalPortProcess(payload: TerminatePortProcessRequest): Promise<TerminatePortProcessResult>
```

注意：

- 后端统一响应是 `{ code, message, data }`，前端需要按项目现有 request 封装处理。
- `GET /api/local-ports` 参数不要传空字符串，空筛选项直接不传。
- `port` 输入必须限制为 `1-65535`。

## 前端需要完成的事项

- [ ] 新增“本机端口”页面。
- [ ] 新增本机端口 API 封装。
- [ ] 页面加载时请求 `GET /api/local-ports`。
- [ ] 实现协议、状态、端口、关键字、只看有进程筛选。
- [ ] 实现刷新按钮和 loading 状态。
- [ ] 实现端口表格。
- [ ] 长命令行和路径做省略展示，保留查看完整内容的方式。
- [ ] 根据 `canTerminate` 控制“结束进程”按钮可用状态。
- [ ] 无 `local-port:terminate` 权限时隐藏或禁用“结束进程”按钮。
- [ ] 实现结束进程确认弹窗。
- [ ] 调用 `POST /api/local-ports/terminate`。
- [ ] 成功后刷新列表。
- [ ] 失败时展示后端错误，不自动重试。
- [ ] 处理空数据、接口错误、无权限、加载中状态。

## 验收标准

- 打开页面能看到当前 Windows 11 本机端口列表。
- 可以按 TCP/UDP、状态、端口和关键字筛选。
- 没有权限时不能执行结束进程操作。
- 对 `canTerminate = false` 的行，按钮不可点击并展示原因。
- 对普通测试进程点击“结束进程”后，需要二次确认。
- 确认后接口成功，列表自动刷新，端口从列表中消失或状态变化。
- 接口失败时页面保留原数据并显示错误提示。
