import { processRoutes } from '@/hooks/RoutesHook'
import Layout from '@/layouts'
import Login from '@/views/Login'
import NotFound from '@/views/NotFound'
import Notes from '@/views/Notes'
import Rbac from '@/views/Rbac'
import Schedule from '@/views/Schedule'
import StaffManagement from '@/views/StaffManagement'
import System from '@/views/System'
import Users from '@/views/Users'
import Dictionary from '@/views/Dictionary'
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
                path: 'clinic',
                name: 'clinic',
                meta: {
                    title: '诊所管理',
                    requiredAuth: true
                },
                children: [
                    {
                        path: '',
                        element: <Navigate to="/clinic/schedule" replace />
                    },
                    {
                        path: 'schedule',
                        element: <Schedule />,
                        meta: {
                            title: '排班表',
                            requiredAuth: true
                        }
                    },
                    {
                        path: 'staff',
                        element: <StaffManagement />,
                        meta: {
                            title: '员工管理',
                            requiredAuth: true
                        }
                    }
                ]
            },
            {
                path: 'settings',
                name: 'settings',
                meta: {
                    title: '系统设置',
                    requiredAuth: true
                },
                children: [
                    {
                        path: '',
                        element: <Navigate to="/settings/users" replace />
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
                    },
                    {
                        path: 'system',
                        element: <System />,
                        meta: {
                            title: '接口权限',
                            requiredAuth: true
                        }
                    },
                    {
                        path: 'dictionary',
                        element: <Dictionary />,
                        meta: {
                            title: '字典管理',
                            requiredAuth: true
                        }
                    }
                ]
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
