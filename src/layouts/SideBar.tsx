import { Icon } from '@iconify/react'
import { ConfigProvider, Menu } from 'antd'
import { useMemo } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { routeConfig } from '@/router'
import { routesToMenuItems } from '@/utils/menu-generator'

const SideBar = () => {
    const location = useLocation()
    const navigate = useNavigate()
    const selectedKey = location.pathname.replace('/', '')
    const menuItems = useMemo(() => {
        const layoutRoute = routeConfig.find((route) => route.name === 'layout')
        return routesToMenuItems(layoutRoute?.children || [])
    }, [])

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
