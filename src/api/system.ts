import request from '@/request'

// 获取系统配置
export function systemConfigApi() {
    return request({
        url: '/system/config',
        method: 'get'
    })
}

 