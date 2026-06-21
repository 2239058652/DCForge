import { Icon } from '@iconify/react'
import { App, Button, Empty, Input, InputNumber, Modal, Pagination, Select, Switch, Table, Tooltip } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { localPortsApi, type LocalPortItem, type ProtocolFilter } from '@/api/localPorts'
import './index.css'

const fuzzyMatch = (text: string, keyword: string): boolean => {
    const lowerText = text.toLowerCase()
    const lowerKw = keyword.toLowerCase()
    let ki = 0
    for (let i = 0; i < lowerText.length && ki < lowerKw.length; i++) {
        if (lowerText[i] === lowerKw[ki]) ki++
    }
    return ki === lowerKw.length
}

const protocolOptions: { label: string; value: ProtocolFilter }[] = [
    { label: '全部', value: 'all' },
    { label: 'TCP', value: 'tcp' },
    { label: 'UDP', value: 'udp' }
]

const stateOptions = [
    { label: '全部', value: '' },
    { label: 'Listen', value: 'Listen' },
    { label: 'Established', value: 'Established' },
    { label: 'TimeWait', value: 'TimeWait' },
    { label: 'CloseWait', value: 'CloseWait' }
]

const stateLabels: Record<string, string> = {
    '1': 'Closed',
    '2': 'Listen',
    '5': 'Established',
    '8': 'CloseWait',
    '11': 'TimeWait'
}

const formatState = (state: string, protocol: string): string => {
    if (protocol === 'udp') return 'None'
    return stateLabels[state] || state
}

const LocalPorts = () => {
    const { message } = App.useApp()
    const [allPorts, setAllPorts] = useState<LocalPortItem[]>([])
    const [loading, setLoading] = useState(false)
    const [confirmModalOpen, setConfirmModalOpen] = useState(false)
    const [terminating, setTerminating] = useState(false)
    const [selectedItem, setSelectedItem] = useState<LocalPortItem | null>(null)
    const [pageNum, setPageNum] = useState(1)
    const [pageSize, setPageSize] = useState(20)

    // 筛选草稿（用户正在编辑，未提交）
    const [draftProtocol, setDraftProtocol] = useState<ProtocolFilter>('all')
    const [draftState, setDraftState] = useState<string>('')
    const [draftPortInput, setDraftPortInput] = useState<number | undefined>(undefined)
    const [draftKeyword, setDraftKeyword] = useState('')
    const [draftOnlyWithProcess, setDraftOnlyWithProcess] = useState(false)

    // 已提交的筛选条件（点查询后才生效）
    const [appliedFilters, setAppliedFilters] = useState({
        protocol: 'all' as ProtocolFilter,
        state: '',
        portInput: undefined as number | undefined,
        keyword: '',
        onlyWithProcess: false
    })

    const fetchPorts = useCallback(async () => {
        setLoading(true)
        try {
            const result = await localPortsApi.list()
            if (result.code !== 200) {
                message.error(result.message || '获取端口列表失败')
                return
            }
            setAllPorts(result.data || [])
        } catch {
            message.error('获取端口列表失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }, [message])

    useEffect(() => {
        fetchPorts()
    }, [fetchPorts])

    const filteredPorts = useMemo(() => {
        return allPorts.filter((item) => {
            if (appliedFilters.protocol !== 'all' && item.protocol !== appliedFilters.protocol) return false
            if (appliedFilters.state) {
                const itemStateLabel = formatState(item.state, item.protocol)
                if (itemStateLabel.toLowerCase() !== appliedFilters.state.toLowerCase()) return false
            }
            if (appliedFilters.portInput !== undefined && item.localPort !== appliedFilters.portInput) return false
            if (appliedFilters.onlyWithProcess && (item.pid === 0 || item.pid === null || item.pid === undefined)) return false
            if (appliedFilters.keyword) {
                const kw = appliedFilters.keyword
                const fields = [
                    item.processName || '',
                    String(item.pid),
                    item.localAddress,
                    item.executablePath || '',
                    item.commandLine || ''
                ]
                if (!fields.some((f) => fuzzyMatch(f, kw))) return false
            }
            return true
        })
    }, [allPorts, appliedFilters])

    const handleQuery = () => {
        setAppliedFilters({
            protocol: draftProtocol,
            state: draftState.trim(),
            portInput: draftPortInput,
            keyword: draftKeyword.trim(),
            onlyWithProcess: draftOnlyWithProcess
        })
    }

    const handleReset = () => {
        setDraftProtocol('all')
        setDraftState('')
        setDraftPortInput(undefined)
        setDraftKeyword('')
        setDraftOnlyWithProcess(false)
        setAppliedFilters({
            protocol: 'all',
            state: '',
            portInput: undefined,
            keyword: '',
            onlyWithProcess: false
        })
    }

    const handleRefresh = () => {
        fetchPorts()
        setPageNum(1)
    }

    const pagedPorts = useMemo(() => {
        const start = (pageNum - 1) * pageSize
        return filteredPorts.slice(start, start + pageSize)
    }, [filteredPorts, pageNum, pageSize])

    const openTerminateConfirm = (record: LocalPortItem) => {
        setSelectedItem(record)
        setConfirmModalOpen(true)
    }

    const handleTerminate = async () => {
        if (!selectedItem) return
        setTerminating(true)
        try {
            const result = await localPortsApi.terminate({
                pid: selectedItem.pid,
                protocol: selectedItem.protocol,
                port: selectedItem.localPort,
                confirm: true
            })
            if (result.code !== 200) {
                message.error(result.message || '结束进程失败')
                return
            }
            if (result.data?.terminated) {
                if (result.data.portReleased) {
                    message.success('进程已结束，端口已释放')
                } else {
                    message.warning('进程已处理，但端口仍未释放，请刷新后确认')
                }
            } else {
                message.warning('未能结束进程')
            }
            setConfirmModalOpen(false)
            setSelectedItem(null)
            await fetchPorts()
        } catch {
            message.error('结束进程失败，请检查后端服务')
        } finally {
            setTerminating(false)
        }
    }

    const columns: ColumnsType<LocalPortItem> = [
        {
            title: '协议',
            dataIndex: 'protocol',
            width: 80,
            render: (value: string) => <span className="port-protocol-tag">{value.toUpperCase()}</span>
        },
        {
            title: '本地端口',
            dataIndex: 'localPort',
            width: 100
        },
        {
            title: '状态',
            dataIndex: 'state',
            width: 110,
            render: (value: string, record: LocalPortItem) => {
                const label = formatState(value, record.protocol)
                return <span className={`port-state-tag port-state-${label.toLowerCase()}`}>{label}</span>
            }
        },
        {
            title: 'PID',
            dataIndex: 'pid',
            width: 80
        },
        {
            title: '进程名',
            dataIndex: 'processName',
            width: 130,
            render: (value?: string | null) => value || '-'
        },
        {
            title: '可执行路径',
            dataIndex: 'executablePath',
            ellipsis: true,
            render: (value?: string | null) => {
                if (!value) return '-'
                return (
                    <Tooltip title={value} placement="topLeft">
                        <span>{value}</span>
                    </Tooltip>
                )
            }
        },
        {
            title: '命令行',
            dataIndex: 'commandLine',
            ellipsis: true,
            render: (value?: string | null) => {
                if (!value) return '-'
                return (
                    <Tooltip title={value} placement="topLeft">
                        <span>{value}</span>
                    </Tooltip>
                )
            }
        },
        {
            title: '操作',
            width: 110,
            fixed: 'right',
            render: (_, record) => {
                if (!record.canTerminate) {
                    return (
                        <Tooltip title={record.terminateBlockedReason || '无法结束该进程'}>
                            <Button type="link" size="small" disabled>
                                结束进程
                            </Button>
                        </Tooltip>
                    )
                }
                return (
                    <Button type="link" size="small" danger onClick={() => openTerminateConfirm(record)}>
                        结束进程
                    </Button>
                )
            }
        }
    ]

    return (
        <div className="local-ports-page">
            <section className="local-ports-card">
                <div className="local-ports-hero">
                    <div>
                        <div className="local-ports-kicker">
                            <Icon icon="solar:server-bold-duotone" width={20} />
                            System Tools
                        </div>
                        <h1 className="local-ports-title">本机端口</h1>
                        <p className="local-ports-desc">查看和管理本机 Windows 端口占用情况，结束后台进程释放端口。</p>
                    </div>
                    <div className="local-ports-stats">
                        <div className="local-ports-stat">
                            <div className="local-ports-stat-label">端口总数</div>
                            <div className="local-ports-stat-value">{filteredPorts.length}</div>
                        </div>
                    </div>
                </div>

                <div className="local-ports-content">
                    <div className="local-ports-toolbar">
                        <div className="local-ports-filters">
                            <Select
                                options={protocolOptions}
                                value={draftProtocol}
                                onChange={setDraftProtocol}
                                style={{ width: 100 }}
                            />
                            <Select
                                options={stateOptions}
                                value={draftState}
                                onChange={setDraftState}
                                style={{ width: 130 }}
                                allowClear
                            />
                            <Input
                                placeholder="搜索进程名、PID、地址"
                                value={draftKeyword}
                                onChange={(e) => setDraftKeyword(e.target.value)}
                                style={{ width: 220 }}
                                allowClear
                            />
                            <InputNumber
                                placeholder="端口号"
                                value={draftPortInput}
                                onChange={(v) => setDraftPortInput(v ?? undefined)}
                                min={1}
                                max={65535}
                                style={{ width: 110 }}
                                controls={false}
                            />
                            <div className="local-ports-switch-wrapper">
                                <span>只看有进程</span>
                                <Switch size="small" value={draftOnlyWithProcess} onChange={setDraftOnlyWithProcess} />
                            </div>
                        </div>
                        <div className="local-ports-toolbar-actions">
                            <Button type="primary" onClick={handleQuery}>
                                查询
                            </Button>
                            <Button onClick={handleReset}>重置</Button>
                            <Button icon={<Icon icon="solar:refresh-bold" />} onClick={handleRefresh} loading={loading}>
                                刷新
                            </Button>
                        </div>
                    </div>

                    <Table
                        rowKey={(record, index) => `${record.protocol}-${record.localPort}-${record.pid}-${index}`}
                        loading={loading}
                        columns={columns}
                        dataSource={pagedPorts}
                        bordered
                        locale={{ emptyText: <Empty description="暂无端口数据" /> }}
                        pagination={false}
                        scroll={{ x: 1400 }}
                        sticky
                    />
                    <div className="local-ports-pagination">
                        <Pagination
                            current={pageNum}
                            pageSize={pageSize}
                            total={filteredPorts.length}
                            showSizeChanger
                            showTotal={(t) => `共 ${t} 条`}
                            pageSizeOptions={['20', '50', '100']}
                            onChange={(page, size) => {
                                setPageNum(page)
                                setPageSize(size)
                            }}
                        />
                    </div>
                </div>
            </section>

            <Modal
                title="确认结束进程"
                width={640}
                open={confirmModalOpen}
                okText="结束进程"
                cancelText="取消"
                okButtonProps={{ danger: true, loading: terminating }}
                cancelButtonProps={{ disabled: terminating }}
                onCancel={() => {
                    if (!terminating) {
                        setConfirmModalOpen(false)
                        setSelectedItem(null)
                    }
                }}
                onOk={handleTerminate}
                destroyOnHidden
            >
                {selectedItem && (
                    <div className="terminate-confirm-content">
                        <p className="terminate-confirm-warning">
                            确认结束进程 <strong>{selectedItem.processName || '未知'}</strong>（PID {selectedItem.pid}
                            ）？
                        </p>
                        <p>
                            它当前占用 {selectedItem.protocol.toUpperCase()} {selectedItem.localPort}{' '}
                            端口。结束进程可能中断正在运行的服务。
                        </p>
                        <div className="terminate-confirm-details">
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">协议</span>
                                <span className="terminate-detail-value">{selectedItem.protocol.toUpperCase()}</span>
                            </div>
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">端口</span>
                                <span className="terminate-detail-value">{selectedItem.localPort}</span>
                            </div>
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">PID</span>
                                <span className="terminate-detail-value">{selectedItem.pid}</span>
                            </div>
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">进程名</span>
                                <span className="terminate-detail-value">{selectedItem.processName || '-'}</span>
                            </div>
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">可执行路径</span>
                                <span className="terminate-detail-value">{selectedItem.executablePath || '-'}</span>
                            </div>
                            <div className="terminate-detail-row">
                                <span className="terminate-detail-label">命令行</span>
                                <span className="terminate-detail-value terminate-detail-wrap">
                                    {selectedItem.commandLine || '-'}
                                </span>
                            </div>
                        </div>
                    </div>
                )}
            </Modal>
        </div>
    )
}

export default LocalPorts
