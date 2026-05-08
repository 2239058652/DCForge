import { message } from 'antd'
import type { MessageInstance } from 'antd/es/message/interface'
import { createContext, ReactNode, useContext, useEffect, useMemo } from 'react'

interface MessageContextType {
    messageApi: MessageInstance
}

const MessageContext = createContext<MessageContextType | null>(null)
let globalMessageApi: MessageInstance | null = null

export const MessageProvider = ({ children }: { children: ReactNode }) => {
    const [messageApi, contextHolder] = message.useMessage()

    useEffect(() => {
        globalMessageApi = messageApi
        return () => {
            globalMessageApi = null
        }
    }, [messageApi])

    const contextValue = useMemo(() => ({ messageApi }), [messageApi])

    return (
        <MessageContext.Provider value={contextValue}>
            {contextHolder}
            {children}
        </MessageContext.Provider>
    )
}

export const useAppMessage = (): MessageInstance => {
    const context = useContext(MessageContext)

    if (!context) {
        throw new Error('useAppMessage must be used within a MessageProvider')
    }

    return context.messageApi
}

export const getAppMessage = (): MessageInstance => {
    if (!globalMessageApi) {
        throw new Error('getAppMessage() cannot be called before the MessageProvider is mounted.')
    }
    return globalMessageApi
}
