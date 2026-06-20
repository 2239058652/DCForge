import request from '@/request'
import type { ApiResult } from '@/api/user'

/** 项目概要 */
export interface ClaudeProjectItem {
    projectPath: string
    dirName: string
    conversationCount: number
    lastModified: string
    lastConversationTime: string
}

/** 项目分页参数 */
export interface ClaudeProjectPageParams {
    pageNum: number
    pageSize: number
    projectPath?: string
}

/** 后端分页结果 */
export interface PageResult<T> {
    total: number
    records: T[]
    pageNum: number
    pageSize: number
}

/** 对话概要 */
export interface ClaudeConversationItem {
    sessionId: string
    title: string
    messageCount: number
    lastActivity: string
}

/** 消息内容块 */
export interface ClaudeMessageContentBlock {
    type: string
    text?: string
}

/** 单条消息 */
export interface ClaudeMessage {
    type: string
    timestamp?: string
    message?: {
        role?: string
        content?: string | ClaudeMessageContentBlock[]
    }
    uuid?: string
    [key: string]: unknown
}

/** 对话详情 */
export interface ClaudeConversationDetail {
    sessionId: string
    projectPath: string
    startTime: string
    lastActivity: string
    messageCount: number
    messages: ClaudeMessage[]
}

export const claudeApi = {
    /** 分页查询项目列表 */
    getProjects: (params: ClaudeProjectPageParams) =>
        request.get<ApiResult<PageResult<ClaudeProjectItem>>>('/claude/projects', params),

    /** 删除项目 */
    deleteProject: (projectDirName: string) =>
        request.delete<ApiResult<unknown>>(`/claude/projects/${projectDirName}`),

    /** 查询项目下的对话列表 */
    getConversations: (projectDirName: string) =>
        request.get<ApiResult<ClaudeConversationItem[]>>(`/claude/projects/${projectDirName}/conversations`),

    /** 查询对话详情 */
    getConversationDetail: (projectDirName: string, sessionId: string) =>
        request.get<ApiResult<ClaudeConversationDetail>>(
            `/claude/projects/${projectDirName}/conversations/${sessionId}`
        ),

    /** 删除对话 */
    deleteConversation: (projectDirName: string, sessionId: string) =>
        request.delete<ApiResult<unknown>>(
            `/claude/projects/${projectDirName}/conversations/${sessionId}`
        )
}
