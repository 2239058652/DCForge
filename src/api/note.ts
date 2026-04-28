import request from '@/request'

export interface ApiResult<T> {
    code: number
    message: string
    data: T
}

export interface NoteItem {
    id: number
    content: string
    createdAt?: string
    updatedAt?: string
}

export interface NotePayload {
    content: string
}

export interface NotePageParams {
    pageNum: number
    pageSize: number
    content?: string
}

export interface NotePageResult {
    total: number
    records: NoteItem[]
    pageNum: number
    pageSize: number
}

export const noteApi = {
    list: () => request.get<ApiResult<NoteItem[]>>('/notes/list'),
    page: (params: NotePageParams) => request.get<ApiResult<NotePageResult>>('/notes/page', params),
    add: (data: NotePayload) => request.post<ApiResult<unknown>>('/notes/add', data),
    delete: (id: number) => request.delete<ApiResult<unknown>>(`/notes/delete/${id}`),
    find: (id: number) => request.get<ApiResult<NoteItem>>(`/notes/find/${id}`),
    update: (id: number, data: NotePayload) => request.put<ApiResult<unknown>>(`/notes/update/${id}`, data)
}
