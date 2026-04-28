import type { ItemType } from 'antd/es/menu/interface'

/**
 * 将路由配置转换为菜单项
 * @param routes - 路由配置数组，类型为 AppRoute[]
 * @returns 返回转换后的菜单项数组，类型为 ItemType[]
 */
export function routesToMenuItems(routes: AppRoute[]): ItemType[] {
    // 首先过滤掉不需要显示的路由
    // 条件为：非隐藏路由、非隐藏页面、路径不为空
    return (
        routes
            .filter((route) => !route.meta?.hiddenRoute && !route.meta?.hidden && route.path !== '')
            // 将过滤后的路由映射为菜单项格式
            .map((route) => ({
                // 使用路由路径作为菜单项的key
                key: route.path,
                // 使用路由元信息中的标题作为菜单项的label，如果没有标题则使用路径作为label
                label: route.meta?.title ?? route.path
            }))
    )
}
