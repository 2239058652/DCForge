import React from 'react'
import { useNavigate } from 'react-router-dom'

const Header: React.FC = () => {
    const navigate = useNavigate()
    return (
        <header className="bg-[#1A2B4D] h-16 flex items-center px-8">
            <div className="flex items-center gap-3">
                <h1 className="text-white text-xl font-bold hover:cursor-pointer" onClick={() => navigate('/')}>
                    AI工业互联网教学实验平台
                </h1>
            </div>
            {/* <nav className="ml-auto flex items-center gap-6">
                <a className="text-gray-300 hover:text-white text-base">？</a>
                <div className="flex items-center gap-2 cursor-pointer">
                    <Icon icon="carbon:user-avatar" className="text-gray-300" width="24" height="24" />
                    <span className="text-gray-300">管理员</span>
                </div>
            </nav> */}
        </header>
    )
}

export default Header
