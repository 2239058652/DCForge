import { Icon } from '@iconify/react'
import {
    App,
    Button,
    Empty,
    Form,
    Input,
    InputNumber,
    Modal,
    Popconfirm,
    Select,
    Space,
    Table,
    Tag,
    UploadFile,
    Upload,
    Avatar
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import { useCallback, useEffect, useState } from 'react'
import {
    scheduleApi,
    type StaffItem,
    type StaffPageDto,
    type StaffPayload,
    type StaffType
} from '@/api/schedule'
import { RcFile } from 'antd/es/upload'
import './index.css'

const staffTypeOptions = [
    { label: '医生', value: 0 },
    { label: '护士', value: 1 },
    { label: '前台', value: 2 }
]

const restDayOptions = [
    { label: '周日', value: 0 },
    { label: '周一', value: 1 },
    { label: '周二', value: 2 },
    { label: '周三', value: 3 },
    { label: '周四', value: 4 },
    { label: '周五', value: 5 },
    { label: '周六', value: 6 }
]

const staffTypeText: Record<StaffType, string> = {
    0: '医生',
    1: '护士',
    2: '前台'
}

const StaffManagement = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<StaffPayload>()
    const [staffList, setStaffList] = useState<StaffItem[]>([])
    const [keyword, setKeyword] = useState('')
    const [loadingStaff, setLoadingStaff] = useState(false)
    const [savingStaff, setSavingStaff] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [editingStaff, setEditingStaff] = useState<StaffItem | null>(null)
    const [avatarFile, setAvatarFile] = useState<UploadFile[]>([])
    const [previewVisible, setPreviewVisible] = useState(false)
    const [previewUrl, setPreviewUrl] = useState<string>('')
    // 分页状态
    const [pagination, setPagination] = useState({ current: 1, pageSize: 8, total: 0 })

    const uploadAvatarRequest = async (file: RcFile): Promise<string> => {
        if (!editingStaff?.id) {
            message.error('新增人员请先保存基本信息后再上传头像')
            throw new Error('暂未获取到人员ID')
        }

        const formData = new FormData()
        formData.append('file', file)

        const result = await scheduleApi.uploadStaffAvatar(editingStaff.id, formData)

        if (result.code !== 200) {
            message.error(result.message || '头像上传失败')
            throw new Error(result.message || '上传失败')
        }

        const data = result.data as { url?: string } | undefined
        const url = data?.url
        if (!url) {
            message.error('上传成功但未返回头像地址')
            throw new Error('未返回头像地址')
        }

        return url
    }

    /**
     * 分页获取人员列表
     * @param page - 页码（传 0 表示使用当前页）
     * @param pageSize - 每页条数（传 0 表示使用当前页大小）
     * @param searchKeyword - 搜索词（不传则使用当前 keyword）
     */
    const fetchStaff = useCallback(
        async (page = 0, pageSize = 0, searchKeyword?: string) => {
            const query = searchKeyword ?? keyword
            const params: StaffPageDto = {
                pageNum: page || pagination.current,
                pageSize: pageSize || pagination.pageSize,
                ...(query.trim() ? { name: query.trim() } : {})
            }

            setLoadingStaff(true)
            try {
                const result = await scheduleApi.staffListByPage(params)
                if (result.code !== 200) {
                    message.error(result.message || '获取人员列表失败')
                    return
                }
                const pageData = result.data!
                setStaffList(pageData.records || [])
                setPagination({
                    current: pageData.pageNum,
                    pageSize: pageData.pageSize,
                    total: pageData.total
                })
            } catch {
                message.error('获取人员列表失败')
            } finally {
                setLoadingStaff(false)
            }
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [message, keyword, pagination.current, pagination.pageSize]
    )

    useEffect(() => {
        (async () => {
            await fetchStaff(1)
        })()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    /** 搜索时重置到第一页 */
    const handleSearch = (value: string) => {
        setKeyword(value)
        setPagination((prev) => ({ ...prev, current: 1 }))
        // 搜索后需要重新请求，通过 setTimeout 确保 keyword 状态已更新
        setTimeout(() => {
            fetchStaff(1, pagination.pageSize, value)
        }, 0)
    }

    const openCreateModal = () => {
        setEditingStaff(null)
        form.resetFields()
        form.setFieldsValue({ type: 0, restDay: 1 })
        setAvatarFile([])
        setModalOpen(true)
    }

    const openEditModal = (staff: StaffItem) => {
        setEditingStaff(staff)
        form.setFieldsValue({
            name: staff.name,
            type: staff.type,
            restDay: staff.restDay,
            nightOrder: staff.nightOrder
        })

        if (staff.avatarUrl) {
            setAvatarFile([
                {
                    uid: '-1',
                    name: 'avatar.png',
                    status: 'done',
                    url: staff.avatarUrl
                }
            ])
        } else {
            setAvatarFile([])
        }
        setModalOpen(true)
    }

    const handleStaffSubmit = async () => {
        const values = await form.validateFields()
        setSavingStaff(true)
        try {
            let avatarUrl = ''
            if (avatarFile.length > 0 && avatarFile[0].url) {
                avatarUrl = avatarFile[0].url
            } else if (editingStaff?.avatarUrl) {
                avatarUrl = editingStaff.avatarUrl
            }

            const payload = {
                ...values,
                name: values.name.trim(),
                avatarUrl
            }
            const result = editingStaff
                ? await scheduleApi.updateStaff(editingStaff.id, payload)
                : await scheduleApi.addStaff(payload)

            if (result.code !== 200) {
                message.error(result.message || '保存人员失败')
                return
            }
            message.success(editingStaff ? '人员已更新' : '人员已新增')
            setModalOpen(false)
            setEditingStaff(null)
            form.resetFields()
            await fetchStaff(0)
        } catch {
            message.error('保存人员失败')
        } finally {
            setSavingStaff(false)
        }
    }

    const handleToggleStaff = async (staff: StaffItem) => {
        setLoadingStaff(true)
        try {
            const result =
                staff.isActive === false
                    ? await scheduleApi.activateStaff(staff.id)
                    : await scheduleApi.deactivateStaff(staff.id)
            if (result.code !== 200) {
                message.error(result.message || '更新人员状态失败')
                return
            }
            message.success(staff.isActive === false ? '人员已启用' : '人员已停用')
            await fetchStaff(0)
        } catch {
            message.error('更新人员状态失败')
        } finally {
            setLoadingStaff(false)
        }
    }

    const handleAvatarPreview = (url: string) => {
        if (!url) {
            message.warning('该人员暂无头像')
            return
        }
        setPreviewUrl(url)
        setPreviewVisible(true)
    }

    const staffColumns: ColumnsType<StaffItem> = [
        {
            title: '姓名',
            dataIndex: 'name',
            render: (_, record) => (
                <Space>
                    <Avatar
                        src={record.avatarUrl}
                        icon={!record.avatarUrl ? <Icon icon="solar:user-bold" /> : undefined}
                        size={32}
                        style={{ cursor: record.avatarUrl ? 'pointer' : 'default' }}
                        onClick={() => handleAvatarPreview(record.avatarUrl || '')}
                    />
                    <Icon
                        icon={
                            record.type === 0
                                ? 'solar:stethoscope-bold-duotone'
                                : record.type === 1
                                  ? 'solar:heart-pulse-bold-duotone'
                                  : 'solar:user-rounded-bold'
                        }
                    />
                    <span className="staff-name">{record.name}</span>
                </Space>
            )
        },
        {
            title: '类型',
            dataIndex: 'type',
            width: 90,
            render: (value: StaffType) => (
                <Tag color={value === 0 ? 'cyan' : value === 1 ? 'purple' : 'gold'}>{staffTypeText[value]}</Tag>
            )
        },
        {
            title: '固定休息',
            dataIndex: 'restDay',
            width: 110,
            render: (value: number) => restDayOptions[value]?.label || '-'
        },
        {
            title: '夜班序号',
            dataIndex: 'nightOrder',
            width: 110,
            sorter: (a, b) => a.nightOrder - b.nightOrder
        },
        {
            title: '状态',
            dataIndex: 'isActive',
            width: 90,
            render: (value?: boolean) => (
                <Tag color={value === false ? 'default' : 'green'}>{value === false ? '停用' : '启用'}</Tag>
            )
        },
        {
            title: '操作',
            width: 220,
            render: (_, record) => (
                <Space size="small">
                    <Button type="link" onClick={() => openEditModal(record)}>
                        编辑
                    </Button>
                    <Popconfirm
                        title={record.isActive === false ? '确认启用该人员？' : '确认停用该人员？'}
                        okText="确认"
                        cancelText="取消"
                        onConfirm={() => handleToggleStaff(record)}
                    >
                        <Button type="link" danger={record.isActive !== false}>
                            {record.isActive === false ? '启用' : '停用'}
                        </Button>
                    </Popconfirm>
                </Space>
            )
        }
    ]

    return (
        <div className="staff-page">
            <div className="staff-container">
                <section className="staff-card">
                    <div className="staff-hero">
                        <div>
                            <div className="staff-kicker">
                                <Icon icon="solar:users-group-rounded-bold-duotone" width={20} />
                                Staff Workspace
                            </div>
                            <h1 className="staff-title">员工管理</h1>
                            <p className="staff-desc">维护医生、护士、前台人员信息，管理夜班队列及启用状态。</p>
                        </div>
                        <Button type="primary" icon={<Icon icon="solar:user-plus-rounded-bold" />} onClick={openCreateModal}>
                            新增人员
                        </Button>
                    </div>

                    <div className="staff-toolbar">
                        <div>
                            <h2 className="section-title">人员列表</h2>
                            <p className="section-desc">新增人员会进入对应类型的夜班队列，停用后不参与后续排班生成。</p>
                        </div>
                        <Input.Search
                            allowClear
                            className="staff-search"
                            placeholder="按姓名、类型、休息日搜索"
                            enterButton="查询"
                            prefix={<Icon icon="solar:magnifer-linear" color="#94a3b8" />}
                            value={keyword}
                            onChange={(event) => setKeyword(event.target.value)}
                            onSearch={handleSearch}
                        />
                    </div>

                    <div className="staff-table-wrapper">
                        <Table
                            rowKey="id"
                            loading={loadingStaff}
                            columns={staffColumns}
                            dataSource={staffList}
                            rowClassName={(record) => (record.isActive === false ? 'staff-row-disabled' : '')}
                            locale={{ emptyText: <Empty description="暂无人员" /> }}
                            pagination={{
                                current: pagination.current,
                                pageSize: pagination.pageSize,
                                total: pagination.total,
                                showSizeChanger: true,
                                showTotal: (total) => `共 ${total} 条`,
                                onChange: (page, pageSize) => {
                                    fetchStaff(page, pageSize)
                                }
                            }}
                        />
                    </div>
                </section>
            </div>

            <Modal
                title={editingStaff ? '编辑人员' : '新增人员'}
                open={modalOpen}
                okText="保存"
                cancelText="取消"
                confirmLoading={savingStaff}
                onCancel={() => setModalOpen(false)}
                onOk={handleStaffSubmit}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" className="pt-3">
                    <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
                        <Input maxLength={50} placeholder="请输入姓名" />
                    </Form.Item>
                    {editingStaff && (
                        <Form.Item label="头像">
                            <Upload
                                listType="picture-card"
                                maxCount={1}
                                fileList={avatarFile}
                                beforeUpload={async (file) => {
                                    try {
                                        const url = await uploadAvatarRequest(file as RcFile)
                                        setAvatarFile([
                                            {
                                                uid: file.uid,
                                                name: file.name,
                                                status: 'done',
                                                url
                                            }
                                        ])
                                        message.success('头像上传成功')
                                    } catch (error: any) {
                                        message.error(error.message || '头像上传失败')
                                        setAvatarFile([])
                                    }
                                    return false
                                }}
                                onRemove={() => {
                                    setAvatarFile([])
                                    return true
                                }}
                            >
                                {avatarFile.length === 0 && (
                                    <div>
                                        <PlusOutlined />
                                        <div style={{ marginTop: 8 }}>上传</div>
                                    </div>
                                )}
                            </Upload>
                        </Form.Item>
                    )}
                    <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
                        <Select disabled={!!editingStaff} options={staffTypeOptions} />
                    </Form.Item>
                    <Form.Item
                        name="restDay"
                        label="固定休息日"
                        rules={[{ required: true, message: '请选择固定休息日' }]}
                    >
                        <Select options={restDayOptions} />
                    </Form.Item>
                    <Form.Item name="nightOrder" label="夜班队列序号">
                        <InputNumber min={1} precision={0} className="full-width" placeholder="不填则追加到队尾" />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 头像预览弹窗 */}
            <Modal
                title="头像预览"
                open={previewVisible}
                footer={null}
                onCancel={() => setPreviewVisible(false)}
                width={520}
                centered
            >
                <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <img
                        src={previewUrl}
                        alt="头像预览"
                        style={{
                            maxWidth: '100%',
                            maxHeight: '500px',
                            objectFit: 'contain',
                            borderRadius: '8px'
                        }}
                    />
                </div>
            </Modal>
        </div>
    )
}

export default StaffManagement
