import { Icon } from '@iconify/react'
import {
    App,
    Button,
    DatePicker,
    Empty,
    Form,
    Input,
    InputNumber,
    Modal,
    Popconfirm,
    Select,
    Space,
    Switch,
    Table,
    Tag,
    UploadFile,
    Upload,
    Avatar
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PlusOutlined } from '@ant-design/icons'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    scheduleApi,
    type DailyScheduleItem,
    type RotaStateResult,
    type ScheduleItem,
    type ShiftType,
    type StaffItem,
    type StaffPayload,
    type StaffType
} from '@/api/schedule'
import html2canvas from 'html2canvas'
import './index.css'
import { RcFile } from 'antd/es/upload'

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

const weekLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const staffTypeText: Record<StaffType, string> = {
    0: '医生',
    1: '护士',
    2: '前台'
}

const shiftText: Record<ShiftType, string> = {
    0: '日班',
    1: '夜班',
    2: '休息'
}

const Schedule = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<StaffPayload>()
    const [monthValue, setMonthValue] = useState<Dayjs>(dayjs())
    const [staffList, setStaffList] = useState<StaffItem[]>([])
    const [dailySchedules, setDailySchedules] = useState<DailyScheduleItem[]>([])
    const [rotaState, setRotaState] = useState<RotaStateResult>({})
    const [keyword, setKeyword] = useState('')
    const [loadingStaff, setLoadingStaff] = useState(false)
    const [loadingSchedule, setLoadingSchedule] = useState(false)
    const [savingStaff, setSavingStaff] = useState(false)
    const [generating, setGenerating] = useState(false)
    const [modalOpen, setModalOpen] = useState(false)
    const [forceOverwrite, setForceOverwrite] = useState(false)
    const [editingStaff, setEditingStaff] = useState<StaffItem | null>(null)
    const [staffScheduleModal, setStaffScheduleModal] = useState<StaffItem | null>(null)
    const [staffSchedules, setStaffSchedules] = useState<ScheduleItem[]>([])
    // 新增状态：编辑中的头像文件列表
    const [avatarFile, setAvatarFile] = useState<UploadFile[]>([])

    // 新增：是否正在上传头像的loading
    const [uploading, setUploading] = useState(false)

    // 新增：头像预览弹窗状态
    const [previewVisible, setPreviewVisible] = useState(false)
    const [previewUrl, setPreviewUrl] = useState<string>('')

    /**
     * 将文件上传到后端，返回图片URL
     * 你需要替换成你自己的上传API
     */
    const uploadAvatarRequest = async (file: RcFile): Promise<string> => {
        // 编辑模式下必须有 staffId
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

        // 假设后端返回 data.url 或 data.avatarUrl，根据实际情况调整
        const data = result.data as { url?: string } | undefined
        const url = data?.url
        if (!url) {
            message.error('上传成功但未返回头像地址')
            throw new Error('未返回头像地址')
        }

        return url
    }

    const fetchStaff = useCallback(async () => {
        setLoadingStaff(true)
        try {
            const result = await scheduleApi.staffList()
            if (result.code !== 200) {
                message.error(result.message || '获取人员列表失败')
                return
            }
            setStaffList(result.data || [])
        } catch {
            message.error('获取人员列表失败')
        } finally {
            setLoadingStaff(false)
        }
    }, [message])

    const fetchRotaState = useCallback(async () => {
        try {
            const result = await scheduleApi.rotaState()
            if (result.code !== 200) {
                message.error(result.message || '获取夜班队列失败')
                return
            }
            setRotaState(result.data || {})
        } catch {
            message.error('获取夜班队列失败')
        }
    }, [message])

    const fetchSchedule = useCallback(
        async (targetMonth: Dayjs) => {
            setLoadingSchedule(true)
            try {
                const result = await scheduleApi.monthly(targetMonth.year(), targetMonth.month() + 1)
                if (result.code !== 200) {
                    message.error(result.message || '获取月排班失败')
                    return
                }
                setDailySchedules(result.data || [])
            } catch {
                message.error('获取月排班失败')
            } finally {
                setLoadingSchedule(false)
            }
        },
        [message]
    )

    useEffect(() => {
        fetchStaff()
        fetchRotaState()
        fetchSchedule(monthValue)
    }, [fetchRotaState, fetchSchedule, fetchStaff, monthValue])

    const scheduleMap = useMemo(() => {
        return new Map(dailySchedules.map((item) => [dayjs(item.date).format('YYYY-MM-DD'), item]))
    }, [dailySchedules])

    const monthCells = useMemo(() => {
        const firstDayOffset = monthValue.startOf('month').day()
        const days = Array.from({ length: monthValue.daysInMonth() }, (_, index) => monthValue.date(index + 1))
        const placeholders = Array.from({ length: firstDayOffset }, () => null)
        return [...placeholders, ...days]
    }, [monthValue])

    const filteredStaff = useMemo(() => {
        const query = keyword.trim().toLowerCase()
        if (!query) {
            return staffList
        }
        return staffList.filter((staff) =>
            [staff.name, staffTypeText[staff.type], restDayOptions[staff.restDay]?.label].some((value) =>
                value?.toLowerCase().includes(query)
            )
        )
    }, [keyword, staffList])

    const activeDoctors = staffList.filter((staff) => staff.type === 0 && staff.isActive !== false).length
    const activeNurses = staffList.filter((staff) => staff.type === 1 && staff.isActive !== false).length
    const activeReceptionists = staffList.filter((staff) => staff.type === 2 && staff.isActive !== false).length
    const swappedTotal = dailySchedules.reduce(
        (total, item) => total + item.shifts.filter((shift) => shift.shiftType === 1 && shift.isSwapped).length,
        0
    )

    const openCreateModal = () => {
        setEditingStaff(null)
        form.resetFields()
        form.setFieldsValue({ type: 0, restDay: 1 })
        setAvatarFile([]) // 清空上传组件
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

        // 如果人员已有头像URL，初始化上传组件
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
            // 获取最终的头像URL（如果上传了新文件就用新URL，否则保留原值）
            let avatarUrl = ''
            if (avatarFile.length > 0 && avatarFile[0].url) {
                avatarUrl = avatarFile[0].url
            } else if (editingStaff?.avatarUrl) {
                avatarUrl = editingStaff.avatarUrl
            }

            const payload = {
                ...values,
                name: values.name.trim(),
                avatarUrl // 传给后端
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
            await fetchStaff()
            await fetchRotaState()
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
            await fetchStaff()
            await fetchRotaState()
        } catch {
            message.error('更新人员状态失败')
        } finally {
            setLoadingStaff(false)
        }
    }

    const handleMonthChange = (value: Dayjs | null) => {
        if (!value) {
            return
        }
        setMonthValue(value)
    }

    const handleGenerate = async () => {
        setGenerating(true)
        try {
            const result = await scheduleApi.generate({
                year: monthValue.year(),
                month: monthValue.month() + 1,
                forceOverwrite
            })
            if (result.code !== 200) {
                message.error(result.message || '生成排班失败')
                return
            }
            message.success('排班已生成')
            await fetchSchedule(monthValue)
            await fetchRotaState()
        } catch {
            message.error('生成排班失败')
        } finally {
            setGenerating(false)
        }
    }

    const handleDeleteMonth = async () => {
        setLoadingSchedule(true)
        try {
            const result = await scheduleApi.deleteMonth(monthValue.year(), monthValue.month() + 1)
            if (result.code !== 200) {
                message.error(result.message || '删除月排班失败')
                return
            }
            message.success('月排班已删除')
            await fetchSchedule(monthValue)
        } catch {
            message.error('删除月排班失败')
        } finally {
            setLoadingSchedule(false)
        }
    }

    const handleExportImage = async () => {
        const board = document.querySelector('.schedule-board') as HTMLElement | null
        if (!board) {
            message.warning('未找到排班看板')
            return
        }

        try {
            const canvas = await html2canvas(board, {
                backgroundColor: '#ffffff',
                scale: 2, // 高清输出
                useCORS: true
            })
            const link = document.createElement('a')
            link.download = `排班_${monthValue.format('YYYY-MM')}.png`
            link.href = canvas.toDataURL('image/png')
            link.click()
            message.success('导出成功')
        } catch {
            message.error('导出失败')
        }
    }

    const openStaffSchedule = async (staff: StaffItem) => {
        setStaffScheduleModal(staff)
        setLoadingSchedule(true)
        try {
            const result = await scheduleApi.staffSchedule(staff.id, monthValue.year(), monthValue.month() + 1)
            if (result.code !== 200) {
                message.error(result.message || '获取个人排班失败')
                return
            }
            setStaffSchedules(
                (result.data || []).sort((a, b) => dayjs(a.shiftDate).valueOf() - dayjs(b.shiftDate).valueOf())
            )
        } catch {
            message.error('获取个人排班失败')
        } finally {
            setLoadingSchedule(false)
        }
    }

    // 新增：处理头像点击预览
    const handleAvatarPreview = (url: string) => {
        if (!url) {
            message.warning('该人员暂无头像')
            return
        }
        setPreviewUrl(url)
        setPreviewVisible(true)
    }

    const renderShiftGroup = (dayItem: DailyScheduleItem | undefined, shiftType: ShiftType, staffType: StaffType) => {
        const shifts = (dayItem?.shifts || []).filter(
            (shift) => shift.shiftType === shiftType && shift.staffType === staffType
        )
        if (!shifts.length) {
            return <span className="schedule-empty-text">无</span>
        }

        return (
            <Space size={[4, 4]} wrap>
                {shifts.map((shift) => (
                    <Tag
                        key={`${shift.staffId}-${shift.shiftType}`}
                        className={shift.isSwapped ? 'shift-tag swapped' : 'shift-tag'}
                        color={shift.shiftType === 1 ? 'volcano' : shift.shiftType === 2 ? 'default' : 'geekblue'}
                    >
                        {shift.staffName}
                        {shift.isSwapped ? ' 顺延' : ''}
                    </Tag>
                ))}
            </Space>
        )
    }

    const renderRotaQueue = (type: StaffType) => {
        const data = type === 0 ? rotaState.doctor : type === 1 ? rotaState.nurse : rotaState.receptionist
        const queue = data?.queue || []
        if (!queue.length) {
            return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无队列" />
        }

        return (
            <div className="rota-queue">
                {queue.map((staff) => (
                    <div key={staff.id} className={data?.nextStaffId === staff.id ? 'rota-person next' : 'rota-person'}>
                        <span>{staff.name}</span>
                        <Tag color={data?.nextStaffId === staff.id ? 'cyan' : 'blue'}>#{staff.nightOrder}</Tag>
                    </div>
                ))}
            </div>
        )
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
                    <Button type="link" onClick={() => openStaffSchedule(record)}>
                        排班
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

    // 根据当前弹窗的个人排班数据，构建日期映射
    const personalScheduleMap = useMemo(() => {
        if (!staffScheduleModal) return new Map<string, ScheduleItem>()
        const map = new Map<string, ScheduleItem>()
        staffSchedules.forEach((item) => {
            map.set(dayjs(item.shiftDate).format('YYYY-MM-DD'), item)
        })
        return map
    }, [staffSchedules, staffScheduleModal])

    return (
        <div className="schedule-page">
            <div className="schedule-container">
                <section className="schedule-card">
                    <div className="schedule-hero">
                        <div>
                            <div className="schedule-kicker">
                                <Icon icon="solar:calendar-mark-bold-duotone" width={20} />
                                Rota Workspace
                            </div>
                            <h1 className="schedule-title">排班管理</h1>
                            <p className="schedule-desc">维护医生、护士夜班队列，生成并查看指定月份排班。</p>
                        </div>

                        <div className="schedule-stats">
                            {[
                                ['医生', activeDoctors],
                                ['护士', activeNurses],
                                ['前台', activeReceptionists],
                                ['已排天数', dailySchedules.length],
                                ['顺延夜班', swappedTotal]
                            ].map(([label, value]) => (
                                <div key={label} className="schedule-stat">
                                    <div className="schedule-stat-label">{label}</div>
                                    <div className="schedule-stat-value">{value}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="schedule-toolbar">
                        <Space size="middle" wrap>
                            <DatePicker
                                picker="month"
                                value={monthValue}
                                allowClear={false}
                                onChange={handleMonthChange}
                            />
                            <Space size={8}>
                                <span className="toolbar-label">覆盖已有排班</span>
                                <Switch checked={forceOverwrite} onChange={setForceOverwrite} />
                            </Space>
                            <Button
                                type="primary"
                                icon={<Icon icon="solar:calendar-add-bold" />}
                                loading={generating}
                                onClick={handleGenerate}
                            >
                                生成排班
                            </Button>
                            <Popconfirm
                                title="确认删除当前月份排班？"
                                okText="删除"
                                cancelText="取消"
                                onConfirm={handleDeleteMonth}
                            >
                                <Button danger icon={<Icon icon="solar:trash-bin-trash-bold" />}>
                                    删除本月
                                </Button>
                            </Popconfirm>
                            <Button icon={<Icon icon="solar:download-bold" />} onClick={handleExportImage}>
                                导出图片
                            </Button>
                        </Space>
                        <Button icon={<Icon icon="solar:user-plus-rounded-bold" />} onClick={openCreateModal}>
                            新增人员
                        </Button>
                    </div>

                    <div className="rota-panel">
                        <div className="rota-card">
                            <div className="rota-title">医生夜班队列</div>
                            {renderRotaQueue(0)}
                        </div>
                        <div className="rota-card">
                            <div className="rota-title">护士夜班队列</div>
                            {renderRotaQueue(1)}
                        </div>
                        <div className="rota-card">
                            <div className="rota-title">前台夜班队列</div>
                            {renderRotaQueue(2)}
                        </div>
                    </div>

                    <div className="schedule-board">
                        <div className="schedule-week-row">
                            {weekLabels.map((label) => (
                                <div key={label} className="schedule-week-cell">
                                    {label}
                                </div>
                            ))}
                        </div>
                        <div className={loadingSchedule ? 'schedule-month-grid loading' : 'schedule-month-grid'}>
                            {monthCells.map((day, index) => {
                                if (!day) {
                                    return <div key={`empty-${index}`} className="schedule-day-cell empty" />
                                }

                                const dateKey = day.format('YYYY-MM-DD')
                                const dayItem = scheduleMap.get(dateKey)
                                return (
                                    <div key={dateKey} className="schedule-day-cell">
                                        <div className="schedule-day-head">
                                            <span>{day.date()}</span>
                                            {dayItem ? <Tag color="green">已生成</Tag> : <Tag>未生成</Tag>}
                                        </div>
                                        <div className="shift-section night">
                                            <div className="shift-label">夜班</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 1, 0)}</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 1, 1)}</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 1, 2)}</div>
                                        </div>
                                        <div className="shift-section">
                                            <div className="shift-label">日班</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 0, 0)}</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 0, 1)}</div>
                                            <div className="shift-row">{renderShiftGroup(dayItem, 0, 2)}</div>
                                        </div>
                                        {/* 仅当该日存在至少一条休息班次时才显示休息模块 */}
                                        {dayItem && dayItem.shifts.some((s) => s.shiftType === 2) && (
                                            <div className="shift-section rest">
                                                <div className="shift-label">休息</div>
                                                <div className="shift-row">{renderShiftGroup(dayItem, 2, 0)}</div>
                                                <div className="shift-row">{renderShiftGroup(dayItem, 2, 1)}</div>
                                                <div className="shift-row">{renderShiftGroup(dayItem, 2, 2)}</div>
                                            </div>
                                        )}
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                </section>

                <section className="schedule-card staff-card">
                    <div className="staff-toolbar">
                        <div>
                            <h2 className="section-title">人员维护</h2>
                            <p className="section-desc">新增人员会进入对应类型的夜班队列，停用后不参与后续生成。</p>
                        </div>
                        <Input.Search
                            allowClear
                            className="staff-search"
                            placeholder="按姓名、类型、休息日搜索"
                            enterButton="查询"
                            prefix={<Icon icon="solar:magnifer-linear" color="#94a3b8" />}
                            value={keyword}
                            onChange={(event) => setKeyword(event.target.value)}
                            onSearch={(value) => setKeyword(value)}
                        />
                    </div>

                    <Table
                        rowKey="id"
                        loading={loadingStaff}
                        columns={staffColumns}
                        dataSource={filteredStaff}
                        rowClassName={(record) => (record.isActive === false ? 'staff-row-disabled' : '')}
                        locale={{ emptyText: <Empty description="暂无人员" /> }}
                        pagination={{ pageSize: 8, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
                    />
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
                                    setUploading(true)
                                    try {
                                        const url = await uploadAvatarRequest(file as RcFile)
                                        setAvatarFile([
                                            {
                                                uid: file.uid,
                                                name: file.name,
                                                status: 'done',
                                                url // 上传成功后使用返回的 URL
                                            }
                                        ])
                                        message.success('头像上传成功')
                                    } catch (error: any) {
                                        message.error(error.message || '头像上传失败')
                                        setAvatarFile([])
                                    } finally {
                                        setUploading(false)
                                    }
                                    // 阻止默认上传行为（我们已经手动调接口）
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

            <Modal
                title={staffScheduleModal ? `${staffScheduleModal.name} 的本月排班` : '个人排班'}
                open={!!staffScheduleModal}
                footer={null}
                width={780}
                onCancel={() => {
                    setStaffScheduleModal(null)
                    setStaffSchedules([])
                }}
                destroyOnHidden
            >
                {staffScheduleModal && (
                    <div className="personal-calendar-wrapper">
                        {/* 星期表头 */}
                        <div className="schedule-week-row">
                            {weekLabels.map((label) => (
                                <div key={label} className="schedule-week-cell">
                                    {label}
                                </div>
                            ))}
                        </div>

                        {/* 日历网格 – 使用新类名 personal-calendar-grid 隔离样式 */}
                        <div className="personal-calendar-grid">
                            {/* 月初占位空白格 */}
                            {Array.from({ length: monthValue.startOf('month').day() }).map((_, idx) => (
                                <div key={`empty-${idx}`} className="schedule-day-cell empty" />
                            ))}

                            {/* 当月每一天 */}
                            {Array.from({ length: monthValue.daysInMonth() }, (_, i) => {
                                const date = monthValue.date(i + 1)
                                const dateKey = date.format('YYYY-MM-DD')
                                const record = personalScheduleMap.get(dateKey)
                                const shiftType = record?.shiftType

                                return (
                                    <div key={dateKey} className="schedule-day-cell">
                                        <div className="schedule-day-head">
                                            <span>{date.date()}</span>
                                        </div>
                                        <div style={{ marginTop: 4 }}>
                                            {record ? (
                                                <Space size={4} wrap>
                                                    <Tag
                                                        color={
                                                            shiftType === 1
                                                                ? 'volcano'
                                                                : shiftType === 2
                                                                  ? 'default'
                                                                  : 'geekblue'
                                                        }
                                                    >
                                                        {shiftText[shiftType ?? 0]}
                                                    </Tag>
                                                    {record.isSwapped && <Tag color="orange">顺延</Tag>}
                                                </Space>
                                            ) : (
                                                <span className="schedule-empty-text">休息</span>
                                            )}
                                        </div>
                                    </div>
                                )
                            })}
                        </div>
                    </div>
                )}
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

export default Schedule
