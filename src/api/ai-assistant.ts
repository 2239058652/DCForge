import request from '@/request'

/** 消息角色 */
export type ChatRole = 'system' | 'user' | 'assistant'

/** 单条消息 */
export interface ChatMessage {
    role: ChatRole
    content: string
}

/** 消息内容项（文本或图片） */
export interface ContentItem {
    type: 'text' | 'image_url'
    text?: string
    image_url?: {
        url: string
    }
}

/** 单条消息（支持多模态） */
export interface ChatMessage {
    role: ChatRole
    content: string | ContentItem[]
}

/** REST 聊天请求参数 */
export interface ChatRequest {
    messages: ChatMessage[]
    systemPrompt?: string
    temperature?: number
    topP?: number
    maxTokens?: number
}

/** 响应中的消息 */
export interface ResponseMessage {
    role: string
    content: string
}

/** 响应中的选项 */
export interface ChatChoice {
    index: number
    message: ResponseMessage
    finish_reason: string
}

/** REST 聊天响应 */
export interface ChatResponse {
    id: string
    model: string
    choices: ChatChoice[]
    usage: {
        prompt_tokens: number
        completion_tokens: number
        total_tokens: number
    }
}

/** 将 HTML 富文本转为纯文本 */
export function htmlToText(html: string): string {
    const div = document.createElement('div')
    div.innerHTML = html
    return div.textContent || div.innerText || ''
}

/**
 * 从 HTML 中提取图片（转为 base64）和文本
 * 返回 { text, images }，images 是 base64 字符串数组
 */
export function parseHtmlContent(html: string): { text: string; images: string[] } {
    const div = document.createElement('div')
    div.innerHTML = html
    const images: string[] = []

    // 提取 img src（已经是 base64 的话直接取）
    div.querySelectorAll('img').forEach(img => {
        const src = img.getAttribute('src') || ''
        if (src) images.push(src)
    })

    const text = div.textContent || div.innerText || ''
    return { text, images }
}

/**
 * 将 HTML 转为多模态 ContentItem 数组
 * 文本和图片分开，图片以 base64 发送
 */
export function htmlToContentItems(html: string): ContentItem[] {
    const items: ContentItem[] = []
    const { text, images } = parseHtmlContent(html)

    if (text.trim()) {
        items.push({ type: 'text', text: text.trim() })
    }

    images.forEach(url => {
        items.push({ type: 'image_url', image_url: { url } })
    })

    return items
}

export const aiAssistantApi = {
    /** REST 同步聊天 */
    sendMessage: (data: ChatRequest) =>
        request.post<ChatResponse>('/ai/chat/completion', data)
}

/** WebSocket 连接选项 */
export interface WsConnectOptions {
    /** 收到流式片段回调 */
    onChunk: (chunk: string) => void
    /** 生成完成回调 */
    onDone: (fullContent: string) => void
    /** 错误回调 */
    onError: (error: string) => void
    /** 状态变更回调 */
    onStatus?: (status: string) => void
}

/**
 * 创建 WebSocket 聊天连接
 * @param token JWT token
 * @param options 回调选项
 */
export function createWsConnection(token: string, options: WsConnectOptions) {
    const baseWsUrl = import.meta.env.VITE_WS_BASE_URL || `ws://${window.location.host}`
    let ws: WebSocket | null = null
    let manualClose = false

    const connect = (): Promise<void> => {
        return new Promise((resolve, reject) => {
            ws = new WebSocket(`${baseWsUrl}/ws/ai/chat?token=${token}`)

            ws.onmessage = (event: MessageEvent) => {
                try {
                    const data = JSON.parse(event.data)
                    switch (data.type) {
                        case 'chunk':
                            options.onChunk(data.content)
                            break
                        case 'done':
                            options.onDone(data.content)
                            break
                        case 'error':
                            options.onError(data.message)
                            break
                        case 'status':
                            options.onStatus?.(data.status)
                            break
                    }
                } catch {
                    // 忽略无法解析的消息
                }
            }

            ws.onclose = () => {
                if (!manualClose) {
                    options.onError('WebSocket 连接已断开')
                }
            }

            ws.onerror = (e) => reject(e)
            ws.onopen = () => resolve()
        })
    }

    const sendMessage = (content: string, systemPrompt?: string) => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ action: 'message', content, systemPrompt }))
        }
    }

    const stop = () => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ action: 'stop' }))
        }
    }

    const clear = () => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ action: 'clear' }))
        }
    }

    const disconnect = () => {
        manualClose = true
        ws?.close()
        ws = null
    }

    return { connect, sendMessage, stop, clear, disconnect }
}
