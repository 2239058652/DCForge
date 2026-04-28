import { getAppMessage } from '@/contexts/MessageContext'
import dayjs from 'dayjs'

/**
 * 复制文本到剪贴板
 * @param text 要复制的文本
 * @returns Promise<void>，成功 resolve，失败 reject
 */
export function copyToClipboard(text: string): Promise<void> {
    const messageApi = getAppMessage()
    messageApi.destroy() // 清除之前的消息
    if (!text) {
        messageApi.warning('复制内容为空')
        return navigator.clipboard.writeText('')
    }

    // 优先使用现代异步 Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
        messageApi.success('复制成功')
        return navigator.clipboard.writeText(text)
    }

    // fallback 老方法
    return new Promise((resolve, reject) => {
        try {
            const textarea = document.createElement('textarea')
            textarea.value = text
            // 避免页面滚动
            textarea.style.position = 'fixed'
            textarea.style.top = '0'
            textarea.style.left = '0'
            textarea.style.width = '1px'
            textarea.style.height = '1px'
            textarea.style.padding = '0'
            textarea.style.border = 'none'
            textarea.style.outline = 'none'
            textarea.style.boxShadow = 'none'
            textarea.style.background = 'transparent'
            document.body.appendChild(textarea)
            textarea.select()
            const successful = document.execCommand('copy')
            document.body.removeChild(textarea)
            if (successful) {
                resolve()
            } else {
                reject(new Error('复制失败'))
            }
        } catch (err) {
            messageApi.error('复制失败，请手动复制')
            reject(err)
        }
    })
}

// 格式化日期时间，返回 'YYYY年MM月DD日 HH:mm' 格式的字符串,默认格式为 'YYYY年MM月DD日 HH:mm',可以传入自定义格式
export const formatDateTime = (isoString: string, format: string = 'YYYY.MM.DD HH:mm'): string => {
    if (!isoString) return ''
    return dayjs(isoString).format(format)
}

/**
 * 去掉首尾空格，如果全是空格则返回空字符串
 */
export const trimString = (value: any): string => {
    if (typeof value !== 'string') return value
    return value.trim() || ''
}

// 辅助函数：过滤掉值为 null 或 undefined 的属性
export const filterEmptyValues = <T extends Record<string, any>>(obj: T): Partial<T> => {
    const filtered: Partial<T> = {}
    for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key) && obj[key] != null) {
            filtered[key] = obj[key]
        }
    }
    return filtered
}
