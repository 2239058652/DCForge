import { Icon } from '@iconify/react'
import { App, Button, Switch } from 'antd'
import type { IDomEditor } from '@wangeditor/editor'
import { Editor, Toolbar } from '@wangeditor/editor-for-react'
import '@wangeditor/editor/dist/css/style.css'
import { useCallback, useEffect, useRef, useState } from 'react'
import { aiAssistantApi, createWsConnection, htmlToContentItems, htmlToText } from '@/api/ai-assistant'
import type { ChatMessage } from '@/api/ai-assistant'
import './index.css'

type SendMode = 'rest' | 'ws'

const AiAssistant = () => {
    const { message } = App.useApp()
    const msg = message
    const [editor, setEditor] = useState<IDomEditor | null>(null)
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [sending, setSending] = useState(false)
    const [mode, setMode] = useState<SendMode>('rest')
    const messagesEndRef = useRef<HTMLDivElement>(null)
    const wsRef = useRef<ReturnType<typeof createWsConnection> | null>(null)
    const streamingRef = useRef(false)

    const scrollToBottom = useCallback(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
    }, [])

    useEffect(() => {
        scrollToBottom()
    }, [messages, scrollToBottom])

    // 获取 token
    const getToken = useCallback(() => {
        const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
        return localStorage.getItem(tokenName) || ''
    }, [])

    // 切换模式时连接/断开 WebSocket
    const handleModeChange = useCallback((checked: boolean) => {
        const newMode = checked ? 'ws' : 'rest'

        if (newMode === 'ws') {
            const token = getToken()
            if (!token) {
                msg.error('未找到登录 token，请重新登录')
                return
            }

            const ws = createWsConnection(token, {
                onChunk: (chunk) => {
                    setMessages(prev => {
                        const last = prev[prev.length - 1]
                        if (last && last.role === 'assistant') {
                            const updated = { ...last, content: last.content + chunk }
                            return [...prev.slice(0, -1), updated]
                        }
                        return [...prev, { role: 'assistant', content: chunk }]
                    })
                },
                onDone: () => {
                    streamingRef.current = false
                    setSending(false)
                },
                onError: (error) => {
                    streamingRef.current = false
                    setSending(false)
                    message.error(error)
                    // 连接失败自动切回同步模式
                    setMode('rest')
                    wsRef.current = null
                },
                onStatus: (status) => {
                    if (status === 'stopped') {
                        streamingRef.current = false
                        setSending(false)
                    }
                }
            })

            ws.connect().then(() => {
                wsRef.current = ws
                setMode('rest')  // 连接成功后再切到 ws
                setMode('ws')
                message.success('已切换到流式输出')
            }).catch(() => {
                msg.error('WebSocket 连接失败，请确认后端已启动')
                ws.disconnect()
                wsRef.current = null
            })
        } else {
            wsRef.current?.disconnect()
            wsRef.current = null
            setMode('rest')
            message.success('已切换到同步输出')
        }
    }, [getToken, message])

    // 组件卸载时断开 WebSocket
    useEffect(() => {
        return () => {
            wsRef.current?.disconnect()
        }
    }, [])


    // REST 模式发送
    const sendRest = useCallback(async () => {
        const html = editor?.getHtml().trim()
        if (!html || html === '<p><br></p>') {
            msg.warning('请输入内容')
            return
        }

        const contentItems = htmlToContentItems(html)
        if (contentItems.length === 0) {
            msg.warning('请输入内容')
            return
        }

        // 保存消息用于显示（纯文本）
        const displayContent = htmlToText(html) || '[图片]'
        const userMessage: ChatMessage = { role: 'user', content: displayContent }
        setMessages(prev => [...prev, userMessage])
        setSending(true)
        editor?.setHtml('')

        try {
            // 发送多模态消息（含 base64 图片）
            const allMessages: ChatMessage[] = [...messages, { role: 'user', content: contentItems }]
            const result = await aiAssistantApi.sendMessage({
                messages: allMessages,
                temperature: 0.7,
                maxTokens: 2048
            })

            if (result.code === 200 && result.data) {
                const reply = result.data.choices?.[0]?.message?.content || ''
                setMessages(prev => [...prev, { role: 'assistant', content: reply }])
            } else {
                msg.error(result.message || '请求失败，请稍后重试')
            }
        } catch {
            msg.error('请求失败，请检查后端服务')
        } finally {
            setSending(false)
        }
    }, [editor, messages, msg])

    // WebSocket 模式发送
    const sendWs = useCallback(() => {
        if (!wsRef.current) {
            msg.error('WebSocket 未连接')
            return
        }

        const html = editor?.getHtml().trim()
        if (!html || html === '<p><br></p>') {
            msg.warning('请输入内容')
            return
        }

        const contentItems = htmlToContentItems(html)
        if (contentItems.length === 0) {
            msg.warning('请输入内容')
            return
        }

        const displayContent = htmlToText(html) || '[图片]'
        setMessages(prev => [...prev, { role: 'user', content: displayContent }])
        setMessages(prev => [...prev, { role: 'assistant', content: '' }])
        setSending(true)
        streamingRef.current = true
        editor?.setHtml('')

        // WebSocket 发送：将图片转为文本格式发送
        const textContent = contentItems.map(item => {
            if (item.type === 'text') return item.text || ''
            if (item.type === 'image_url') return `[图片] ${item.image_url?.url || ''}`
            return ''
        }).join('\n')

        wsRef.current.sendMessage(textContent)
    }, [editor, msg])

    const handleSend = useCallback(() => {
        if (mode === 'ws') {
            sendWs()
        } else {
            sendRest()
        }
    }, [mode, sendWs, sendRest])

    const handleStop = useCallback(() => {
        if (mode === 'ws' && wsRef.current) {
            wsRef.current.stop()
        }
    }, [mode])

    const handleClear = useCallback(() => {
        setMessages([])
        editor?.setHtml('')
        if (mode === 'ws' && wsRef.current) {
            wsRef.current.clear()
        }
    }, [editor, mode])

    const handleKeyDown = useCallback(
        (e: React.KeyboardEvent) => {
            if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
                e.preventDefault()
                handleSend()
            }
        },
        [handleSend]
    )

    useEffect(() => {
        return () => {
            editor?.destroy()
        }
    }, [editor])

    return (
        <div className="ai-assistant-page">
            <div className="ai-assistant-container">
                <section className="ai-assistant-card">
                    {/* Header */}
                    <div className="ai-assistant-hero">
                        <div>
                            <div className="ai-assistant-kicker">
                                <Icon icon="solar:chat-round-dots-bold-duotone" width={20} color="#8b5cf6" />
                                AI Assistant
                            </div>
                            <h1 className="ai-assistant-title">AI 助手</h1>
                            <p className="ai-assistant-desc">
                                基于 Agnes-2.0-Flash 模型的智能对话助手，支持富文本输入。
                            </p>
                        </div>
                        <div className="ai-assistant-header-actions">
                            <div className="ai-assistant-mode-switch">
                                <span className={mode === 'rest' ? 'active' : ''}>同步</span>
                                <Switch
                                    size="small"
                                    checked={mode === 'ws'}
                                    onChange={handleModeChange}
                                    checkedChildren="流式"
                                    unCheckedChildren="同步"
                                />
                                <span className={mode === 'ws' ? 'active' : ''}>流式</span>
                            </div>
                            {mode === 'ws' && sending && (
                                <Button
                                    size="small"
                                    icon={<Icon icon="solar:stop-bold" width={16} />}
                                    onClick={handleStop}
                                >
                                    停止生成
                                </Button>
                            )}
                            <Button
                                icon={<Icon icon="solar:trash-bin-trash-bold" width={18} />}
                                onClick={handleClear}
                            >
                                清空对话
                            </Button>
                        </div>
                    </div>

                    {/* Messages area */}
                    <div className="ai-assistant-messages">
                        {messages.length === 0 && (
                            <div className="ai-assistant-empty">
                                <Icon icon="solar:chat-round-dots-bold-duotone" width={48} color="#cbd5e1" />
                                <p>开始与 Agnes-2.0-Flash 对话吧</p>
                                <span className="ai-assistant-empty-hint">支持富文本输入，Ctrl+Enter 发送</span>
                            </div>
                        )}

                        {messages.map((item, index) => (
                            <div
                                key={index}
                                className={`ai-assistant-message ai-assistant-message-${item.role}`}
                            >
                                <div className="ai-assistant-avatar">
                                    <Icon
                                        icon={
                                            item.role === 'user'
                                                ? 'solar:user-bold-duotone'
                                                : 'solar:chat-round-dots-bold-duotone'
                                        }
                                        width={20}
                                    />
                                </div>
                                <div className="ai-assistant-bubble">
                                    <div className="ai-assistant-content">{item.content}</div>
                                </div>
                            </div>
                        ))}

                        {sending && mode === 'rest' && (
                            <div className="ai-assistant-message ai-assistant-message-assistant">
                                <div className="ai-assistant-avatar">
                                    <Icon icon="solar:chat-round-dots-bold-duotone" width={20} />
                                </div>
                                <div className="ai-assistant-bubble">
                                    <div className="ai-assistant-loading">
                                        <span className="ai-assistant-dot" />
                                        <span className="ai-assistant-dot" />
                                        <span className="ai-assistant-dot" />
                                        <span>Agnes-2.0-Flash 正在思考...</span>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div ref={messagesEndRef} />
                    </div>

                    {/* Editor area */}
                    <div className="ai-assistant-input-area">
                        <div className="ai-assistant-toolbar-wrapper">
                            <Toolbar editor={editor} onCreated={setEditor} defaultConfig={{ excludeKeys: ['uploadImage'] }} mode="simple" />
                        </div>
                        <div className="ai-assistant-editor-wrapper" onKeyDown={handleKeyDown}>
                            <Editor
                                onCreated={setEditor}
                                defaultContent=""
                                mode="simple"
                                config={{}}
                            />
                        </div>
                        <div className="ai-assistant-actions">
                            <span className="ai-assistant-hint">
                                {mode === 'ws' ? '流式输出模式' : '同步输出模式'} · Ctrl+Enter 发送
                            </span>
                            <Button
                                type="primary"
                                icon={<Icon icon="solar:paper-plane-bold" width={18} />}
                                loading={sending && mode === 'rest'}
                                disabled={sending && mode === 'ws'}
                                onClick={handleSend}
                            >
                                {sending ? (mode === 'ws' ? '生成中...' : '发送中...') : '发送'}
                            </Button>
                        </div>
                    </div>
                </section>
            </div>
        </div>
    )
}

export default AiAssistant
