import { createRoot } from 'react-dom/client'
import 'antd/dist/reset.css'
import './index.css'
import '@/assets/styles/reset.css'
import '@/assets/styles/global.css'
import { ConfigProvider, App as AntdApp } from 'antd'
import zh from 'antd/es/locale/zh_CN'
import { MessageProvider } from '@/contexts/MessageContext'
import { RouterProvider } from 'react-router-dom'
import { router } from './router'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn' // 引入中文 locale

dayjs.locale('zh-cn') //  设置全局中文

createRoot(document.getElementById('root')!).render(
    <ConfigProvider
        locale={zh}
        theme={{
            token: {}
        }}
    >
        <AntdApp>
            <MessageProvider>
                <RouterProvider router={router} />
            </MessageProvider>
        </AntdApp>
    </ConfigProvider>
)
