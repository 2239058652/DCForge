# AI 图像生成 - 异步任务系统设计

## 一、背景与目标

### 问题

前端请求图生图/文生图后，需要等待 AI 模型返回结果（可能 1-2 分钟）。如果前端关闭或断开连接，结果丢失。

### 目标

引入消息队列（RabbitMQ）和 WebSocket，实现：
- 请求立即返回，异步处理
- 前端实时接收任务状态变化
- 任务结果持久化，不会丢失

### 技术选型

| 组件 | 技术 | 用途 |
|------|------|------|
| 消息队列 | RabbitMQ | 任务调度 |
| 实时推送 | WebSocket | 后端主动推送任务状态 |
| 任务存储 | MySQL | 持久化任务状态和结果 |

---

## 二、任务状态

| 状态 | 含义 |
|------|------|
| `PENDING` | 已入队，等待处理 |
| `PROCESSING` | 正在处理 |
| `COMPLETED` | 处理完成 |
| `FAILED` | 处理失败 |

---

## 三、数据库设计

```sql
CREATE TABLE ai_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    prompt          VARCHAR(1000) NOT NULL,
    size            VARCHAR(20) DEFAULT NULL,
    images          TEXT DEFAULT NULL,
    object_name     VARCHAR(500) DEFAULT NULL,
    revised_prompt  VARCHAR(1000) DEFAULT NULL,
    error_message   VARCHAR(500) DEFAULT NULL,
    retry_count     INT DEFAULT 0,
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL
);
```

---

## 四、API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/task/submit` | 提交任务 |
| GET | `/ai/task/{id}` | 查询任务 |
| GET | `/ai/task/page` | 分页查询 |

---

## 五、WebSocket

连接地址：`ws://localhost:5273/ws/ai/task?token={JWT_TOKEN}`

订阅：`{ "action": "subscribe", "taskId": 12345 }`

取消订阅：`{ "action": "unsubscribe", "taskId": 12345 }`

推送格式：
```json
{
  "taskId": 12345,
  "status": "COMPLETED",
  "imageUrl": "http://minio:9000/...",
  "revisedPrompt": "...",
  "errorMessage": null
}
```
