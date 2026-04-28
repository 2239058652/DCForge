// 路由 meta 类型
interface RouteMeta {
    title?: string
    requiredAuth?: boolean
    hidden?: boolean
    hiddenRoute?: boolean
    requiresState?: boolean
}

// 路由项类型（可递归）
interface AppRoute {
    path: string
    name?: string
    element?: ReactNode
    meta?: RouteMeta
    children?: AppRoute[]
}
