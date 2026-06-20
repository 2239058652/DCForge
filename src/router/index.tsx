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
import Claude from '@/views/Claude'
import ClaudeConversations from '@/views/Claude/Conversations'
import ClaudeConversationDetail from '@/views/Claude/ConversationDetail'
import AiImage from '@/views/AiImage'
import ImageHistory from '@/views/ImageHistory'
import LocalPorts from '@/views/LocalPorts'
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
                path: 'ai',
                name: 'ai',
                meta: {
                    title: 'AI服务',
                    requiredAuth: true
                },
                children: [
                    {
                        path: '',
                        element: <Navigate to="/ai/image" replace />
                    },
                    {
                        path: 'image',
                        element: <AiImage />,
                        meta: {
                            title: '图像管理',
                            requiredAuth: true
                        }
                    },
                    {
                        path: 'image/history',
                        element: <ImageHistory />,
                        meta: {
                            title: '图片历史',
                            requiredAuth: true
                        }
                    }
                ]
            },
            {
                path: 'machine',
                name: 'machine',
                meta: {
                    title: '本机管理',
                    requiredAuth: true
                },
                children: [
                    {
                        path: '',
                        element: <Navigate to="/machine/claude" replace />
                    },
                    {
                        path: 'local-ports',
                        element: <LocalPorts />,
                        meta: {
                            title: '本机端口',
                            requiredAuth: true
                        }
                    },
                    {
                        path: 'claude',
                        element: <Claude />,
                        meta: {
                            title: 'Claude管理',
                            requiredAuth: true
                        }
                    },
                    {
                        path: 'claude/:projectDirName/conversations',
                        element: <ClaudeConversations />,
                        meta: {
                            title: '对话列表',
                            requiredAuth: true,
                            hidden: true
                        }
                    },
                    {
                        path: 'claude/:projectDirName/conversations/:sessionId',
                        element: <ClaudeConversationDetail />,
                        meta: {
                            title: '对话详情',
                            requiredAuth: true,
                            hidden: true
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
