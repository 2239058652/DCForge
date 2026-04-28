import { Icon } from '@iconify/react'
import { Button } from 'antd'
import React from 'react'
import { useNavigate } from 'react-router-dom'

const Header: React.FC = () => {
    const navigate = useNavigate()

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
                <span>admin</span>
                <Button size="small" icon={<Icon icon="solar:logout-2-linear" />} onClick={handleLogout}>
                    退出
                </Button>
            </div>
        </header>
    )
}

export default Header
