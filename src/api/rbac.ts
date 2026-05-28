import request from '@/request'
import type { ApiResult } from '@/api/user'

export interface RoleItem {
    id: number
    roleCode: string
    roleName: string
    status?: number
    createdAt?: string
    updatedAt?: string
}

export interface PermissionItem {
    id: number
    permissionCode: string
    permissionName: string
    resourceType?: string
    path?: string
    status?: number
    createdAt?: string
    updatedAt?: string
}

export interface RolePayload {
    roleCode: string
    roleName: string
    status?: number
}

export interface PermissionPayload {
    permissionCode: string
    permissionName: string
    resourceType?: string
    path?: string
    status?: number
}

export interface UserRoleAssignPayload {
    userId: number
    roleIds: number[]
}

export interface RolePermissionAssignPayload {
    roleId: number
    permissionIds: number[]
}

export const rbacApi = {
    roles: () => request.get<ApiResult<RoleItem[]>>('/rbac/roles'),
    addRole: (data: RolePayload) => request.post<ApiResult<unknown>>('/rbac/roles', data),
    updateRole: (id: number, data: RolePayload) => request.put<ApiResult<unknown>>(`/rbac/roles/${id}`, data),
    deleteRole: (id: number) => request.delete<ApiResult<unknown>>(`/rbac/roles/${id}`),
    permissions: () => request.get<ApiResult<PermissionItem[]>>('/rbac/permissions'),
    addPermission: (data: PermissionPayload) => request.post<ApiResult<unknown>>('/rbac/permissions', data),
    updatePermission: (id: number, data: PermissionPayload) => request.put<ApiResult<unknown>>(`/rbac/permissions/${id}`, data),
    deletePermission: (id: number) => request.delete<ApiResult<unknown>>(`/rbac/permissions/${id}`),
    assignUserRoles: (data: UserRoleAssignPayload) => request.put<ApiResult<unknown>>('/rbac/user-roles', data),
    assignRolePermissions: (data: RolePermissionAssignPayload) => request.put<ApiResult<unknown>>('/rbac/role-permissions', data),
    getRolePermissions: (roleId: number) => request.get<ApiResult<PermissionItem[]>>(`/rbac/roles/${roleId}/permissions`)
}
