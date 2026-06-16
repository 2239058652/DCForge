# 前端改造提示词

## 背景

后端已完成 AI 图像生成的异步任务系统改造。需要改造前端配合新的异步接口。

## 新增接口

### 1. 提交任务

**POST** `/ai/task/submit`

请求体：
```json
{
  "type": "text2img",
  "prompt": "一只可爱的橘猫",
  "size": "1024x768",
  "images": null
}
```

响应：
```json
{
  "code": 200,
  "data": {
    "id": 12345,
    "type": "text2img",
    "status": "PENDING",
    "prompt": "一只可爱的橘猫",
    "size": "1024x768",
    "createdAt": "2026-06-11 15:30:00"
  }
}
```

### 2. 查询任务状态

**GET** `/ai/task/{id}`

### 3. 分页查询任务列表

**GET** `/ai/task/page?pageNum=1&pageSize=10&status=COMPLETED`

## WebSocket 连接

连接地址：`ws://localhost:5273/ws/ai/task?token={JWT_TOKEN}`

订阅消息（前端发给后端）：
```json
{ "action": "subscribe", "taskId": 12345 }
```

接收通知（后端推给前端）：
```json
{
  "taskId": 12345,
  "status": "COMPLETED",
  "imageUrl": "http://localhost:9000/...",
  "revisedPrompt": "...",
  "errorMessage": null
}
```

## 任务状态

| 状态 | 前端展示 |
|------|---------|
| `PENDING` | 提交成功后前端自己显示"任务已提交" |
| `PROCESSING` | 显示"正在生成图片" + loading |
| `COMPLETED` | 显示图片 |
| `FAILED` | 显示错误信息 |

## 改造步骤

1. 新增 API 方法（submitTask, getTask, getTaskPage）
2. 创建 WebSocket Hook
3. 修改 AiImage 页面为异步提交
4. 修改历史记录页调用 /ai/task/page

## 注意事项

- PENDING 由前端自己处理，不依赖 WebSocket 推送
- WebSocket 断开后自动重连
- 保留 /ai/image/history/save 接口用于保存图片
