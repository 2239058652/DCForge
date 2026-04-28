import React, { useState, useEffect, useRef } from 'react'

const Footer: React.FC = () => {
    const [currentTime, setCurrentTime] = useState(() =>
        new Date().toLocaleTimeString('zh-CN', {
            hour: 'numeric',
            minute: 'numeric',
            second: 'numeric'
        })
    )

    const rafRef = useRef<number>(0)
    const lastUpdateRef = useRef<number>(Date.now())

    const updateTime = () => {
        const now = Date.now()
        // 每整秒更新一次（避免每帧都格式化字符串，减少计算）
        if (now - lastUpdateRef.current >= 1000) {
            setCurrentTime(
                new Date().toLocaleTimeString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                })
            )
            lastUpdateRef.current = now
        }
        rafRef.current = requestAnimationFrame(updateTime)
    }

    useEffect(() => {
        rafRef.current = requestAnimationFrame(updateTime)
        return () => cancelAnimationFrame(rafRef.current)
    }, [])

    const currentDate = new Date().toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    })

    return (
        <footer className="bg-[#1A2B4D] h-12 py-4 text-gray-300">
            <div className="max-w-[1200px] mx-auto px-8 flex items-center justify-between">
                <p className="text-sm">© 2025 工业互联网教学实验平台 | 教育部工业互联网实验室</p>
                <div className="flex items-center gap-4">
                    <span className="text-sm">技术支持: 中软国际教育</span>
                    <span className="text-sm">
                        当前日期: {currentDate} {currentTime}
                    </span>
                </div>
            </div>
        </footer>
    )
}

export default Footer
