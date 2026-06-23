# 聊天机器人 API 对接文档

## 概述

DCForge 提供基于 Agnes 2.0 Flash 的聊天机器人接口，支持同步 REST 和流式 WebSocket 两种方式。

---

## 认证方式

所有接口都需要 JWT 认证。

### 获取 Token

```
POST /users/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

响应：
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "your_username"
  }
}
```

---

## REST API - 同步聊天

### 接口信息

| 项目 | 说明 |
|------|------|
| URL | `POST /ai/chat/completion` |
| Content-Type | `application/json` |
| 认证 | `Authorization: Bearer {token}` |

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| messages | array | 是 | 对话消息列表 |
| messages[].role | string | 是 | 消息角色：`system`、`user`、`assistant` |
| messages[].content | string | 是 | 消息内容 |
| systemPrompt | string | 否 | 系统提示词（首次对话时传入） |
| temperature | number | 否 | 随机性，0-2，默认 0.7 |
| topP | number | 否 | 核采样，0-1，默认 1 |
| maxTokens | integer | 否 | 最大输出 token 数，默认 1024 |

### 请求示例

```json
{
  "messages": [
    {"role": "user", "content": "你好"}
  ],
  "systemPrompt": "你是一个有用的AI助手",
  "temperature": 0.7,
  "maxTokens": 2048
}
```

### 响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "chatcmpl_xxx",
    "model": "agnes-2.0-flash",
    "choices": [
      {
        "index": 0,
        "message": {
          "role": "assistant",
          "content": "你好！有什么我可以帮助你的？"
        },
        "finish_reason": "stop"
      }
    ],
    "usage": {
      "prompt_tokens": 15,
      "completion_tokens": 12,
      "total_tokens": 27
    }
  }
}
```

### 前端示例（JavaScript）

```javascript
async function chat(token, messages, systemPrompt) {
  const response = await fetch('/ai/chat/completion', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      messages: messages,
      systemPrompt: systemPrompt,
      temperature: 0.7,
      maxTokens: 2048
    })
  });

  return await response.json();
}

// 使用示例
const result = await chat(
  'eyJhbGciOiJIUzI1NiJ9...',
  [{ role: 'user', content: '你好' }],
  '你是一个有用的AI助手'
);
console.log(result.data.choices[0].message.content);
```

---

## WebSocket - 流式聊天

### 接口信息

| 项目 | 说明 |
|------|------|
| URL | `ws://localhost:5273/ws/ai/chat?token={token}` |
| 协议 | WebSocket |
| 认证 | URL 参数 `?token=xxx` |

### 客户端发送消息

#### 发送聊天消息

```json
{
  "action": "message",
  "content": "你好",
  "systemPrompt": "你是一个有用的AI助手",
  "temperature": 0.7,
  "maxTokens": 2048
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| action | string | 是 | 固定为 `message` |
| content | string | 是 | 用户输入的消息内容 |
| systemPrompt | string | 否 | 系统提示词（仅首次对话时传入） |
| temperature | number | 否 | 随机性，0-2，默认 0.7 |
| maxTokens | integer | 否 | 最大输出 token 数 |

#### 停止生成

```json
{
  "action": "stop"
}
```

#### 清除对话历史

```json
{
  "action": "clear"
}
```

### 服务端推送消息

#### 流式内容片段

```json
{
  "type": "chunk",
  "content": "你"
}
```

#### 生成完成

```json
{
  "type": "done",
  "content": "你好！有什么我可以帮助你的？"
}
```

#### 错误

```json
{
  "type": "error",
  "message": "错误信息"
}
```

#### 状态变更

```json
{
  "type": "status",
  "status": "processing"
}
```

| status 值 | 说明 |
|-----------|------|
| processing | 正在生成中 |
| stopped | 用户停止生成 |
| cleared | 对话历史已清除 |

### 前端示例（JavaScript）

```javascript
class ChatClient {
  constructor(token) {
    this.token = token;
    this.ws = null;
    this.sessionId = this.generateSessionId();
    this.onMessage = null;
    this.onComplete = null;
    this.onError = null;
  }

  connect() {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(`ws://localhost:5273/ws/ai/chat?token=${this.token}`);

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功');
        resolve();
      };

      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        this.handleMessage(data);
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
        if (this.onError) this.onError(error);
      };

      this.ws.onclose = () => {
        console.log('WebSocket 连接关闭');
      };
    });
  }

  handleMessage(data) {
    switch (data.type) {
      case 'chunk':
        if (this.onMessage) this.onMessage(data.content);
        break;
      case 'done':
        if (this.onComplete) this.onComplete(data.content);
        break;
      case 'error':
        if (this.onError) this.onError(data.message);
        break;
      case 'status':
        console.log('状态变更:', data.status);
        break;
    }
  }

  sendMessage(content, systemPrompt) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        action: 'message',
        content: content,
        systemPrompt: systemPrompt
      }));
    }
  }

  stop() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ action: 'stop' }));
    }
  }

  clear() {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({ action: 'clear' }));
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close();
    }
  }

  generateSessionId() {
    return 'session_' + Math.random().toString(36).substr(2, 9);
  }
}

// 使用示例
async function initChat() {
  // 1. 先登录获取 token
  const loginRes = await fetch('/users/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: '123456' })
  });
  const { data: { token } } = await loginRes.json();

  // 2. 连接 WebSocket
  const chatClient = new ChatClient(token);
  await chatClient.connect();

  // 3. 设置回调
  let fullResponse = '';
  chatClient.onMessage = (chunk) => {
    fullResponse += chunk;
    console.log('收到片段:', chunk);
    console.log('当前内容:', fullResponse);
  };
  chatClient.onComplete = (content) => {
    console.log('完整回复:', content);
  };
  chatClient.onError = (error) => {
    console.error('错误:', error);
  };

  // 4. 发送消息
  chatClient.sendMessage('你好', '你是一个有用的AI助手');

  // 5. 如需停止生成
  // chatClient.stop();

  // 6. 如需清除对话历史
  // chatClient.clear();
}
```

---

## 多轮对话

### REST API 方式

客户端需要维护完整的对话历史，每次请求时传入：

```javascript
const messages = [
  { role: 'user', content: '你好' },
  { role: 'assistant', content: '你好！有什么可以帮你的？' },
  { role: 'user', content: '帮我写代码' }
];
```

### WebSocket 方式

服务端自动维护对话历史，客户端无需手动管理。如需重新开始对话，发送 `{"action": "clear"}` 清除历史。

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未认证（token 无效或过期） |
| 403 | 无权限访问 |
| 500 | 服务器内部错误 |

---

## 注意事项

1. **Token 过期**: JWT token 有效期为 24 小时，过期后需要重新登录
2. **权限配置**: REST 接口需要 `ai:chat` 权限
3. **WebSocket 白名单**: `/ws/**` 路径不走接口权限校验，仅验证 token 有效性
4. **对话历史**: WebSocket 模式下服务端会保存对话历史，直到连接断开或发送 clear 命令
5. **并发限制**: 同一用户同时只能有一个活跃的连接，新连接会替换旧连接

---

## 测试工具

### cURL 测试 REST API

```bash
curl -X POST http://localhost:5273/ai/chat/completion \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"hi"}]}'
```

### WebSocket 测试

使用浏览器开发者工具或 Postman 的 WebSocket 功能进行测试。
