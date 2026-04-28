import Header from '@/layouts/Header'
import Footer from '@/layouts/Footer'
import SideBar from '@/layouts/SideBar'
import { Outlet } from 'react-router-dom'

const Layout = () => {
    return (
        <div className="flex flex-col h-screen w-full overflow-hidden bg-[#f3f4f7]">
            <Header />
            {/* 主要内容区域 - 使用 flex-1 占据剩余空间 */}
            <div className="flex flex-1 min-h-0">
                {/* Sidebar - 固定宽度，高度跟随父容器 */}
                <SideBar />

                {/* 内容区域 - 可滚动 */}
                <div className="flex-1 min-h-0 overflow-auto p-4">
                    <Outlet />
                </div>
            </div>
            <Footer />
        </div>
    )
}

export default Layout
