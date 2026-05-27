import request from '@/request'
import type { ApiResult } from '@/api/user'

export interface InterfacePermissionItem {
    id: number
    httpMethod: string
    urlPattern: string
    permissionCode: string
    description?: string
    createdAt?: string
}

export interface InterfacePermissionPayload {
    httpMethod: string
    urlPattern: string
    permissionCode: string
    description?: string
}

export interface PermissionCodeItem {
    label: string
    value: string
}

export const interfacePermissionApi = {
    list: () => request.get<ApiResult<InterfacePermissionItem[]>>('/admin/interface-permissions'),
    add: (data: InterfacePermissionPayload) => request.post<ApiResult<unknown>>('/admin/interface-permissions', data),
    remove: (id: number) => request.delete<ApiResult<unknown>>(`/admin/interface-permissions/${id}`),
    refresh: () => request.post<ApiResult<unknown>>('/admin/interface-permissions/refresh'),
    getPermissionCodes: () => request.get<ApiResult<PermissionCodeItem[]>>('/dictionary/permisson-code/list')
}
