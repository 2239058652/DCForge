import { useCallback, useEffect, useRef, useState } from 'react'

interface TaskNotification {
    taskId: number
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED'
    imageUrl?: string
    revisedPrompt?: string
    errorMessage?: string
}

interface UseWebSocketOptions {
    onTaskUpdate?: (notification: TaskNotification) => void
}

export function useWebSocket(options?: UseWebSocketOptions) {
    const [connected, setConnected] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const wsRef = useRef<WebSocket | null>(null)
    const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
    const pendingSubscriptionsRef = useRef<Map<number, boolean>>(new Map())
    const reconnectDelay = useRef(1000)
    const maxReconnectDelay = 10000
    const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
    const token = localStorage.getItem(tokenName)
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsHost = import.meta.env.VITE_WS_HOST || 'localhost:5273'
    const hostStr = wsHost
    const onTaskUpdateRef = useRef(options?.onTaskUpdate)
    onTaskUpdateRef.current = options?.onTaskUpdate

    const connect = useCallback(() => {
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            return
        }

        const wsUrl = `${protocol}//${hostStr}/ws/ai/task?token=${encodeURIComponent(token || '')}`
        const ws = new WebSocket(wsUrl)
        wsRef.current = ws

        ws.onopen = () => {
            setConnected(true)
            setError(null)
            reconnectDelay.current = 1000

            // 重连后重新订阅
            pendingSubscriptionsRef.current.forEach((_, taskId) => {
                ws.send(JSON.stringify({ action: 'subscribe', taskId }))
            })
        }

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data) as TaskNotification

                if (data.taskId && data.status) {
                    onTaskUpdateRef.current?.(data)
                }
            } catch {
                // ignore parse errors
            }
        }

        ws.onerror = () => {
            setError('WebSocket 连接错误')
        }

        ws.onclose = () => {
            setConnected(false)
            // 自动重连
            reconnectTimerRef.current = setTimeout(() => {
                reconnectDelay.current = Math.min(reconnectDelay.current * 2, maxReconnectDelay)
                connect()
            }, reconnectDelay.current)
        }
    }, [token, protocol])

    const subscribe = useCallback((taskId: number) => {
        pendingSubscriptionsRef.current.set(taskId, true)
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify({ action: 'subscribe', taskId }))
        }
    }, [])

    const unsubscribe = useCallback((taskId: number) => {
        pendingSubscriptionsRef.current.delete(taskId)
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify({ action: 'unsubscribe', taskId }))
        }
    }, [])

    useEffect(() => {
        connect()
        return () => {
            if (reconnectTimerRef.current) {
                clearTimeout(reconnectTimerRef.current)
            }
            wsRef.current?.close()
            wsRef.current = null
            pendingSubscriptionsRef.current.clear()
        }
    }, [connect])

    return { connected, error, subscribe, unsubscribe }
}
