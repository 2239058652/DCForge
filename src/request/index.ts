import axios, { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

// 创建axios实例
const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 1000 * 60,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        return config
    },
    (err) => Promise.reject(err)
)

// 响应拦截器
instance.interceptors.response.use(
    (res: AxiosResponse) => {
        // 如果是文件流，直接返回，调用方手动处理
        if (res.request?.responseType === 'blob') {
            return res
        }

        const result = res.data

        return result
    },
    (err) => {
        return err.response.data
    }
)

// 通用请求函数（支持直接传 config）
const request = <T = any>(config: AxiosRequestConfig): Promise<T> => {
    return instance.request(config)
}

// 快捷方法封装
request.get = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> =>
    instance.get(url, { ...config, params })

request.post = <T = any>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> =>
    instance.post(url, data, config)

request.put = <T = any>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> =>
    instance.put(url, data, config)

request.delete = <T = any>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> =>
    instance.delete(url, { ...config, params })

export default request
