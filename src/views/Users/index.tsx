import { Icon } from '@iconify/react'
import { App, Avatar, Button, Empty, Form, Input, Modal, Space, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useMemo, useState } from 'react'
import { userApi, type UserItem, type UserRegisterPayload } from '@/api/user'
import './index.css'

const Users = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<UserRegisterPayload>()
    const [users, setUsers] = useState<UserItem[]>([])
    const [keyword, setKeyword] = useState('')
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [detailUser, setDetailUser] = useState<UserItem | null>(null)

    const fetchUsers = async () => {
        setLoading(true)
        try {
            const result = await userApi.list()
            if (result.code !== 200) {
                message.error(result.message || '获取用户列表失败')
                return
            }

            setUsers(result.data || [])
        } catch {
            message.error('获取用户列表失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchUsers()
    }, [])

    const filteredUsers = useMemo(() => {
        const query = keyword.trim().toLowerCase()
        if (!query) {
            return users
        }

        return users.filter((user) =>
            [user.username, user.nickname, user.role].some((value) => value?.toLowerCase().includes(query))
        )
    }, [keyword, users])

    const activeTotal = users.filter((user) => user.status === 1).length
    const adminTotal = users.filter((user) => user.role === 'ADMIN').length

    const openRegisterModal = () => {
        form.resetFields()
        setModalOpen(true)
    }

    const handleSubmit = async () => {
        const values = await form.validateFields()
        setSaving(true)

        try {
            const payload = {
                ...values,
                username: values.username.trim(),
                nickname: values.nickname?.trim(),
                avatar: values.avatar?.trim()
            }
            const result = await userApi.register(payload)
            if (result.code !== 200) {
                message.error(result.message || '注册用户失败')
                return
            }

            message.success('用户已注册')
            setModalOpen(false)
            form.resetFields()
            await fetchUsers()
        } catch {
            message.error('注册用户失败')
        } finally {
            setSaving(false)
        }
    }

    const columns: ColumnsType<UserItem> = [
        {
            title: 'ID',
            dataIndex: 'id',
            width: 80
        },
        {
            title: '用户',
            dataIndex: 'username',
            render: (_, record) => (
                <Space size="middle">
                    <Avatar src={record.avatar} icon={<Icon icon="solar:user-rounded-bold" />} />
                    <div>
                        <div className="users-name">{record.nickname || record.username}</div>
                        <div className="users-username">@{record.username}</div>
                    </div>
                </Space>
            )
        },
        {
            title: '角色',
            dataIndex: 'role',
            width: 130,
            render: (value?: string) => <Tag color={value === 'ADMIN' ? 'cyan' : 'blue'}>{value || 'USER'}</Tag>
        },
        {
            title: '状态',
            dataIndex: 'status',
            width: 120,
            render: (value?: number) => <Tag color={value === 1 ? 'green' : 'default'}>{value === 1 ? '启用' : '禁用'}</Tag>
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
                <Button type="link" onClick={() => setDetailUser(record)}>
                    查看
                </Button>
            )
        }
    ]

    return (
        <div className="users-page">
            <div className="users-container">
                <section className="users-card">
                    <div className="users-hero">
                        <div>
                            <div className="users-kicker">
                                <Icon icon="solar:users-group-rounded-bold-duotone" width={20} />
                                User Workspace
                            </div>
                            <h1 className="users-title">用户管理</h1>
                            <p className="users-desc">管理后端 sys_user 表中的账号、角色和启用状态。</p>
                        </div>

                        <div className="users-stats">
                            {[
                                ['总数', users.length],
                                ['启用', activeTotal],
                                ['管理员', adminTotal]
                            ].map(([label, value]) => (
                                <div key={label} className="users-stat">
                                    <div className="users-stat-label">{label}</div>
                                    <div className="users-stat-value">{value}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="users-toolbar">
                        <Input.Search
                            allowClear
                            className="users-search"
                            placeholder="按用户名、昵称或角色搜索"
                            enterButton="查询"
                            prefix={<Icon icon="solar:magnifer-linear" color="#94a3b8" />}
                            value={keyword}
                            onChange={(event) => setKeyword(event.target.value)}
                            onSearch={(value) => setKeyword(value)}
                        />
                        <Button type="primary" icon={<Icon icon="solar:user-plus-rounded-bold" />} onClick={openRegisterModal}>
                            注册用户
                        </Button>
                    </div>

                    <div className="users-table-wrap">
                        <Table
                            rowKey="id"
                            loading={loading}
                            columns={columns}
                            dataSource={filteredUsers}
                            locale={{ emptyText: <Empty description="暂无用户" /> }}
                            pagination={{
                                pageSize: 10,
                                showSizeChanger: true,
                                showTotal: (value) => `共 ${value} 条`
                            }}
                        />
                    </div>
                </section>
            </div>

            <Modal
                title="注册用户"
                open={modalOpen}
                okText="注册"
                cancelText="取消"
                confirmLoading={saving}
                onCancel={() => setModalOpen(false)}
                onOk={handleSubmit}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" className="pt-3">
                    <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
                        <Input maxLength={50} placeholder="请输入用户名" />
                    </Form.Item>
                    <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password maxLength={255} placeholder="请输入密码" />
                    </Form.Item>
                    <Form.Item name="nickname" label="昵称">
                        <Input maxLength={50} placeholder="请输入昵称" />
                    </Form.Item>
                    <Form.Item name="avatar" label="头像地址">
                        <Input maxLength={200} placeholder="请输入头像 URL" />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal title="用户详情" open={!!detailUser} footer={null} onCancel={() => setDetailUser(null)} destroyOnHidden>
                {detailUser && (
                    <div className="user-detail">
                        <Avatar size={56} src={detailUser.avatar} icon={<Icon icon="solar:user-rounded-bold" />} />
                        <div className="user-detail-main">
                            <div className="user-detail-name">{detailUser.nickname || detailUser.username}</div>
                            <div className="user-detail-username">@{detailUser.username}</div>
                        </div>
                        <div className="user-detail-grid">
                            <span>ID：{detailUser.id}</span>
                            <span>角色：{detailUser.role || 'USER'}</span>
                            <span>状态：{detailUser.status === 1 ? '启用' : '禁用'}</span>
                            <span>创建：{detailUser.createdAt || '-'}</span>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    )
}

export default Users
