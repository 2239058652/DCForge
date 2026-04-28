import { JSX, ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

// 处理用户认证逻辑的 React 组件，其主要作用是根据用户是否已经登录来决定是否允许访问某个路由
export function AuthRoute({ children }: { children: JSX.Element }) {
    const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
    const token = localStorage.getItem(tokenName)
    const location = useLocation()

    if (!token || token === 'undefined') {
        localStorage.clear()
        return <Navigate to="/login" state={{ from: location.pathname }} replace />
    }

    return children
}

// 处理路由配置中需要 state 参数的逻辑，如果需要，则检查当前路由的 state 是否存在，如不存在，则重定向到 404 页面
export const RequireStateParam: React.FC<{ children: ReactNode }> = ({ children }) => {
    const location = useLocation()
    const state = location.state

    if (!state) {
        return <Navigate to="/notfound" replace />
    }

    return <>{children}</>
}

// 过滤和处理路由配置
export const processRoutes = (routes: AppRoute[]): AppRoute[] => {
    return routes.reduce<AppRoute[]>((acc, route) => {
        if (route.meta?.hiddenRoute) {
            return acc
        }

        const newRoute: AppRoute = { ...route }

        // 权限处理
        if (newRoute.meta?.requiredAuth && newRoute.element) {
            newRoute.element = <AuthRoute>{newRoute.element}</AuthRoute>
        }

        // state 参数要求
        if (newRoute.meta?.requiresState && newRoute.element) {
            newRoute.element = <RequireStateParam>{newRoute.element}</RequireStateParam>
        }

        if (newRoute.children) {
            newRoute.children = processRoutes(newRoute.children)
        }

        acc.push(newRoute)
        return acc
    }, [])
}
