import { Icon } from '@iconify/react'
import {
    App,
    Button,
    DatePicker,
    Empty,
    Modal,
    Popconfirm,
    Space,
    Switch,
    Tag
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
    scheduleApi,
    type DailyScheduleItem,
    type RotaStateResult,
    type ScheduleItem,
    type ShiftType,
    type StaffItem,
    type StaffType
} from '@/api/schedule'
import html2canvas from 'html2canvas'
import './index.css'

const weekLabels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const shiftText: Record<ShiftType, string> = {
    0: '日班',
    1: '夜班',
    2: '休息'
}

const Schedule = () => {
    const { message } = App.useApp()
    const [monthValue, setMonthValue] = useState<Dayjs>(dayjs())
    const [staffList, setStaffList] = useState<StaffItem[]>([])
    const [dailySchedules, setDailySchedules] = useState<DailyScheduleItem[]>([])
    const [rotaState, setRotaState] = useState<RotaStateResult>({})
    const [loadingSchedule, setLoadingSchedule] = useState(false)
    const [generating, setGenerating] = useState(false)
    const [forceOverwrite, setForceOverwrite] = useState(false)
    const [staffScheduleModal, setStaffScheduleModal] = useState<StaffItem | null>(null)
    const [staffSchedules, setStaffSchedules] = useState<ScheduleItem[]>([])

    const fetchStaff = useCallback(async () => {
        try {
            const result = await scheduleApi.staffList()
            if (result.code !== 200) {
                message.error(result.message || '获取人员列表失败')
                return
            }
            setStaffList(result.data || [])
        } catch {
            message.error('获取人员列表失败')
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
        (async () => {
            await fetchStaff()
            await fetchRotaState()
            await fetchSchedule(monthValue)
        })()
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

    const activeDoctors = staffList.filter((staff) => staff.type === 0 && staff.isActive !== false).length
    const activeNurses = staffList.filter((staff) => staff.type === 1 && staff.isActive !== false).length
    const activeReceptionists = staffList.filter((staff) => staff.type === 2 && staff.isActive !== false).length
    const swappedTotal = dailySchedules.reduce(
        (total, item) => total + item.shifts.filter((shift) => shift.shiftType === 1 && shift.isSwapped).length,
        0
    )

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
                scale: 2,
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
                        <span className="rota-person-name" style={{ cursor: 'pointer' }} onClick={() => openStaffSchedule(staff)}>
                            {staff.name}
                        </span>
                        <Tag color={data?.nextStaffId === staff.id ? 'cyan' : 'blue'}>#{staff.nightOrder}</Tag>
                    </div>
                ))}
            </div>
        )
    }

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
                            <h1 className="schedule-title">排班表</h1>
                            <p className="schedule-desc">查看医生、护士夜班队列，生成并查看指定月份排班。</p>
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
            </div>

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
                        <div className="schedule-week-row">
                            {weekLabels.map((label) => (
                                <div key={label} className="schedule-week-cell">
                                    {label}
                                </div>
                            ))}
                        </div>

                        <div className="personal-calendar-grid">
                            {Array.from({ length: monthValue.startOf('month').day() }).map((_, idx) => (
                                <div key={`empty-${idx}`} className="schedule-day-cell empty" />
                            ))}

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
        </div>
    )
}

export default Schedule
