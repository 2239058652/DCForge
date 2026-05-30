import { Icon } from '@iconify/react'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import {
    interfacePermissionApi,
    type InterfacePermissionItem,
    type InterfacePermissionPayload,
    type PermissionCodeItem,
    type InterfacePageParams
} from '@/api/system'
import './index.css'

const httpMethodOptions = [
    { label: 'GET', value: 'GET' },
    { label: 'POST', value: 'POST' },
    { label: 'PUT', value: 'PUT' },
    { label: 'DELETE', value: 'DELETE' },
    { label: 'PATCH', value: 'PATCH' }
]

const methodColorMap: Record<string, string> = {
    GET: 'green',
    POST: 'blue',
    PUT: 'orange',
    DELETE: 'red',
    PATCH: 'purple'
}

const System = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<InterfacePermissionPayload>()
    const [rules, setRules] = useState<InterfacePermissionItem[]>([])
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [refreshing, setRefreshing] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingRule, setEditingRule] = useState<InterfacePermissionItem | null>(null)
    const [permissionCodes, setPermissionCodes] = useState<PermissionCodeItem[]>([])
    const [loadingPermissionCodes, setLoadingPermissionCodes] = useState(false)
    const [pageNum, setPageNum] = useState(1)
    const [pageSize, setPageSize] = useState(10)
    const [total, setTotal] = useState(0)
    const [searchName, setSearchName] = useState('')
    const [searchType, setSearchType] = useState<number | undefined>(undefined)

    const fetchRules = useCallback(
        async (page: number, size: number, name?: string, type?: number) => {
            setLoading(true)
            try {
                const params: InterfacePageParams = { pageNum: page, pageSize: size }
                if (name) params.name = name
                if (type !== undefined) params.type = type
                const result = await interfacePermissionApi.page(params)
                if (result.code !== 200) {
                    message.error(result.message || '获取接口权限规则失败')
                    return
                }
                setRules(result.data?.records || [])
                setTotal(result.data?.total || 0)
                setPageNum(result.data?.pageNum || page)
                setPageSize(result.data?.pageSize || size)
            } catch {
                message.error('获取接口权限规则失败，请检查后端服务')
            } finally {
                setLoading(false)
            }
        },
        [message]
    )

    useEffect(() => {
        fetchRules(1, pageSize)
    }, []) // eslint-disable-line react-hooks/exhaustive-deps

    const handleSearch = () => {
        fetchRules(1, pageSize, searchName, searchType)
    }

    const handleReset = () => {
        setSearchName('')
        setSearchType(undefined)
        fetchRules(1, pageSize)
    }

    const handleTableChange = (pagination: { current?: number; pageSize?: number }) => {
        const newPage = pagination.current || 1
        const newSize = pagination.pageSize || 10
        fetchRules(newPage, newSize, searchName, searchType)
    }

    const fetchPermissionCodes = useCallback(async () => {
        setLoadingPermissionCodes(true)
        try {
            const result = await interfacePermissionApi.getPermissionCodes()
            if (result.code !== 200) {
                message.error(result.message || '获取权限码列表失败')
                return
            }
            setPermissionCodes(result.data || [])
        } catch {
            message.error('获取权限码列表失败，请检查后端服务')
        } finally {
            setLoadingPermissionCodes(false)
        }
    }, [message])

    const openAddModal = () => {
        form.resetFields()
        form.setFieldsValue({ httpMethod: 'GET' })
        setEditingRule(null)
        setModalOpen(true)
        fetchPermissionCodes()
    }

    const openEditModal = (record: InterfacePermissionItem) => {
        form.resetFields()
        form.setFieldsValue(record)
        setEditingRule(record)
        setModalOpen(true)
        fetchPermissionCodes()
    }

    const handleSubmit = async () => {
        const values = await form.validateFields()
        setSaving(true)
        try {
            const payload: InterfacePermissionPayload = {
                ...values,
                urlPattern: values.urlPattern.trim(),
                permissionCode: values.permissionCode.trim(),
                description: values.description?.trim()
            }
            if (editingRule) {
                payload.id = editingRule.id
            }
            const result = await interfacePermissionApi.add(payload)
            if (result.code !== 200) {
                message.error(result.message || (editingRule ? '修改规则失败' : '新增规则失败'))
                return
            }
            message.success(editingRule ? '规则已修改' : '规则已新增')
            setModalOpen(false)
            form.resetFields()
            setEditingRule(null)
            await fetchRules(pageNum, pageSize, searchName, searchType)
        } catch {
            message.error(editingRule ? '修改规则失败' : '新增规则失败')
        } finally {
            setSaving(false)
        }
    }

    const handleRemove = async (id: number) => {
        setLoading(true)
        try {
            const result = await interfacePermissionApi.remove(id)
            if (result.code !== 200) {
                message.error(result.message || '删除规则失败')
                return
            }
            message.success('规则已删除')
            await fetchRules(pageNum, pageSize, searchName, searchType)
        } catch {
            message.error('删除规则失败')
        } finally {
            setLoading(false)
        }
    }

    const handleRefresh = async () => {
        setRefreshing(true)
        try {
            const result = await interfacePermissionApi.refresh()
            if (result.code !== 200) {
                message.error(result.message || '刷新缓存失败')
                return
            }
            message.success('权限缓存已刷新')
        } catch {
            message.error('刷新缓存失败')
        } finally {
            setRefreshing(false)
        }
    }

    const columns: ColumnsType<InterfacePermissionItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 60
        },
        {
            title: 'HTTP 方法',
            dataIndex: 'httpMethod',
            width: 120,
            render: (value: string) => <Tag color={methodColorMap[value] || 'default'}>{value}</Tag>
        },
        {
            title: 'URL 匹配模式',
            dataIndex: 'urlPattern',
            ellipsis: true
        },
        {
            title: '所需权限编码',
            dataIndex: 'permissionCode',
            width: 220,
            render: (value: string) => <Tag color="cyan">{value}</Tag>
        },
        {
            title: '描述',
            dataIndex: 'description',
            ellipsis: true,
            render: (value?: string) => value || '-'
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            width: 190,
            render: (value?: string) => value || '-'
        },
        {
            title: '操作',
            width: 180,
            render: (_, record) => (
                <>
                    <Button type="link" onClick={() => openEditModal(record)}>
                        编辑
                    </Button>
                    <Popconfirm
                        title="确认删除该规则？"
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
        <div className="system-page">
            <section className="system-card">
                <div className="system-hero">
                    <div>
                        <div className="system-kicker">
                            <Icon icon="solar:settings-bold-duotone" width={20} />
                            System Workspace
                        </div>
                        <h1 className="system-title">动态接口权限</h1>
                        <p className="system-desc">管理运行时接口访问规则，修改后需刷新缓存方可生效。</p>
                    </div>

                    <div className="system-stats">
                        {[['规则总数', total]].map(([label, value]) => (
                            <div key={label} className="system-stat">
                                <div className="system-stat-label">{label}</div>
                                <div className="system-stat-value">{value}</div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="system-content">
                    <div className="system-toolbar">
                        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                            <Input
                                placeholder="按名称搜索"
                                value={searchName}
                                onChange={(e) => setSearchName(e.target.value)}
                                onPressEnter={handleSearch}
                                style={{ width: 180 }}
                                allowClear
                            />
                            <Select
                                placeholder="按类型筛选"
                                value={searchType}
                                onChange={(v) => setSearchType(v)}
                                allowClear
                                style={{ width: 150 }}
                                options={[
                                    { label: '类型 1', value: 1 },
                                    { label: '类型 2', value: 2 }
                                ]}
                            />
                            <Button type="primary" onClick={handleSearch}>
                                搜索
                            </Button>
                            <Button onClick={handleReset}>重置</Button>
                        </div>
                        <div className="system-toolbar-actions">
                            <Button
                                icon={<Icon icon="solar:refresh-bold" />}
                                loading={refreshing}
                                onClick={handleRefresh}
                            >
                                刷新缓存
                            </Button>
                            <Button type="primary" icon={<Icon icon="solar:add-circle-bold" />} onClick={openAddModal}>
                                新增规则
                            </Button>
                        </div>
                    </div>

                    <Table
                        rowKey="id"
                        loading={loading}
                        columns={columns}
                        dataSource={rules}
                        bordered
                        locale={{ emptyText: <Empty description="暂无接口权限规则" /> }}
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
                title={editingRule ? '编辑接口权限规则' : '新增接口权限规则'}
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
                        name="httpMethod"
                        label="HTTP 方法"
                        rules={[{ required: true, message: '请选择 HTTP 方法' }]}
                    >
                        <Select options={httpMethodOptions} />
                    </Form.Item>
                    <Form.Item
                        name="urlPattern"
                        label="URL 匹配模式"
                        rules={[{ required: true, message: '请输入 URL 匹配模式' }]}
                    >
                        <Input maxLength={200} placeholder="例如 /notes/list 或 /api/staff/**" />
                    </Form.Item>
                    <Form.Item
                        name="permissionCode"
                        label="所需权限编码"
                        rules={[{ required: true, message: '请选择权限编码' }]}
                    >
                        <Select
                            placeholder="请选择权限编码"
                            loading={loadingPermissionCodes}
                            options={permissionCodes}
                            showSearch={{
                                filterOption: (input, option) =>
                                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                            }}
                        />
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input maxLength={200} placeholder="可选，备注此规则用途" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default System
