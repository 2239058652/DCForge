import request from '@/request'
import type { ApiResult } from '@/api/user'

export type PortProtocol = 'tcp' | 'udp'
export type ProtocolFilter = 'all' | PortProtocol

export interface LocalPortItem {
    protocol: 'tcp' | 'udp'
    localAddress: string
    localPort: number
    remoteAddress?: string | null
    remotePort?: number | null
    state: string
    pid: number
    processName?: string | null
    executablePath?: string | null
    commandLine?: string | null
    canTerminate: boolean
    terminateBlockedReason?: string | null
}

export interface TerminatePortProcessRequest {
    pid: number
    protocol: 'tcp' | 'udp'
    port: number
    confirm: true
}

export interface TerminatePortProcessResult {
    pid: number
    protocol: 'tcp' | 'udp'
    port: number
    processName?: string | null
    terminated: boolean
    portReleased: boolean
}

export const localPortsApi = {
    list: () =>
        request.get<ApiResult<LocalPortItem[]>>('/api/local-ports'),
    terminate: (data: TerminatePortProcessRequest) =>
        request.post<ApiResult<TerminatePortProcessResult>>('/api/local-ports/terminate', data)
}
