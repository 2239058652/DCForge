import { Icon } from '@iconify/react'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Space, Table } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { noteApi, type NoteItem } from '@/api/note'

interface NoteForm {
    content: string
}

const Notes = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<NoteForm>()
    const [notes, setNotes] = useState<NoteItem[]>([])
    const [total, setTotal] = useState(0)
    const [pageNum, setPageNum] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [keyword, setKeyword] = useState('')
    const [queryContent, setQueryContent] = useState('')
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingNote, setEditingNote] = useState<NoteItem | null>(null)
    const [detailNote, setDetailNote] = useState<NoteItem | null>(null)

    const fetchNotes = async (nextPageNum = pageNum, nextPageSize = pageSize, nextContent = queryContent) => {
        setLoading(true)
        try {
            const result = await noteApi.page({
                pageNum: nextPageNum,
                pageSize: nextPageSize,
                content: nextContent
            })

            if (result.code !== 200) {
                message.error(result.message || '获取 Note 列表失败')
                return
            }

            setNotes(result.data.records || [])
            setTotal(result.data.total || 0)
            setPageNum(result.data.pageNum || nextPageNum)
            setPageSize(result.data.pageSize || nextPageSize)
        } catch {
            message.error('获取 Note 列表失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchNotes(1, pageSize, '')
    }, [])

    const openCreateModal = () => {
        setEditingNote(null)
        form.resetFields()
        setModalOpen(true)
    }

    const openEditModal = (note: NoteItem) => {
        setEditingNote(note)
        form.setFieldsValue({ content: note.content })
        setModalOpen(true)
    }

    const openDetailModal = async (id: number) => {
        setLoading(true)
        try {
            const result = await noteApi.find(id)
            if (result.code !== 200) {
                message.error(result.message || '查询 Note 失败')
                return
            }
            setDetailNote(result.data)
        } catch {
            message.error('查询 Note 失败')
        } finally {
            setLoading(false)
        }
    }

    const handleSubmit = async () => {
        const values = await form.validateFields()
        setSaving(true)

        try {
            const result = editingNote ? await noteApi.update(editingNote.id, values) : await noteApi.add(values)
            if (result.code !== 200) {
                message.error(result.message || '保存 Note 失败')
                return
            }

            message.success(editingNote ? 'Note 已更新' : 'Note 已添加')
            setModalOpen(false)
            form.resetFields()
            await fetchNotes(editingNote ? pageNum : 1, pageSize, queryContent)
        } catch {
            message.error('保存 Note 失败')
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (id: number) => {
        setLoading(true)
        try {
            const result = await noteApi.delete(id)
            if (result.code !== 200) {
                message.error(result.message || '删除 Note 失败')
                return
            }

            message.success('Note 已删除')
            const nextPageNum = notes.length === 1 && pageNum > 1 ? pageNum - 1 : pageNum
            await fetchNotes(nextPageNum, pageSize, queryContent)
        } catch {
            message.error('删除 Note 失败')
        } finally {
            setLoading(false)
        }
    }

    const handleSearch = () => {
        const nextContent = keyword.trim()
        setQueryContent(nextContent)
        fetchNotes(1, pageSize, nextContent)
    }

    const handleTableChange = (pagination: TablePaginationConfig) => {
        const nextPageNum = pagination.current || 1
        const nextPageSize = pagination.pageSize || 10
        fetchNotes(nextPageNum, nextPageSize, queryContent)
    }

    const columns: ColumnsType<NoteItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 80
        },
        {
            title: '内容',
            dataIndex: 'content',
            ellipsis: true
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 190,
            render: (value?: string) => value || '-'
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            width: 190,
            render: (value?: string) => value || '-'
        },
        {
            title: '操作',
            width: 220,
            render: (_, record) => (
                <Space size="small">
                    <Button type="link" onClick={() => openDetailModal(record.id)}>
                        查询
                    </Button>
                    <Button type="link" onClick={() => openEditModal(record)}>
                        修改
                    </Button>
                    <Popconfirm title="确认删除这条 note？" okText="删除" cancelText="取消" onConfirm={() => handleDelete(record.id)}>
                        <Button type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    return (
        <div className="notes-page">
            <div className="notes-container">
                <section className="notes-card">
                    <div className="notes-hero">
                        <div>
                            <div className="notes-kicker">
                                <Icon icon="solar:notes-bold-duotone" width={20} />
                                Note Workspace
                            </div>
                            <h1 className="notes-title">Note 管理</h1>
                            <p className="notes-desc">数据来自本地后端服务 localhost:5273。</p>
                        </div>

                        <div className="notes-stats">
                            {[
                                ['总数', total],
                                ['页码', pageNum],
                                ['每页', pageSize]
                            ].map(([label, value]) => (
                                <div key={label} className="notes-stat">
                                    <div className="notes-stat-label">{label}</div>
                                    <div className="notes-stat-value">{value}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="notes-toolbar">
                        <Input.Search
                            allowClear
                            className="notes-search"
                            placeholder="按内容搜索 note"
                            enterButton="查询"
                            prefix={<Icon icon="solar:magnifer-linear" color="#94a3b8" />}
                            value={keyword}
                            onChange={(event) => setKeyword(event.target.value)}
                            onSearch={handleSearch}
                        />
                        <Button type="primary" icon={<Icon icon="solar:add-circle-bold" />} onClick={openCreateModal}>
                            添加 Note
                        </Button>
                    </div>

                    <div className="notes-table-wrap">
                        <Table
                            rowKey="id"
                            loading={loading}
                            columns={columns}
                            dataSource={notes}
                            locale={{ emptyText: <Empty description="暂无 note" /> }}
                            pagination={{
                                current: pageNum,
                                pageSize,
                                total,
                                showSizeChanger: true,
                                showTotal: (value) => `共 ${value} 条`
                            }}
                            onChange={handleTableChange}
                        />
                    </div>
                </section>
            </div>

            <Modal
                title={editingNote ? '修改 Note' : '添加 Note'}
                open={modalOpen}
                okText={editingNote ? '保存修改' : '添加'}
                cancelText="取消"
                confirmLoading={saving}
                onCancel={() => setModalOpen(false)}
                onOk={handleSubmit}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" className="pt-3">
                    <Form.Item name="content" label="内容" rules={[{ required: true, message: '请输入 note 内容' }]}>
                        <Input.TextArea rows={6} maxLength={500} showCount placeholder="请输入 note 内容" />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal title="Note 详情" open={!!detailNote} footer={null} onCancel={() => setDetailNote(null)} destroyOnHidden>
                {detailNote && (
                    <div>
                        <div className="note-detail-box">{detailNote.content}</div>
                        <div className="note-detail-meta">
                            <span>ID：{detailNote.id}</span>
                            <span>创建：{detailNote.createdAt || '-'}</span>
                            <span>更新：{detailNote.updatedAt || '-'}</span>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    )
}

export default Notes
