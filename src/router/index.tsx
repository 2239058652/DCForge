import NotFound from '@/views/NotFound'
import { processRoutes } from '@/hooks/RoutesHook'
import Layout from '@/layouts'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import DashBord from '@/views/Dashbord'

/**
 * 路由配置说明
 * requiredAuth: 是否需要登录才能查看路由 , 默认为false 需要登录才能查看路由
 * hidden: 是否在顶栏菜单中显示, 默认为false 显示，true则不显示,但是路由还在
 * hiddenRoute: 是否显示此路由，默认为false 显示，true则不显示，并且路由也不存在
 * requiresState: 是否需要传参才能跳转路由
 */
export const routeConfig = processRoutes([
    {
        path: '/',
        name: 'layout',
        element: <Layout />,
        children: [
            {
                path: '',
                element: <Navigate to="/home" replace />
            },
            {
                path: 'home', // 默认子路由 || index: true,
                element: <DashBord />,
                meta: {
                    title: 'home'
                }
            }
        ]
    },
    {
        path: '*',
        element: <NotFound />
    }
])

// 这是给 RouterProvider 用的
export const router = createBrowserRouter(routeConfig)
