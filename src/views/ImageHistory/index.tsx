import { Icon } from '@iconify/react'
import { App, Button, Card, Empty, Modal, Popconfirm, Typography, message } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { aiImageApi, type TaskDetail, type TaskPageParams, type HistoryItem, type HistoryPageParams } from '@/api/ai-image'
import './index.css'

const { Text } = Typography

const statusLabel: Record<string, string> = {
    PENDING: '排队中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败'
}

const statusTagClass: Record<string, string> = {
    PENDING: 'pending',
    PROCESSING: 'processing',
    COMPLETED: 'completed',
    FAILED: 'failed'
}

const ImageHistory = () => {
    const { message: msg } = App.useApp()
    const navigate = useNavigate()
    const [tab, setTab] = useState<'tasks' | 'saved'>('tasks')
    const [historyType, setHistoryType] = useState<string>('all')

    // 任务列表状态
    const [taskRecords, setTaskRecords] = useState<TaskDetail[]>([])
    const [taskTotal, setTaskTotal] = useState(0)
    const [taskPageNum, setTaskPageNum] = useState(1)
    const [taskPageSize, setTaskPageSize] = useState(12)
    const [taskLoading, setTaskLoading] = useState(false)

    // 已保存列表状态
    const [savedRecords, setSavedRecords] = useState<HistoryItem[]>([])
    const [savedTotal, setSavedTotal] = useState(0)
    const [savedPageNum, setSavedPageNum] = useState(1)
    const [savedPageSize, setSavedPageSize] = useState(12)
    const [savedLoading, setSavedLoading] = useState(false)

    const [previewImage, setPreviewImage] = useState<string>('')
    const [previewOpen, setPreviewOpen] = useState(false)

    // 获取任务列表
    const fetchTasks = useCallback(async (nextPageNum: number, nextPageSize: number, nextType: string) => {
        setTaskLoading(true)
        try {
            const params: TaskPageParams = { pageNum: nextPageNum, pageSize: nextPageSize }
            if (nextType !== 'all') {
                params.type = nextType as 'text2img' | 'img2img'
            }
            const result = await aiImageApi.getTaskPage(params)
            if (result.code !== 200) {
                msg.error(result.message || '获取任务列表失败')
                return
            }
            setTaskRecords(result.data.records || [])
            setTaskTotal(result.data.total || 0)
            setTaskPageNum(result.data.pageNum || nextPageNum)
            setTaskPageSize(result.data.pageSize || nextPageSize)
        } catch {
            msg.error('获取任务列表失败，请检查后端服务')
        } finally {
            setTaskLoading(false)
        }
    }, [msg])

    // 获取已保存列表
    const fetchSaved = useCallback(async (nextPageNum: number, nextPageSize: number) => {
        setSavedLoading(true)
        try {
            const params: HistoryPageParams = { pageNum: nextPageNum, pageSize: nextPageSize }
            const result = await aiImageApi.getImageHistoryPage(params)
            if (result.code !== 200) {
                msg.error(result.message || '获取保存列表失败')
                return
            }
            setSavedRecords(result.data.records || [])
            setSavedTotal(result.data.total || 0)
            setSavedPageNum(result.data.pageNum || nextPageNum)
            setSavedPageSize(result.data.pageSize || nextPageSize)
        } catch {
            msg.error('获取保存列表失败，请检查后端服务')
        } finally {
            setSavedLoading(false)
        }
    }, [msg])

    // 任务列表：切换 type 或 tab 时重新获取
    useEffect(() => {
        if (tab === 'tasks') {
            fetchTasks(1, taskPageSize, historyType)
        }
    }, [tab, fetchTasks, taskPageSize, historyType])

    // 已保存列表：切换 tab 时重新获取
    useEffect(() => {
        if (tab === 'saved') {
            fetchSaved(1, savedPageSize)
        }
    }, [tab, fetchSaved, savedPageSize])

    const handleDeleteTask = async (id: number) => {
        try {
            const result = await aiImageApi.deleteTask(id)
            if (result.code !== 200) {
                msg.error(result.message || '删除失败')
                return
            }
            msg.success('已删除')
            fetchTasks(taskPageNum, taskPageSize, historyType)
        } catch {
            msg.error('删除失败')
        }
    }

    const handleDelete = async (id: number) => {
        try {
            const result = await aiImageApi.deleteImageHistory(id)
            if (result.code !== 200) {
                msg.error(result.message || '删除失败')
                return
            }
            msg.success('已删除')
            fetchSaved(savedPageNum, savedPageSize)
        } catch {
            msg.error('删除失败')
        }
    }

    const formatDate = (dateStr: string) => {
        if (!dateStr) return '-'
        const d = new Date(dateStr.replace(' ', 'T'))
        return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
    }

    const typeFilter = [
        { label: '全部', value: 'all' },
        { label: '文生图', value: 'text2img' },
        { label: '图生图', value: 'img2img' }
    ]

    return (
        <div className="image-history-page">
            <div className="image-history-container">
                <div className="image-history-header">
                    <button className="image-history-back-btn" onClick={() => navigate('/ai/image')}>
                        <Icon icon="solar:arrow-left-bold" width={20} />
                    </button>
                    <div>
                        <h1 className="image-history-title">图片历史</h1>
                        <p className="image-history-desc">查看生成任务和已保存的图片</p>
                    </div>
                </div>

                <div className="image-history-tabs">
                    <div
                        className={`image-history-tab ${tab === 'tasks' ? 'active' : ''}`}
                        onClick={() => setTab('tasks')}
                    >
                        <Icon icon="solar:clock-circle-bold-duotone" width={18} />
                        任务记录
                    </div>
                    <div
                        className={`image-history-tab ${tab === 'saved' ? 'active' : ''}`}
                        onClick={() => setTab('saved')}
                    >
                        <Icon icon="solar:gallery-bold-duotone" width={18} />
                        已保存
                    </div>
                </div>

                {tab === 'tasks' && (
                    <>
                        <div className="image-history-filters">
                            {typeFilter.map(f => (
                                <button
                                    key={`type-${f.value}`}
                                    className={`image-history-filter ${historyType === f.value ? 'active' : ''}`}
                                    onClick={() => setHistoryType(f.value)}
                                >
                                    {f.label}
                                </button>
                            ))}
                        </div>

                        <div className="image-history-grid">
                            {taskRecords.map(item => (
                                <Card
                                    key={item.id}
                                    className="image-history-card"
                                    hoverable
                                    onClick={() => {
                                        if (item.imageUrl) {
                                            setPreviewImage(item.imageUrl)
                                            setPreviewOpen(true)
                                        }
                                    }}
                                    cover={
                                        <div className="image-history-card-cover">
                                            {item.imageUrl ? (
                                                <img src={item.imageUrl} alt={item.prompt} />
                                            ) : (
                                                <div className="image-history-no-image">
                                                    <Icon icon="solar:gallery-bold-duotone" width={32} color="#94a3b8" />
                                                    <div style={{ marginTop: 4, color: '#94a3b8', fontSize: 12 }}>
                                                        {statusLabel[item.status] || item.status}
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    }
                                >
                                    <div className="image-history-card-body">
                                        <div className="image-history-card-type">
                                            <span className={`image-history-tag ${item.type}`}>
                                                {item.type === 'text2img' ? '文生图' : '图生图'}
                                            </span>
                                            <span className={`image-history-status-tag ${statusTagClass[item.status] || ''}`}>
                                                {statusLabel[item.status] || item.status}
                                            </span>
                                            <span className="image-history-card-size">{item.size}</span>
                                        </div>
                                        <Text className="image-history-card-prompt" title={item.prompt}>
                                            {item.prompt}
                                        </Text>
                                        <div className="image-history-card-footer">
                                            <span className="image-history-card-date">{formatDate(item.createdAt)}</span>
                                            <Popconfirm
                                                title="确认删除这条记录？"
                                                okText="删除"
                                                cancelText="取消"
                                                onConfirm={(e) => {
                                                    e?.stopPropagation()
                                                    handleDeleteTask(item.id)
                                                }}
                                            >
                                                <Button
                                                    type="text"
                                                    danger
                                                    size="small"
                                                    icon={<Icon icon="solar:trash-bin-trash-bold" width={16} />}
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    删除
                                                </Button>
                                            </Popconfirm>
                                        </div>
                                    </div>
                                </Card>
                            ))}
                        </div>

                        {taskRecords.length === 0 && !taskLoading && (
                            <div className="image-history-empty">
                                <Empty description="暂无任务记录" />
                            </div>
                        )}

                        {taskTotal > taskPageSize && (
                            <div className="image-history-pagination">
                                <button
                                    disabled={taskPageNum <= 1 || taskLoading}
                                    onClick={() => fetchTasks(taskPageNum - 1, taskPageSize, historyType)}
                                >
                                    <Icon icon="solar:arrow-left-bold" width={16} />
                                </button>
                                <span className="image-history-page-info">
                                    第 {taskPageNum} / {Math.ceil(taskTotal / taskPageSize) || 1} 页，共 {taskTotal} 条
                                </span>
                                <button
                                    disabled={taskPageNum >= Math.ceil(taskTotal / taskPageSize) || taskLoading}
                                    onClick={() => fetchTasks(taskPageNum + 1, taskPageSize, historyType)}
                                >
                                    <Icon icon="solar:arrow-right-bold" width={16} />
                                </button>
                            </div>
                        )}
                    </>
                )}

                {tab === 'saved' && (
                    <>
                        <div className="image-history-grid">
                            {savedRecords.map(item => (
                                <Card
                                    key={item.id}
                                    className="image-history-card"
                                    hoverable
                                    onClick={() => {
                                        if (item.imageUrl) {
                                            setPreviewImage(item.imageUrl)
                                            setPreviewOpen(true)
                                        }
                                    }}
                                    cover={
                                        <div className="image-history-card-cover">
                                            {item.imageUrl ? (
                                                <img src={item.imageUrl} alt={item.prompt} />
                                            ) : (
                                                <div className="image-history-no-image">
                                                    <Icon icon="solar:gallery-bold-duotone" width={32} color="#94a3b8" />
                                                </div>
                                            )}
                                        </div>
                                    }
                                >
                                    <div className="image-history-card-body">
                                        <div className="image-history-card-type">
                                            <span className={`image-history-tag ${item.type}`}>
                                                {item.type === 'text2img' ? '文生图' : '图生图'}
                                            </span>
                                            <span className="image-history-card-size">{item.size}</span>
                                        </div>
                                        <Text className="image-history-card-prompt" title={item.prompt}>
                                            {item.prompt}
                                        </Text>
                                        <div className="image-history-card-footer">
                                            <span className="image-history-card-date">{formatDate(item.createdAt)}</span>
                                            <Popconfirm
                                                title="确认删除这条记录？"
                                                okText="删除"
                                                cancelText="取消"
                                                onConfirm={(e) => {
                                                    e?.stopPropagation()
                                                    handleDelete(item.id)
                                                }}
                                            >
                                                <Button
                                                    type="text"
                                                    danger
                                                    size="small"
                                                    icon={<Icon icon="solar:trash-bin-trash-bold" width={16} />}
                                                    onClick={(e) => e.stopPropagation()}
                                                >
                                                    删除
                                                </Button>
                                            </Popconfirm>
                                        </div>
                                    </div>
                                </Card>
                            ))}
                        </div>

                        {savedRecords.length === 0 && !savedLoading && (
                            <div className="image-history-empty">
                                <Empty description="暂无已保存的图片" />
                            </div>
                        )}

                        {savedTotal > savedPageSize && (
                            <div className="image-history-pagination">
                                <button
                                    disabled={savedPageNum <= 1 || savedLoading}
                                    onClick={() => fetchSaved(savedPageNum - 1, savedPageSize)}
                                >
                                    <Icon icon="solar:arrow-left-bold" width={16} />
                                </button>
                                <span className="image-history-page-info">
                                    第 {savedPageNum} / {Math.ceil(savedTotal / savedPageSize) || 1} 页，共 {savedTotal} 条
                                </span>
                                <button
                                    disabled={savedPageNum >= Math.ceil(savedTotal / savedPageSize) || savedLoading}
                                    onClick={() => fetchSaved(savedPageNum + 1, savedPageSize)}
                                >
                                    <Icon icon="solar:arrow-right-bold" width={16} />
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>

            <Modal
                open={previewOpen}
                footer={null}
                onCancel={() => setPreviewOpen(false)}
                width="80vw"
                style={{ maxHeight: '90vh' }}
            >
                <img src={previewImage} alt="" style={{ width: '100%', maxHeight: '80vh', objectFit: 'contain' }} />
            </Modal>
        </div>
    )
}

export default ImageHistory
