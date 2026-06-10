import { Icon } from '@iconify/react'
import { App, Button, Empty, Form, Input, InputNumber, Modal, Popconfirm, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { dictionaryApi, type DictItem, type DictPayload, type DictPageParams } from '@/api/dictionary'
import './index.css'

const statusOptions = [
    { label: '启用', value: 1 },
    { label: '禁用', value: 0 }
]

const Dictionary = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<DictPayload>()
    const [dictList, setDictList] = useState<DictItem[]>([])
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingItem, setEditingItem] = useState<DictItem | null>(null)
    const [pageNum, setPageNum] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [total, setTotal] = useState(0)
    const [searchDictCode, setSearchDictCode] = useState('')

    const fetchList = useCallback(
        async (page: number, size: number, dictCode?: string) => {
            setLoading(true)
            try {
                const params: DictPageParams = { pageNum: page, pageSize: size }
                if (dictCode) params.dictCode = dictCode
                const result = await dictionaryApi.page(params)
                if (result.code !== 200) {
                    message.error(result.message || '获取字典列表失败')
                    return
                }
                setDictList(result.data?.records || [])
                setTotal(result.data?.total || 0)
                setPageNum(result.data?.pageNum || page)
                setPageSize(result.data?.pageSize || size)
            } catch {
                message.error('获取字典列表失败，请检查后端服务')
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
        fetchList(1, pageSize, searchDictCode)
    }

    const handleReset = () => {
        setSearchDictCode('')
        fetchList(1, pageSize)
    }

    const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
        const newPage = pagination.current || 1
        const newSize = pagination.pageSize || 10
        fetchList(newPage, newSize, searchDictCode)
    }

    const openAddModal = () => {
        form.resetFields()
        form.setFieldsValue({ status: 1, sortOrder: 0 })
        setEditingItem(null)
        setModalOpen(true)
    }

    const openEditModal = (record: DictItem) => {
        form.resetFields()
        form.setFieldsValue(record)
        setEditingItem(record)
        setModalOpen(true)
    }

    const handleSubmit = async () => {
        const values = await form.validateFields()
        setSaving(true)
        try {
            const payload: DictPayload = {
                ...values,
                dictCode: values.dictCode.trim(),
                dictLabel: values.dictLabel.trim(),
                dictValue: values.dictValue.trim()
            }
            if (editingItem) {
                payload.id = editingItem.id
            }
            const result = await dictionaryApi.save(payload)
            if (result.code !== 200) {
                message.error(result.message || (editingItem ? '修改字典项失败' : '新增字典项失败'))
                return
            }
            message.success(editingItem ? '字典项已修改' : '字典项已新增')
            setModalOpen(false)
            form.resetFields()
            setEditingItem(null)
            await fetchList(pageNum, pageSize, searchDictCode)
        } catch {
            message.error(editingItem ? '修改字典项失败' : '新增字典项失败')
        } finally {
            setSaving(false)
        }
    }

    const handleRemove = async (id: number) => {
        setLoading(true)
        try {
            const result = await dictionaryApi.remove(id)
            if (result.code !== 200) {
                message.error(result.message || '删除字典项失败')
                return
            }
            message.success('字典项已删除')
            await fetchList(pageNum, pageSize, searchDictCode)
        } catch {
            message.error('删除字典项失败')
        } finally {
            setLoading(false)
        }
    }

    const columns: ColumnsType<DictItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 80
        },
        {
            title: '字典编码',
            dataIndex: 'dictCode',
            width: 160,
            render: (value: string) => <Tag color="blue">{value}</Tag>
        },
        {
            title: '显示名',
            dataIndex: 'dictLabel',
            width: 160
        },
        {
            title: '存储值',
            dataIndex: 'dictValue',
            width: 160,
            render: (value: string) => <Tag color="cyan">{value}</Tag>
        },
        {
            title: '排序号',
            dataIndex: 'sortOrder',
            width: 100
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (value: number) => (
                <Tag color={value === 1 ? 'green' : 'red'}>{value === 1 ? '启用' : '禁用'}</Tag>
            )
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 190,
            render: (value?: string) => value || '-'
        },
        {
            title: '操作',
            width: 120,
            render: (_, record) => (
                <>
                    <Button type="link" onClick={() => openEditModal(record)}>
                        编辑
                    </Button>
                    <Popconfirm
                        title="确认删除该字典项？"
                        okText="删除"
                        cancelText="取消"
                        onConfirm={() => handleRemove(record.id)}
                    >
                        <Button type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </>
            )
        }
    ]

    return (
        <div className="dict-page">
            <section className="dict-card">
                <div className="dict-hero">
                    <div>
                        <div className="dict-kicker">
                            <Icon icon="solar:book-bold-duotone" width={20} />
                            Dictionary
                        </div>
                        <h1 className="dict-title">字典管理</h1>
                        <p className="dict-desc">管理系统字典数据，维护下拉选项、状态标签等配置项。</p>
                    </div>

                    <div className="dict-stats">
                        {[['字典总数', total]].map(([label, value]) => (
                            <div key={label} className="dict-stat">
                                <div className="dict-stat-label">{label}</div>
                                <div className="dict-stat-value">{value}</div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="dict-content">
                    <div className="dict-toolbar">
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                            <Input
                                placeholder="按字典编码搜索"
                                value={searchDictCode}
                                onChange={(e) => setSearchDictCode(e.target.value)}
                                onPressEnter={handleSearch}
                                style={{ width: 200 }}
                                allowClear
                            />
                            <Button type="primary" onClick={handleSearch}>
                                搜索
                            </Button>
                            <Button onClick={handleReset}>重置</Button>
                        </div>
                        <div className="dict-toolbar-actions">
                            <Button type="primary" icon={<Icon icon="solar:add-circle-bold" />} onClick={openAddModal}>
                                新增字典项
                            </Button>
                        </div>
                    </div>

                    <Table
                        rowKey="id"
                        loading={loading}
                        columns={columns}
                        dataSource={dictList}
                        bordered
                        locale={{ emptyText: <Empty description="暂无字典数据" /> }}
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

            <Modal
                title={editingItem ? '编辑字典项' : '新增字典项'}
                open={modalOpen}
                okText="保存"
                cancelText="取消"
                confirmLoading={saving}
                onCancel={() => setModalOpen(false)}
                onOk={handleSubmit}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" className="pt-3">
                    <Form.Item
                        name="dictCode"
                        label="字典编码"
                        rules={[{ required: true, message: '请输入字典编码' }]}
                    >
                        <Input maxLength={50} placeholder="例如 permission_code" />
                    </Form.Item>
                    <Form.Item
                        name="dictLabel"
                        label="显示名"
                        rules={[{ required: true, message: '请输入显示名' }]}
                    >
                        <Input maxLength={100} placeholder="例如 管理员" />
                    </Form.Item>
                    <Form.Item
                        name="dictValue"
                        label="存储值"
                        rules={[{ required: true, message: '请输入存储值' }]}
                    >
                        <Input maxLength={100} placeholder="例如 admin" />
                    </Form.Item>
                    <Form.Item
                        name="sortOrder"
                        label="排序号"
                        rules={[{ required: true, message: '请输入排序号' }]}
                    >
                        <InputNumber min={0} max={9999} style={{ width: '100%' }} placeholder="数字越小越靠前" />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select options={statusOptions} />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default Dictionary
