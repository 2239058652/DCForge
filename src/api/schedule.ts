import request from '@/request'
import type { ApiResult } from '@/api/user'

export type StaffType = 0 | 1 | 2
export type ShiftType = 0 | 1 | 2

export interface StaffItem {
    id: number
    name: string
    type: StaffType
    restDay: number
    nightOrder: number
    isActive?: boolean
    createdAt?: string
    updatedAt?: string
}

export interface StaffPayload {
    name: string
    type: StaffType
    restDay: number
    nightOrder?: number
}

export interface ShiftItem {
    staffId: number
    staffName: string
    staffType: StaffType
    shiftType: ShiftType
    isSwapped?: boolean
}

export interface DailyScheduleItem {
    date: string
    shifts: ShiftItem[]
}

export interface ScheduleItem {
    id: number
    staffId: number
    shiftDate: string
    shiftType: ShiftType
    isSwapped?: boolean
    createdAt?: string
}

export interface GenerateSchedulePayload {
    year: number
    month: number
    forceOverwrite: boolean
}

export interface RotaStateGroup {
    queue: StaffItem[]
    nextStaffId?: number | null
}

export interface RotaStateResult {
    doctor?: RotaStateGroup
    nurse?: RotaStateGroup
    receptionist?: RotaStateGroup
}

export const scheduleApi = {
    staffList: () => request.get<ApiResult<StaffItem[]>>('/api/staff'),
    addStaff: (data: StaffPayload) => request.post<ApiResult<StaffItem>>('/api/staff', data),
    updateStaff: (id: number, data: StaffPayload) => request.put<ApiResult<StaffItem>>(`/api/staff/${id}`, data),
    deactivateStaff: (id: number) => request.put<ApiResult<unknown>>(`/api/staff/${id}/deactivate`),
    activateStaff: (id: number) => request.put<ApiResult<unknown>>(`/api/staff/${id}/activate`),
    generate: (data: GenerateSchedulePayload) => request.post<ApiResult<unknown>>('/api/schedule/generate', data),
    monthly: (year: number, month: number) =>
        request.get<ApiResult<DailyScheduleItem[]>>('/api/schedule', { year, month }),
    staffSchedule: (staffId: number, year: number, month: number) =>
        request.get<ApiResult<ScheduleItem[]>>(`/api/schedule/staff/${staffId}`, { year, month }),
    deleteMonth: (year: number, month: number) => request.delete<ApiResult<unknown>>('/api/schedule', { year, month }),
    rotaState: () => request.get<ApiResult<RotaStateResult>>('/api/rota-state')
}
