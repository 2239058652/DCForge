# Claude Code 第三方模型接入配置

配置文件路径通常为 `~/.claude/config.json` 或对应客户端（如 Cline/RooCode）的 `settings.json`。
使用前请将 `sk-xxx` 替换为实际的 API Key。

## 1. DeepSeek (deepseek-v4)

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "https://api.deepseek.com/anthropic",
    "ANTHROPIC_AUTH_TOKEN": "sk-xxx",
    "ANTHROPIC_MODEL": "deepseek-v4-pro[1m]",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "deepseek-v4-pro[1m]",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "deepseek-v4-pro[1m]",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "deepseek-v4-flash[1m]",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
    "CLAUDE_CODE_ATTRIBUTION_HEADER": "0",
    "CLAUDE_CODE_EFFORT_LEVEL": "max"
  },
  "model": "deepseek-v4-pro",
  "theme": "auto"
}
```

## 2. Mimo 中转 (mimo-v2.5)

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "https://api.xiaomimimo.com/anthropic",
    "ANTHROPIC_AUTH_TOKEN": "sk-xxx",
    "ANTHROPIC_MODEL": "mimo-v2.5-pro[1m]",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "mimo-v2.5-pro[1m]",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "mimo-v2.5-pro[1m]",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "mimo-v2.5[1m]",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
    "CLAUDE_CODE_ATTRIBUTION_HEADER": "0",
    "CLAUDE_CODE_EFFORT_LEVEL": "max"
  },
  "model": "opus",
  "theme": "light"
}
```

## 3. Moonshot Kimi (kimi-k2)

```json
{
  "env": {
    "ANTHROPIC_BASE_URL": "https://api.moonshot.cn/anthropic",
    "ANTHROPIC_AUTH_TOKEN": "sk-xxx",
    "ANTHROPIC_MODEL": "kimi-k2.6",
    "ANTHROPIC_DEFAULT_OPUS_MODEL": "kimi-k2.6",
    "ANTHROPIC_DEFAULT_SONNET_MODEL": "kimi-k2.6",
    "ANTHROPIC_DEFAULT_HAIKU_MODEL": "kimi-k2.5",
    "CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC": "1",
    "CLAUDE_CODE_ATTRIBUTION_HEADER": "0",
    "CLAUDE_CODE_EFFORT_LEVEL": "max"
  },
  "model": "kimi-k2.6",
  "theme": "auto"
}
```

## 环境变量说明

* `ANTHROPIC_BASE_URL`: API 转发代理地址。
* `ANTHROPIC_AUTH_TOKEN`: 鉴权密钥。
* `CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC`: 设为 `1` 禁用非必要遥测流量，防止第三方接口报错。
* `CLAUDE_CODE_EFFORT_LEVEL`: 设为 `max` 开启最大推理深度。
* `[1m]` 后缀: 部分中转 API 用于指定上下文窗口大小（1M tokens），若报错可尝试删除该后缀。