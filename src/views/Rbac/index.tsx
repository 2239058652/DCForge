import { Icon } from '@iconify/react'
import { App, Button, Empty, Form, Input, Modal, Popconfirm, Select, Space, Table, Tabs, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { rbacApi, type PermissionItem, type PermissionPayload, type RoleItem, type RolePayload } from '@/api/rbac'
import './index.css'

type RbacTab = 'roles' | 'permissions' | 'assign'

interface AssignPermissionForm {
    roleId: number
    permissionIds: number[]
}

const statusOptions = [
    { label: '启用', value: 1 },
    { label: '禁用', value: 0 }
]

const resourceTypeOptions = [
    { label: 'API', value: 'API' },
    { label: 'MENU', value: 'MENU' },
    { label: 'BUTTON', value: 'BUTTON' }
]

const Rbac = () => {
    const { message } = App.useApp()
    const [roleForm] = Form.useForm<RolePayload>()
    const [permissionForm] = Form.useForm<PermissionPayload>()
    const [assignForm] = Form.useForm<AssignPermissionForm>()
    const [activeTab, setActiveTab] = useState<RbacTab>('roles')
    const [roles, setRoles] = useState<RoleItem[]>([])
    const [permissions, setPermissions] = useState<PermissionItem[]>([])
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [roleModalOpen, setRoleModalOpen] = useState(false)
    const [permissionModalOpen, setPermissionModalOpen] = useState(false)
    const [editingRole, setEditingRole] = useState<RoleItem | null>(null)
    const [editingPermission, setEditingPermission] = useState<PermissionItem | null>(null)
    const [rolePermissionsLoading, setRolePermissionsLoading] = useState(false)

    const enabledRoles = useMemo(() => roles.filter((role) => role.status === 1), [roles])
    const enabledPermissions = useMemo(() => permissions.filter((permission) => permission.status === 1), [permissions])

    const fetchRbac = useCallback(async () => {
        setLoading(true)
        try {
            const [roleResult, permissionResult] = await Promise.all([rbacApi.roles(), rbacApi.permissions()])

            if (roleResult.code !== 200) {
                message.error(roleResult.message || '获取角色列表失败')
                return
            }

            if (permissionResult.code !== 200) {
                message.error(permissionResult.message || '获取权限列表失败')
                return
            }

            setRoles(roleResult.data || [])
            setPermissions(permissionResult.data || [])
        } catch {
            message.error('获取权限数据失败')
        } finally {
            setLoading(false)
        }
    }, [message])

    useEffect(() => {
        fetchRbac()
    }, [fetchRbac])

    const openRoleModal = (role?: RoleItem) => {
        setEditingRole(role || null)
        roleForm.setFieldsValue(
            role
                ? { roleCode: role.roleCode, roleName: role.roleName, status: role.status }
                : { roleCode: '', roleName: '', status: 1 }
        )
        setRoleModalOpen(true)
    }

    const openPermissionModal = (permission?: PermissionItem) => {
        setEditingPermission(permission || null)
        permissionForm.setFieldsValue(
            permission
                ? {
                      permissionCode: permission.permissionCode,
                      permissionName: permission.permissionName,
                      resourceType: permission.resourceType || 'API',
                      path: permission.path,
                      status: permission.status
                  }
                : { permissionCode: '', permissionName: '', resourceType: 'API', path: '', status: 1 }
        )
        setPermissionModalOpen(true)
    }

    const saveRole = async () => {
        const values = await roleForm.validateFields()
        setSaving(true)
        try {
            const payload = {
                ...values,
                roleCode: values.roleCode.trim(),
                roleName: values.roleName.trim()
            }
            const result = editingRole ? await rbacApi.updateRole(editingRole.id, payload) : await rbacApi.addRole(payload)

            if (result.code !== 200) {
                message.error(result.message || '保存角色失败')
                return
            }

            message.success(editingRole ? '角色已更新' : '角色已新增')
            setRoleModalOpen(false)
            setEditingRole(null)
            await fetchRbac()
        } catch {
            message.error('保存角色失败')
        } finally {
            setSaving(false)
        }
    }

    const savePermission = async () => {
        const values = await permissionForm.validateFields()
        setSaving(true)
        try {
            const payload = {
                ...values,
                permissionCode: values.permissionCode.trim(),
                permissionName: values.permissionName.trim(),
                path: values.path?.trim()
            }
            const result = editingPermission
                ? await rbacApi.updatePermission(editingPermission.id, payload)
                : await rbacApi.addPermission(payload)

            if (result.code !== 200) {
                message.error(result.message || '保存权限失败')
                return
            }

            message.success(editingPermission ? '权限已更新' : '权限已新增')
            setPermissionModalOpen(false)
            setEditingPermission(null)
            await fetchRbac()
        } catch {
            message.error('保存权限失败')
        } finally {
            setSaving(false)
        }
    }

    const deleteRole = async (id: number) => {
        setLoading(true)
        try {
            const result = await rbacApi.deleteRole(id)
            if (result.code !== 200) {
                message.error(result.message || '删除角色失败')
                return
            }

            message.success('角色已删除')
            await fetchRbac()
        } catch {
            message.error('删除角色失败')
        } finally {
            setLoading(false)
        }
    }

    const deletePermission = async (id: number) => {
        setLoading(true)
        try {
            const result = await rbacApi.deletePermission(id)
            if (result.code !== 200) {
                message.error(result.message || '删除权限失败')
                return
            }

            message.success('权限已删除')
            await fetchRbac()
        } catch {
            message.error('删除权限失败')
        } finally {
            setLoading(false)
        }
    }

    const assignPermissions = async () => {
        const values = await assignForm.validateFields()
        setSaving(true)
        try {
            const result = await rbacApi.assignRolePermissions(values)
            if (result.code !== 200) {
                message.error(result.message || '分配角色权限失败')
                return
            }

            message.success('角色权限已更新')
            assignForm.resetFields()
        } catch {
            message.error('分配角色权限失败')
        } finally {
            setSaving(false)
        }
    }

    const roleColumns: ColumnsType<RoleItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 80
        },
        {
            title: '角色编码',
            dataIndex: 'roleCode',
            width: 180
        },
        {
            title: '角色名称',
            dataIndex: 'roleName'
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 120,
            render: (value?: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            width: 190,
            render: (value?: string) => value || '-'
        },
        {
            title: '操作',
            width: 160,
            render: (_, record) => (
                <Space size="small">
                    <Button type="link" onClick={() => openRoleModal(record)}>
                        编辑
                    </Button>
                    <Popconfirm title="确认删除该角色？" okText="删除" cancelText="取消" onConfirm={() => deleteRole(record.id)}>
                        <Button type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    const permissionColumns: ColumnsType<PermissionItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 80
        },
        {
            title: '权限编码',
            dataIndex: 'permissionCode',
            width: 220
        },
        {
            title: '权限名称',
            dataIndex: 'permissionName',
            width: 220
        },
        {
            title: '资源类型',
            dataIndex: 'resourceType',
            width: 120,
            render: (value?: string) => <Tag>{value || 'API'}</Tag>
        },
        {
            title: '路径',
            dataIndex: 'path',
            ellipsis: true,
            render: (value?: string) => value || '-'
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 120,
            render: (value?: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>
        },
        {
            title: '操作',
            width: 160,
            render: (_, record) => (
                <Space size="small">
                    <Button type="link" onClick={() => openPermissionModal(record)}>
                        编辑
                    </Button>
                    <Popconfirm title="确认删除该权限？" okText="删除" cancelText="取消" onConfirm={() => deletePermission(record.id)}>
                        <Button type="link" danger>
                            删除
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    const rolePanel = (
        <>
            <div className="rbac-toolbar">
                <Button type="primary" icon={<Icon icon="solar:add-circle-bold" />} onClick={() => openRoleModal()}>
                    新增角色
                </Button>
            </div>
            <Table
                rowKey="id"
                loading={loading}
                columns={roleColumns}
                dataSource={roles}
                locale={{ emptyText: <Empty description="暂无角色" /> }}
                pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (value) => `共 ${value} 条` }}
            />
        </>
    )

    const permissionPanel = (
        <>
            <div className="rbac-toolbar">
                <Button type="primary" icon={<Icon icon="solar:add-circle-bold" />} onClick={() => openPermissionModal()}>
                    新增权限
                </Button>
            </div>
            <Table
                rowKey="id"
                loading={loading}
                columns={permissionColumns}
                dataSource={permissions}
                locale={{ emptyText: <Empty description="暂无权限" /> }}
                pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (value) => `共 ${value} 条` }}
            />
        </>
    )

    const assignPanel = (
        <div className="rbac-assign">
            <Form form={assignForm} layout="vertical" onFinish={assignPermissions}>
                <Form.Item name="roleId" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
                    <Select
                        placeholder="请选择角色"
                        options={enabledRoles.map((role) => ({
                            label: `${role.roleName} (${role.roleCode})`,
                            value: role.id
                        }))}
                        onChange={async (roleId: number) => {
                            assignForm.setFieldValue('permissionIds', [])
                            if (!roleId) return
                            setRolePermissionsLoading(true)
                            try {
                                const res = await rbacApi.getRolePermissions(roleId)
                                if (res.code === 200 && res.data) {
                                    assignForm.setFieldValue('permissionIds', res.data.map((p) => p.id))
                                } else {
                                    message.error(res.message || '获取角色权限失败')
                                }
                            } catch {
                                message.error('获取角色权限失败')
                            } finally {
                                setRolePermissionsLoading(false)
                            }
                        }}
                    />
                </Form.Item>
                <Form.Item name="permissionIds" label="权限" rules={[{ required: true, message: '请选择权限' }]}>
                    <Select
                        mode="multiple"
                        placeholder="请选择权限"
                        loading={rolePermissionsLoading}
                        options={enabledPermissions.map((permission) => ({
                            label: `${permission.permissionName} (${permission.permissionCode})`,
                            value: permission.id
                        }))}
                    />
                </Form.Item>
                <Button type="primary" htmlType="submit" loading={saving} icon={<Icon icon="solar:shield-keyhole-bold" />}>
                    保存角色权限
                </Button>
            </Form>
        </div>
    )

    return (
        <div className="rbac-page">
            <section className="rbac-card">
                <div className="rbac-hero">
                    <div>
                        <div className="rbac-kicker">
                            <Icon icon="solar:shield-user-bold-duotone" width={20} />
                            RBAC Workspace
                        </div>
                        <h1 className="rbac-title">权限管理</h1>
                        <p className="rbac-desc">管理角色、权限和角色权限绑定。</p>
                    </div>

                    <div className="rbac-stats">
                        {[
                            ['角色', roles.length],
                            ['权限', permissions.length],
                            ['启用权限', enabledPermissions.length]
                        ].map(([label, value]) => (
                            <div key={label} className="rbac-stat">
                                <div className="rbac-stat-label">{label}</div>
                                <div className="rbac-stat-value">{value}</div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="rbac-content">
                    <Tabs
                        activeKey={activeTab}
                        onChange={(key) => setActiveTab(key as RbacTab)}
                        items={[
                            { key: 'roles', label: '角色', children: rolePanel },
                            { key: 'permissions', label: '权限', children: permissionPanel },
                            { key: 'assign', label: '角色权限', children: assignPanel }
                        ]}
                    />
                </div>
            </section>

            <Modal
                title={editingRole ? '编辑角色' : '新增角色'}
                open={roleModalOpen}
                okText="保存"
                cancelText="取消"
                confirmLoading={saving}
                onCancel={() => setRoleModalOpen(false)}
                onOk={saveRole}
                destroyOnHidden
            >
                <Form form={roleForm} layout="vertical" className="pt-3">
                    <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}>
                        <Input maxLength={50} placeholder="例如 ADMIN" />
                    </Form.Item>
                    <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}>
                        <Input maxLength={50} placeholder="例如 Administrator" />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select options={statusOptions} />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={editingPermission ? '编辑权限' : '新增权限'}
                open={permissionModalOpen}
                okText="保存"
                cancelText="取消"
                confirmLoading={saving}
                onCancel={() => setPermissionModalOpen(false)}
                onOk={savePermission}
                destroyOnHidden
            >
                <Form form={permissionForm} layout="vertical" className="pt-3">
                    <Form.Item name="permissionCode" label="权限编码" rules={[{ required: true, message: '请输入权限编码' }]}>
                        <Input maxLength={100} placeholder="例如 note:list" disabled={!!editingPermission} />
                    </Form.Item>
                    <Form.Item name="permissionName" label="权限名称" rules={[{ required: true, message: '请输入权限名称' }]}>
                        <Input maxLength={100} placeholder="例如 List notes" />
                    </Form.Item>
                    <Form.Item name="resourceType" label="资源类型">
                        <Select options={resourceTypeOptions} />
                    </Form.Item>
                    <Form.Item name="path" label="路径">
                        <Input maxLength={200} placeholder="例如 /notes/page" />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <Select options={statusOptions} />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}

export default Rbac
