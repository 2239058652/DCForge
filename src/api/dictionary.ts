import request from '@/request'
import type { ApiResult } from '@/api/user'

export interface DictOption {
    label: string
    value: string
}

export interface DictItem {
    id: number
    dictCode: string
    dictLabel: string
    dictValue: string
    sortOrder: number
    status: number
    createdAt?: string
}

export interface DictPayload {
    id?: number | null
    dictCode: string
    dictLabel: string
    dictValue: string
    sortOrder: number
    status: number
}

export interface DictPageParams {
    pageNum?: number
    pageSize?: number
    dictCode?: string
}

export interface DictPageResult {
    records: DictItem[]
    total: number
    pageNum: number
    pageSize: number
    pages: number
}

export const dictionaryApi = {
    page: (params: DictPageParams) =>
        request.get<ApiResult<DictPageResult>>('/dictionary/page', params),
    getByCode: (dictCode: string) =>
        request.get<ApiResult<DictOption[]>>(`/dictionary/${dictCode}`),
    save: (data: DictPayload) =>
        request.post<ApiResult<unknown>>('/dictionary', data),
    remove: (id: number) =>
        request.delete<ApiResult<unknown>>(`/dictionary/${id}`)
}
