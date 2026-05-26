import { Icon } from '@iconify/react'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { interfacePermissionApi, type InterfacePermissionItem, type InterfacePermissionPayload } from '@/api/system'
import './index.css'

const httpMethodOptions = [
    { label: 'GET', value: 'GET' },
    { label: 'POST', value: 'POST' },
    { label: 'PUT', value: 'PUT' },
    { label: 'DELETE', value: 'DELETE' },
    { label: 'PATCH', value: 'PATCH' }
]

const permissionCodeOptions = [
    { label: 'note:add', value: 'note:add' },
    { label: 'note:delete', value: 'note:delete' },
    { label: 'note:detail', value: 'note:detail' },
    { label: 'note:list', value: 'note:list' },
    { label: 'note:update', value: 'note:update' },
    { label: 'permission:add', value: 'permission:add' },
    { label: 'permission:delete', value: 'permission:delete' },
    { label: 'permission:list', value: 'permission:list' },
    { label: 'permission:update', value: 'permission:update' },
    { label: 'role:add', value: 'role:add' },
    { label: 'role:assign-permission', value: 'role:assign-permission' },
    { label: 'role:delete', value: 'role:delete' },
    { label: 'role:list', value: 'role:list' },
    { label: 'role:update', value: 'role:update' },
    { label: 'system:admin', value: 'system:admin' },
    { label: 'user:assign-role', value: 'user:assign-role' },
    { label: 'user:list', value: 'user:list' }
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

    const fetchRules = useCallback(async () => {
        setLoading(true)
        try {
            const result = await interfacePermissionApi.list()
            if (result.code !== 200) {
                message.error(result.message || '获取接口权限规则失败')
                return
            }
            setRules(result.data || [])
        } catch {
            message.error('获取接口权限规则失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }, [message])

    useEffect(() => {
        fetchRules()
    }, [fetchRules])

    const openAddModal = () => {
        form.resetFields()
        form.setFieldsValue({ httpMethod: 'GET' })
        setModalOpen(true)
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
            const result = await interfacePermissionApi.add(payload)
            if (result.code !== 200) {
                message.error(result.message || '新增规则失败')
                return
            }
            message.success('规则已新增')
            setModalOpen(false)
            form.resetFields()
            await fetchRules()
        } catch {
            message.error('新增规则失败')
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
            await fetchRules()
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
            width: 80
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
            width: 100,
            render: (_, record) => (
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
                        {[['规则总数', rules.length]].map(([label, value]) => (
                            <div key={label} className="system-stat">
                                <div className="system-stat-label">{label}</div>
                                <div className="system-stat-value">{value}</div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="system-content">
                    <div className="system-toolbar">
                        <span style={{ color: '#64748b', fontSize: 13 }}>共 {rules.length} 条规则</span>
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
                        locale={{ emptyText: <Empty description="暂无接口权限规则" /> }}
                        pagination={{
                            pageSize: 10,
                            showSizeChanger: true,
                            showTotal: (total) => `共 ${total} 条`
                        }}
                    />
                </div>
            </section>

            <Modal
                title="新增接口权限规则"
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
                        <Select placeholder="请选择权限编码" options={permissionCodeOptions} />
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
