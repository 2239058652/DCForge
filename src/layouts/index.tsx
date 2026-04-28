import Footer from '@/layouts/Footer'
import Header from '@/layouts/Header'
import SideBar from '@/layouts/SideBar'
import { Outlet } from 'react-router-dom'

const Layout = () => {
    return (
        <div className="app-shell">
            <Header />
            <div className="app-body">
                <SideBar />
                <main className="app-main">
                    <Outlet />
                </main>
            </div>
            <Footer />
        </div>
    )
}

export default Layout
