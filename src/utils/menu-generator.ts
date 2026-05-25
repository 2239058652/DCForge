import type { ItemType } from 'antd/es/menu/interface'

/**
 * 将路由配置转换为菜单项，支持二级嵌套菜单
 * @param routes - 路由配置数组
 * @param parentPath - 父级路径前缀，用于拼接子菜单 key
 * @returns 返回转换后的菜单项数组
 */
export function routesToMenuItems(routes: AppRoute[], parentPath = ''): ItemType[] {
    return routes
        .filter((route) => !route.meta?.hiddenRoute && !route.meta?.hidden && route.path !== '')
        .map((route) => {
            const fullPath = parentPath ? `${parentPath}/${route.path}` : route.path

            // 有 children 且非空 → 渲染为 SubMenu
            const visibleChildren = (route.children || []).filter(
                (child) => !child.meta?.hiddenRoute && !child.meta?.hidden
            )

            if (visibleChildren.length > 0) {
                return {
                    key: fullPath,
                    label: route.meta?.title ?? route.path,
                    children: routesToMenuItems(visibleChildren, fullPath)
                }
            }

            // 叶子节点
            return {
                key: fullPath,
                label: route.meta?.title ?? route.path
            }
        })
}
