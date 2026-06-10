import request from '@/request'
import type { ApiResult } from '@/api/user'

export interface InterfacePermissionItem {
    id: number
    httpMethod: string
    urlPattern: string
    permissionCode: string
    type?: number
    description?: string
    createdAt?: string
}

export interface InterfacePermissionPayload {
    id?: number
    httpMethod: string
    urlPattern: string
    permissionCode: string
    type?: number
    description?: string
}

export interface PermissionCodeItem {
    label: string
    value: string
}

export interface InterfacePageParams {
    pageNum?: number
    pageSize?: number
    name?: string
    type?: number
}

export interface InterfacePageResult {
    records: InterfacePermissionItem[]
    total: number
    pageNum: number
    pageSize: number
    pages: number
}

export const interfacePermissionApi = {
    list: () => request.get<ApiResult<InterfacePermissionItem[]>>('/admin/interface-permissions'),
    page: (params: InterfacePageParams) =>
        request.get<ApiResult<InterfacePageResult>>('/admin/interface-permissions/page', params),
    add: (data: InterfacePermissionPayload) => request.post<ApiResult<unknown>>('/admin/interface-permissions', data),
    remove: (id: number) => request.delete<ApiResult<unknown>>(`/admin/interface-permissions/${id}`),
    refresh: () => request.post<ApiResult<unknown>>('/admin/interface-permissions/refresh'),
    getPermissionCodes: () => request.get<ApiResult<PermissionCodeItem[]>>('/dictionary/permission-code/list')
}
