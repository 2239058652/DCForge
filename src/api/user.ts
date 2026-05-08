import request from '@/request'

export interface ApiResult<T> {
    code: number
    message: string
    data: T
}

export interface UserItem {
    id: number
    username: string
    nickname?: string
    avatar?: string
    status?: number
    roles?: string[]
    createdAt?: string
}

export interface UserRegisterPayload {
    username: string
    password: string
    nickname?: string
    avatar?: string
}

export interface UserLoginPayload {
    username: string
    password: string
}

export interface UserLoginResult {
    token: string
    id: number
    username: string
    nickname?: string
    avatar?: string
    roles?: string[]
    permissions?: string[]
}

export const userApi = {
    list: () => request.get<ApiResult<UserItem[]>>('/users/list'),
    register: (data: UserRegisterPayload) => request.post<ApiResult<unknown>>('/users/register', data),
    login: (data: UserLoginPayload) => request.post<ApiResult<UserLoginResult>>('/users/login', data)
}
