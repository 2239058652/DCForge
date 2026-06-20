import { Icon } from '@iconify/react'
import { App, Button, Empty, Input, Popconfirm, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { claudeApi, type ClaudeProjectItem, type ClaudeProjectPageParams } from '@/api/claude'
import { formatDateTime } from '@/utils/util-common'
import './index.css'

const Claude = () => {
    const { message } = App.useApp()
    const navigate = useNavigate()
    const [projectList, setProjectList] = useState<ClaudeProjectItem[]>([])
    const [loading, setLoading] = useState(false)
    const [pageNum, setPageNum] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [total, setTotal] = useState(0)
    const [searchPath, setSearchPath] = useState('')

    const fetchList = useCallback(
        async (page: number, size: number, path?: string) => {
            setLoading(true)
            try {
                const params: ClaudeProjectPageParams = { pageNum: page, pageSize: size }
                if (path) params.projectPath = path
                const result = await claudeApi.getProjects(params)
                if (result.code !== 200) {
                    message.error(result.message || '获取项目列表失败')
                    return
                }
                setProjectList(result.data?.records || [])
                setTotal(result.data?.total || 0)
                setPageNum(result.data?.pageNum || page)
                setPageSize(result.data?.pageSize || size)
            } catch {
                message.error('获取项目列表失败，请检查后端服务')
            } finally {
                setLoading(false)
            }
        },
        [message]
    )

    useEffect(() => {
        fetchList(1, pageSize)
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    const handleSearch = () => {
        fetchList(1, pageSize, searchPath)
    }

    const handleReset = () => {
        setSearchPath('')
        fetchList(1, pageSize)
    }

    const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
        const newPage = pagination.current || 1
        const newSize = pagination.pageSize || 10
        fetchList(newPage, newSize, searchPath)
    }

    const handleDelete = async (dirName: string) => {
        setLoading(true)
        try {
            const result = await claudeApi.deleteProject(dirName)
            if (result.code !== 200) {
                message.error(result.message || '删除项目失败')
                return
            }
            message.success('项目已删除')
            await fetchList(pageNum, pageSize, searchPath)
        } catch {
            message.error('删除项目失败')
        } finally {
            setLoading(false)
        }
    }

    const columns: ColumnsType<ClaudeProjectItem> = [
        {
            title: '项目路径',
            dataIndex: 'projectPath',
            render: (value: string) => <Tag color="blue">{value}</Tag>
        },
        {
            title: '对话数量',
            dataIndex: 'conversationCount'
        },
        {
            title: '最后修改',
            dataIndex: 'lastModified',
            render: (value: string) => formatDateTime(value)
        },
        {
            title: '最后对话',
            dataIndex: 'lastConversationTime',
            render: (value: string) => formatDateTime(value)
        },
        {
            title: '操作',
            render: (_, record) => (
                <>
                    <Button type="link" onClick={() => navigate(`/machine/claude/${record.dirName}/conversations`)}>
                        查看对话
                    </Button>
                    <Popconfirm
                        title="确认删除该项目？"
                        okText="删除"
                        cancelText="取消"
                        onConfirm={() => handleDelete(record.dirName)}
                    >
                        <Button type="link" danger>
                            删除项目
                        </Button>
                    </Popconfirm>
                </>
            )
        }
    ]

    return (
        <div className="claude-page">
            <section className="claude-card">
                <div className="claude-hero">
                    <div>
                        <div className="claude-kicker">
                            <Icon icon="solar:settings-bold-duotone" width={20} />
                            Claude
                        </div>
                        <h1 className="claude-title">Claude 管理</h1>
                        <p className="claude-desc">管理本机 Claude Code 的项目与对话数据。</p>
                    </div>

                    <div className="claude-stats">
                        <div className="claude-stat">
                            <div className="claude-stat-label">项目总数</div>
                            <div className="claude-stat-value">{total}</div>
                        </div>
                    </div>
                </div>

                <div className="claude-content">
                    <div className="claude-toolbar">
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                            <Input
                                placeholder="按项目路径搜索"
                                value={searchPath}
                                onChange={(e) => setSearchPath(e.target.value)}
                                onPressEnter={handleSearch}
                                style={{ width: 260 }}
                                allowClear
                            />
                            <Button type="primary" onClick={handleSearch}>
                                搜索
                            </Button>
                            <Button onClick={handleReset}>重置</Button>
                        </div>
                    </div>

                    <Table
                        rowKey="dirName"
                        loading={loading}
                        columns={columns}
                        dataSource={projectList}
                        bordered
                        locale={{ emptyText: <Empty description="暂无项目数据" /> }}
                        pagination={{
                            current: pageNum,
                            pageSize: pageSize,
                            total: total,
                            showSizeChanger: true,
                            showTotal: (t) => `共 ${t} 条`
                        }}
                        onChange={handleTableChange}
                    />
                </div>
            </section>
        </div>
    )
}

export default Claude
