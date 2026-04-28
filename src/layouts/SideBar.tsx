import { Icon } from '@iconify/react'
import { ConfigProvider, Menu } from 'antd'
import type { ItemType } from 'antd/es/menu/interface'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { routeConfig } from '@/router'
import { routesToMenuItems } from '@/utils/menu-generator'

const SideBar = () => {
    const [menuItems, setMenuItems] = useState<ItemType[]>([])
    const [selectedKey, setSelectedKey] = useState('')
    const location = useLocation()
    const navigate = useNavigate()

    useEffect(() => {
        const layoutRoute = routeConfig.find((route) => route.name === 'layout')
        const children = layoutRoute?.children || []
        setMenuItems(routesToMenuItems(children))
        setSelectedKey(location.pathname.replace('/', ''))
    }, [location.pathname])

    const handleMenuClick = ({ key }: { key: string }) => {
        navigate(`/${key}`)
    }

    return (
        <aside className="app-sidebar">
            <div className="sidebar-title">
                <Icon icon="solar:widget-5-bold-duotone" width={18} color="#0891b2" />
                功能模块
            </div>
            <ConfigProvider
                theme={{
                    components: {
                        Menu: {
                            itemSelectedBg: '#ecfeff',
                            itemSelectedColor: '#0e7490',
                            itemHoverBg: '#f1f5f9',
                            itemHoverColor: '#0f172a'
                        }
                    }
                }}
            >
                <Menu
                    className="sidebar-menu"
                    style={{ fontSize: 15 }}
                    inlineIndent={28}
                    mode="inline"
                    items={menuItems}
                    selectedKeys={[selectedKey]}
                    onClick={handleMenuClick}
                />
            </ConfigProvider>
        </aside>
    )
}

export default SideBar
