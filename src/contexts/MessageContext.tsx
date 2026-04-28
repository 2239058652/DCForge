import { createContext, useContext, ReactNode, useMemo } from 'react'
import { message } from 'antd'
import { MessageInstance } from 'antd/es/message/interface'

// 1. 定义 Context 的类型
interface MessageContextType {
    messageApi: MessageInstance
}

// 2. 创建 Context，并给一个默认值
//    这里我们用一个空对象作为默认值，并通过 isInitialized 标志来确保安全
const MessageContext = createContext<MessageContextType | null>(null)

// --- 为非组件环境提供的全局访问器 ---
let globalMessageApi: MessageInstance | null = null

// 在 Provider 中设置全局实例
export const MessageProvider = ({ children }: { children: ReactNode }) => {
    const [messageApi, contextHolder] = message.useMessage()

    // 设置全局实例
    globalMessageApi = messageApi

    const contextValue = useMemo(() => ({ messageApi }), [messageApi])

    return (
        <MessageContext.Provider value={contextValue}>
            {contextHolder}
            {children}
        </MessageContext.Provider>
    )
}

// 4. 创建一个自定义 Hook 来消费 Context
export const useAppMessage = (): MessageInstance => {
    const context = useContext(MessageContext)

    // 如果 context 不存在，说明 Hook 没有在 Provider 内部被调用
    if (!context) {
        throw new Error('useAppMessage must be used within a MessageProvider')
    }

    return context.messageApi
}

// 导出一个全局访问器
export const getAppMessage = (): MessageInstance => {
    if (!globalMessageApi) {
        // 如果在 Provider 初始化前调用，说明有代码逻辑错误
        throw new Error('getAppMessage() cannot be called before the MessageProvider is mounted.')
    }
    return globalMessageApi
}
