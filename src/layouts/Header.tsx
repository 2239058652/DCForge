import { Icon } from '@iconify/react'
import { Avatar, Button } from 'antd'
import React from 'react'
import { useNavigate } from 'react-router-dom'

interface StoredUser {
    username?: string
    nickname?: string
    avatar?: string
}

const Header: React.FC = () => {
    const navigate = useNavigate()
    const userInfo = localStorage.getItem('userInfo')
    const user: StoredUser | null = userInfo ? JSON.parse(userInfo) : null

    const handleLogout = () => {
        localStorage.clear()
        navigate('/login')
    }

    return (
        <header className="app-header">
            <div className="brand-icon">
                <Icon icon="solar:notes-bold-duotone" width={22} />
            </div>
            <div className="app-header-title" onClick={() => navigate('/')}>
                Note Matrix
            </div>
            <div className="app-header-user">
                <Avatar size={24} src={user?.avatar} icon={<Icon icon="solar:user-rounded-bold" />} />
                <span>{user?.nickname || user?.username || 'admin'}</span>
                <Button size="small" icon={<Icon icon="solar:logout-2-linear" />} onClick={handleLogout}>
                    退出
                </Button>
            </div>
        </header>
    )
}

export default Header
