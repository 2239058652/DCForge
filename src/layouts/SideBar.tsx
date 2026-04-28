import { ConfigProvider, Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import type { ItemType } from 'antd/es/menu/interface'
import { routeConfig } from '@/router'
import { routesToMenuItems } from '@/utils/menu-generator'

const SideBar = () => {
    const [menuItems, setMenuItems] = useState<ItemType[]>([])
    const [selectedKey, setSelectedKey] = useState('')
    const location = useLocation()
    const navigate = useNavigate()

    useEffect(() => {
        // 获取 layout 下的子路由
        const layoutRoute = routeConfig.find((r) => r.name === 'layout')
        const children = layoutRoute?.children || []

        // 使用工具方法生成菜单
        const items = routesToMenuItems(children)
        setMenuItems(items)

        // 设置默认选中
        const current = location.pathname.replace('/', '')
        setSelectedKey(current)
    }, [location.pathname])

    const handleMenuClick = ({ key }: { key: string }) => {
        navigate(`/${key}`)
    }

    return (
        <div className="w-60 h-full bg-white">
            <ConfigProvider
                theme={{
                    components: {
                        Menu: {
                            itemSelectedBg: '#ecf7ff',
                            itemHoverBg: '#f0f7ff',
                            itemActiveBg: '#ecf7ff'
                        }
                    }
                }}
            >
                <Menu
                    className="h-full"
                    style={{ fontSize: 16 }}
                    inlineIndent={40}
                    mode="inline"
                    items={menuItems}
                    selectedKeys={[selectedKey]}
                    onClick={handleMenuClick}
                />
            </ConfigProvider>
        </div>
    )
}

export default SideBar
