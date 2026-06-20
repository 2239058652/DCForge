import { Icon } from '@iconify/react'
import { App, Button, Collapse, Empty, Popconfirm, Tag } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { claudeApi, type ClaudeConversationDetail, type ClaudeMessage, type ClaudeMessageContentBlock } from '@/api/claude'
import { formatDateTime } from '@/utils/util-common'
import './ConversationDetail.css'

/** 从消息内容中提取纯文本 */
const extractText = (content: string | ClaudeMessageContentBlock[] | undefined): string => {
    if (!content) return ''
    if (typeof content === 'string') return content
    if (!Array.isArray(content)) return String(content)
    return content
        .filter((block): block is ClaudeMessageContentBlock & { type: 'text'; text: string } => block.type === 'text' && typeof block.text === 'string')
        .map((block) => block.text)
        .join('\n')
}

/** 检查消息是否包含图片块 */
const hasImageBlock = (content: string | ClaudeMessageContentBlock[] | undefined): boolean => {
    if (!Array.isArray(content)) return false
    return content.some((block) => block.type === 'image')
}

const ConversationDetail = () => {
    const { message } = App.useApp()
    const navigate = useNavigate()
    const { projectDirName, sessionId } = useParams<{ projectDirName: string; sessionId: string }>()
    const [detail, setDetail] = useState<ClaudeConversationDetail | null>(null)
    const [loading, setLoading] = useState(false)

    const fetchDetail = useCallback(async () => {
        if (!projectDirName || !sessionId) return
        setLoading(true)
        try {
            const result = await claudeApi.getConversationDetail(projectDirName, sessionId)
            if (result.code !== 200) {
                message.error(result.message || '获取对话详情失败')
                return
            }
            setDetail(result.data)
        } catch {
            message.error('获取对话详情失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }, [projectDirName, sessionId, message])

    useEffect(() => {
        fetchDetail()
    }, [fetchDetail])

    const handleDelete = async () => {
        if (!projectDirName || !sessionId) return
        try {
            const result = await claudeApi.deleteConversation(projectDirName, sessionId)
            if (result.code !== 200) {
                message.error(result.message || '删除对话失败')
                return
            }
            message.success('对话已删除')
            navigate(`/machine/claude/${projectDirName}/conversations`)
        } catch {
            message.error('删除对话失败')
        }
    }

    const renderMessage = (msg: ClaudeMessage, index: number) => {
        const isUser = msg.type === 'user'
        const isAssistant = msg.type === 'assistant'

        // 非对话消息（mode、permission-mode、file-history-snapshot 等）→ 折叠展示
        if (!isUser && !isAssistant) {
            const label = msg.type || 'system'
            const rawContent = JSON.stringify(msg, null, 2)
            return (
                <div key={msg.uuid || `msg-${index}`} className="msg-row msg-row-system">
                    <Collapse
                        size="small"
                        ghost
                        items={[
                            {
                                key: 'system',
                                label: (
                                    <span className="msg-system-label">
                                        <Icon icon="solar:settings-minimalistic-linear" width={14} />
                                        {label}
                                    </span>
                                ),
                                children: (
                                    <pre className="msg-system-content">{rawContent}</pre>
                                )
                            }
                        ]}
                    />
                </div>
            )
        }

        const content = msg.message?.content
        const textContent = extractText(content)
        const hasImage = hasImageBlock(content)

        return (
            <div
                key={msg.uuid || `msg-${index}`}
                className={`msg-row ${isUser ? 'msg-row-user' : 'msg-row-assistant'}`}
            >
                <div className={`msg-bubble ${isUser ? 'msg-bubble-user' : 'msg-bubble-assistant'}`}>
                    <div className="msg-role">
                        {isUser ? (
                            <><Icon icon="solar:user-linear" width={14} /> User</>
                        ) : (
                            <><Icon icon="solar:chat-round-dots-linear" width={14} /> Assistant</>
                        )}
                    </div>
                    {hasImage && (
                        <div className="msg-image-tag">
                            <Icon icon="solar:gallery-linear" width={14} />
                            [图片]
                        </div>
                    )}
                    <pre className="msg-text">{textContent}</pre>
                    {msg.timestamp && <div className="msg-time">{formatDateTime(msg.timestamp)}</div>}
                </div>
            </div>
        )
    }

    return (
        <div className="detail-page">
            <section className="detail-card">
                <div className="detail-hero">
                    <div>
                        <div className="detail-kicker">
                            <Icon icon="solar:chat-round-dots-bold-duotone" width={20} />
                            Conversation Detail
                        </div>
                        <h1 className="detail-title">对话详情</h1>
                    </div>

                    <div className="detail-actions">
                        <Button
                            icon={<Icon icon="solar:arrow-left-linear" />}
                            onClick={() => navigate(`/machine/claude/${projectDirName}/conversations`)}
                        >
                            返回对话列表
                        </Button>
                        <Popconfirm
                            title="确认删除此对话？"
                            okText="删除"
                            cancelText="取消"
                            onConfirm={handleDelete}
                        >
                            <Button danger icon={<Icon icon="solar:trash-bin-linear" />}>
                                删除此对话
                            </Button>
                        </Popconfirm>
                    </div>
                </div>

                {detail && (
                    <div className="detail-meta">
                        <div className="detail-meta-item">
                            <span className="detail-meta-label">项目路径</span>
                            <span className="detail-meta-value">{detail.projectPath}</span>
                        </div>
                        <div className="detail-meta-item">
                            <span className="detail-meta-label">会话ID</span>
                            <span className="detail-meta-value">{detail.sessionId}</span>
                        </div>
                        <div className="detail-meta-item">
                            <span className="detail-meta-label">消息总数</span>
                            <span className="detail-meta-value">{detail.messageCount}</span>
                        </div>
                        <div className="detail-meta-item">
                            <span className="detail-meta-label">开始时间</span>
                            <span className="detail-meta-value">{formatDateTime(detail.startTime)}</span>
                        </div>
                        <div className="detail-meta-item">
                            <span className="detail-meta-label">最后活跃</span>
                            <span className="detail-meta-value">{formatDateTime(detail.lastActivity)}</span>
                        </div>
                    </div>
                )}

                <div className="detail-content">
                    {loading ? (
                        <div className="detail-loading">加载中...</div>
                    ) : detail && detail.messages.length > 0 ? (
                        <div className="msg-list">
                            {detail.messages.map((msg, idx) => renderMessage(msg, idx))}
                        </div>
                    ) : (
                        <Empty description="暂无消息" />
                    )}
                </div>
            </section>
        </div>
    )
}

export default ConversationDetail
