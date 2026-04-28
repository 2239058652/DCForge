import { processRoutes } from '@/hooks/RoutesHook'
import Layout from '@/layouts'
import Login from '@/views/Login'
import NotFound from '@/views/NotFound'
import Notes from '@/views/Notes'
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
