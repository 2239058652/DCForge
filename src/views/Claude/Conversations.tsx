import { Icon } from '@iconify/react'
import { App, Button, Empty, Popconfirm, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { claudeApi, type ClaudeConversationItem } from '@/api/claude'
import { formatDateTime } from '@/utils/util-common'
import './Conversations.css'

const Conversations = () => {
    const { message } = App.useApp()
    const navigate = useNavigate()
    const { projectDirName } = useParams<{ projectDirName: string }>()
    const [list, setList] = useState<ClaudeConversationItem[]>([])
    const [loading, setLoading] = useState(false)

    const fetchList = useCallback(async () => {
        if (!projectDirName) return
        setLoading(true)
        try {
            const result = await claudeApi.getConversations(projectDirName)
            if (result.code !== 200) {
                message.error(result.message || '获取对话列表失败')
                return
            }
            const records = result.data || []
            records.sort((a, b) => (b.lastActivity || '').localeCompare(a.lastActivity || ''))
            setList(records)
        } catch {
            message.error('获取对话列表失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }, [projectDirName, message])

    useEffect(() => {
        fetchList()
    }, [fetchList])

    const handleDelete = async (sessionId: string) => {
        if (!projectDirName) return
        setLoading(true)
        try {
            const result = await claudeApi.deleteConversation(projectDirName, sessionId)
            if (result.code !== 200) {
                message.error(result.message || '删除对话失败')
                return
            }
            message.success('对话已删除')
            await fetchList()
        } catch {
            message.error('删除对话失败')
        } finally {
            setLoading(false)
        }
    }

    const shortId = (id: string) => (id.length > 12 ? `${id.slice(0, 12)}...` : id)

    const columns: ColumnsType<ClaudeConversationItem> = [
        {
            title: '会话ID',
            dataIndex: 'sessionId',
            render: (value: string) => <span style={{ fontFamily: 'monospace', fontSize: 13 }}>{shortId(value)}</span>
        },
        {
            title: '标题',
            dataIndex: 'title',
            ellipsis: true
        },
        {
            title: '消息数',
            dataIndex: 'messageCount'
        },
        {
            title: '最后活跃',
            dataIndex: 'lastActivity',
            render: (value: string) => formatDateTime(value)
        },
        {
            title: '操作',
            render: (_, record) => (
                <>
                    <Button
                        type="link"
                        onClick={() => navigate(`/machine/claude/${projectDirName}/conversations/${record.sessionId}`)}
                    >
                        查看详情
                    </Button>
                    <Popconfirm
                        title="确认删除该对话？"
                        okText="删除"
                        cancelText="取消"
                        onConfirm={() => handleDelete(record.sessionId)}
                    >
                        <Button type="link" danger>
                            删除对话
                        </Button>
                    </Popconfirm>
                </>
            )
        }
    ]

    return (
        <div className="conv-page">
            <section className="conv-card">
                <div className="conv-hero">
                    <div>
                        <div className="conv-kicker">
                            <Icon icon="solar:chat-round-dots-bold-duotone" width={20} />
                            Conversations
                        </div>
                        <h1 className="conv-title">对话列表</h1>
                        <p className="conv-desc">项目目录：{projectDirName}</p>
                    </div>

                    <div className="conv-actions">
                        <Button icon={<Icon icon="solar:arrow-left-linear" />} onClick={() => navigate('/machine/claude')}>
                            返回项目列表
                        </Button>
                    </div>
                </div>

                <div className="conv-content">
                    <Table
                        rowKey="sessionId"
                        loading={loading}
                        columns={columns}
                        dataSource={list}
                        bordered
                        locale={{ emptyText: <Empty description="暂无对话数据" /> }}
                        pagination={{
                            pageSize: 20,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`
                        }}
                    />
                </div>
            </section>
        </div>
    )
}

export default Conversations
