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
    role?: string
    createdAt?: string
}

export interface UserRegisterPayload {
    username: string
    password: string
    nickname?: string
    avatar?: string
}

export const userApi = {
    list: () => request.get<ApiResult<UserItem[]>>('/users/list'),
    register: (data: UserRegisterPayload) => request.post<ApiResult<unknown>>('/users/register', data)
}
