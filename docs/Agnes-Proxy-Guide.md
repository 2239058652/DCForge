# Agnes Proxy 使用指南

## 简介

Agnes Proxy 是 DCForge 项目中的一个代理服务，用于让 Claude Code（使用 Anthropic API 格式）能够调用 Agnes 2.0 Flash 模型（仅支持 OpenAI API 格式）。

```
┌─────────────┐     Anthropic 格式     ┌──────────────┐     OpenAI 格式     ┌─────────────┐
│             │ ──────────────────────> │              │ ──────────────────> │             │
│ Claude Code │ <────────────────────── │  Agnes Proxy │ <────────────────── │  Agnes API  │
│             │     Anthropic 格式     │   (DCForge)  │     OpenAI 格式     │             │
└─────────────┘                        └──────────────┘                     └─────────────┘
```

## 架构说明

### 核心组件

1. **AgnesProxyController** - 提供 Anthropic 兼容的 API 端点
   - `POST /v1/messages` - 同步聊天
   - `POST /v1/messages/stream` - 流式聊天 (Server-Sent Events)
   - `GET /v1/models` - 模型信息

2. **AgnesProxyService** - 格式转换服务
   - Anthropic → OpenAI 请求转换
   - OpenAI → Anthropic 响应转换
   - 流式响应格式转换

### API 格式差异

| 特性 | Anthropic API | OpenAI API |
|------|-------------|------------|
| System prompt | 独立 `system` 参数 | `messages` 数组中 `role: "system"` |
| 认证 Header | `x-api-key` | `Authorization: Bearer` |
| 响应内容位置 | `content[].text` | `choices[0].message.content` |
| Token 统计字段 | `input_tokens` / `output_tokens` | `prompt_tokens` / `completion_tokens` |
| API 版本 Header | `anthropic-version` | 无 |

## 配置步骤

### 1. 启动 DCForge 应用

确保 DCForge 应用在本地运行（默认端口 5273）：

```bash
mvn spring-boot:run
```

### 2. 配置 Claude Code

编辑 Claude Code 配置文件 `C:\Users\22390\.claude\settings.json`：

```json
{
  "env": {
    "ANTHROPIC_AUTH_TOKEN": "your-agnes-api-key",
    "ANTHROPIC_BASE_URL": "http://localhost:5273/v1",
    "ANTHROPIC_MODEL": "agnes-2.0-flash",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1"
  }
}
```

**配置说明：**

- `ANTHROPIC_AUTH_TOKEN` - 你的 Agnes API Key（与 DCForge 配置中的 `agnes.chat.api-key` 相同）
- `ANTHROPIC_BASE_URL` - 指向 DCForge 代理地址（注意：不包含 `/messages`）
- `ANTHROPIC_MODEL` - 模型名称（可选，Agnes 默认使用 `agnes-2.0-flash`）
- `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC` - 禁用遥测，避免不必要的错误

### 3. 启动 Claude Code

```bash
claude
```

## 环境变量说明

| 变量 | 必需 | 说明 | 示例 |
|------|------|------|------|
| `ANTHROPIC_AUTH_TOKEN` | 是 | Agnes API Key | `sk-xxx...` |
| `ANTHROPIC_BASE_URL` | 是 | 代理服务器地址 | `http://localhost:5273/v1` |
| `ANTHROPIC_MODEL` | 否 | 模型名称 | `agnes-2.0-flash` |
| `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC` | 推荐 | 禁用遥测 | `1` |

## 验证配置

### 1. 测试代理端点是否可用

```bash
curl http://localhost:5273/v1/models
```

应返回：
```json
{
  "data": [
    {
      "type": "model",
      "id": "agnes-2.0-flash",
      "display_name": "Agnes 2.0 Flash (Proxy)"
    }
  ],
  "has_more": false
}
```

### 2. 测试同步请求

```bash
curl -X POST http://localhost:5273/v1/messages \
  -H "x-api-key: your-api-key" \
  -H "anthropic-version: 2023-06-01" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "agnes-2.0-flash",
    "max_tokens": 100,
    "messages": [
      {"role": "user", "content": "Hello!"}
    ]
  }'
```

### 3. 测试流式请求

```bash
curl -X POST http://localhost:5273/v1/messages/stream \
  -H "x-api-key: your-api-key" \
  -H "anthropic-version: 2023-06-01" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "agnes-2.0-flash",
    "max_tokens": 100,
    "stream": true,
    "messages": [
      {"role": "user", "content": "Hello!"}
    ]
  }'
```

## 常见问题

### Q: 请求返回 401 Unauthorized

**原因：** API Key 无效或未正确设置

**解决：**
1. 检查 `ANTHROPIC_AUTH_TOKEN` 是否与 DCForge 配置中的 API Key 一致
2. 确保 Key 没有过期

### Q: 请求超时或无响应

**原因：** DCForge 应用未启动或网络不通

**解决：**
1. 确认 DCForge 应用正在运行
2. 检查端口 5273 是否被占用
3. 检查防火墙设置

### Q: Claude Code 报错 "Invalid API format"

**原因：** 请求或响应格式不兼容

**解决：**
1. 确保使用的是最新版 DCForge
2. 检查 Claude Code 版本是否兼容

### Q: 流式响应中断

**原因：** 网络不稳定或超时

**解决：**
1. 增加 `agnes.chat.timeout` 配置
2. 检查网络连接

## 限制

- 当前版本仅支持基础聊天功能（text content）
- 不支持 Anthropic 特有功能（如 tool_use、computer_use）
- 流式响应使用 Server-Sent Events 格式
- 需要 DCForge 应用持续运行

## 故障排查

### 查看日志

DCForge 日志中会记录代理请求和响应：

```
INFO  收到 Anthropic 消息请求: model=agnes-2.0-flash, messages=1
INFO  Agnes Chat 请求: model=agnes-2.0-flash, messages=2
INFO  Agnes Chat 响应状态: 200
```

### 启用调试日志

在 `application-dev.yaml` 中添加：

```yaml
logging:
  level:
    com.forge.dc.modules.ai: DEBUG
```

## 安全建议

- 不要在公网暴露代理端点
- 使用环境变量管理 API Key，不要硬编码
- 定期轮换 API Key
- 监控异常请求

## 更新记录

- 2024-01 - 初始版本，支持基础和流式聊天
