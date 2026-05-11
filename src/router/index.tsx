import { processRoutes } from '@/hooks/RoutesHook'
import Layout from '@/layouts'
import Login from '@/views/Login'
import NotFound from '@/views/NotFound'
import Notes from '@/views/Notes'
import Rbac from '@/views/Rbac'
import Schedule from '@/views/Schedule'
import Users from '@/views/Users'
import { createBrowserRouter, Navigate } from 'react-router-dom'

export const routeConfig = processRoutes([
    {
        path: '/',
        name: 'layout',
        element: <Layout />,
        children: [
            {
                path: '',
                element: <Navigate to="/notes" replace />
            },
            {
                path: 'notes',
                element: <Notes />,
                meta: {
                    title: 'Note 管理',
                    requiredAuth: true
                }
            },
            {
                path: 'schedule',
                element: <Schedule />,
                meta: {
                    title: '排班管理',
                    requiredAuth: true
                }
            },
            {
                path: 'users',
                element: <Users />,
                meta: {
                    title: '用户管理',
                    requiredAuth: true
                }
            },
            {
                path: 'rbac',
                element: <Rbac />,
                meta: {
                    title: '权限管理',
                    requiredAuth: true
                }
            }
        ]
    },
    {
        path: '/login',
        element: <Login />
    },
    {
        path: '*',
        element: <NotFound />
    }
])

export const router = createBrowserRouter(routeConfig)
