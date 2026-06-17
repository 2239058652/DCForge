import request from '@/request'

export interface TaskSubmitData {
    type: 'text2img' | 'img2img'
    prompt: string
    size?: string
    images?: string[]
}

export interface TaskResult {
    id: number
    type: 'text2img' | 'img2img'
    status: string
    prompt: string
    size: string
    createdAt: string
}

export interface TaskDetail {
    id: number
    type: 'text2img' | 'img2img'
    status: string
    imageUrl: string | null
    revisedPrompt: string | null
    errorMessage: string | null
    prompt: string
    size: string
    createdAt: string
}

export interface TaskPageParams {
    pageNum: number
    pageSize: number
    status?: string
    type?: 'text2img' | 'img2img'
}

export interface TaskPageResult {
    total: number
    pageNum: number
    pageSize: number
    records: TaskDetail[]
}

export interface SaveImageHistoryPayload {
    taskId: number
    sourceImageUrls?: string[]
}

export interface HistoryItem {
    id: number
    type: 'text2img' | 'img2img'
    prompt: string
    revisedPrompt: string | null
    sourceImageUrl: string | null
    imageUrl: string
    size: string
    createdAt: string
}

export interface HistoryPageResult {
    total: number
    pageNum: number
    pageSize: number
    records: HistoryItem[]
}

export interface HistoryPageParams {
    pageNum: number
    pageSize: number
    type?: 'text2img' | 'img2img'
}

const AI_TIMEOUT = 1000 * 60 * 6

export const aiImageApi = {
    // 异步任务接口
    submitTask: (data: TaskSubmitData) =>
        request.post<{ code: number; data: TaskResult }>('/ai/task/submit', data, { timeout: AI_TIMEOUT }),
    getTask: (id: number) =>
        request.get<{ code: number; data: TaskDetail }>(`/ai/task/${id}`),
    getTaskPage: (params: TaskPageParams) =>
        request.get<{ code: number; data: TaskPageResult }>('/ai/task/page', params),
    deleteTask: (id: number) =>
        request.delete<{ code: number; message: string; data: null }>(`/ai/task/${id}`),
    uploadImage: (file: File) => {
        const formData = new FormData()
        formData.append('file', file)
        return request.post<{ code: number; message: string; data: string }>('/ai/image/upload', formData)
    },

    // 图片历史接口（保存/删除）
    saveImageHistory: (data: SaveImageHistoryPayload) =>
        request.post<{ code: number; message: string; data: HistoryItem }>('/ai/image/history/save', data),
    getImageHistoryPage: (params: HistoryPageParams) =>
        request.get<{ code: number; message: string; data: HistoryPageResult }>('/ai/image/history/page', params),
    deleteImageHistory: (id: number) =>
        request.delete<{ code: number; message: string; data: null }>(`/ai/image/history/${id}`)
}
