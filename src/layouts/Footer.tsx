import React, { useEffect, useRef, useState } from 'react'

const Footer: React.FC = () => {
    const [currentTime, setCurrentTime] = useState(() =>
        new Date().toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        })
    )

    const rafRef = useRef<number>(0)
    const lastUpdateRef = useRef<number>(Date.now())

    useEffect(() => {
        const updateTime = () => {
            const now = Date.now()
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

        rafRef.current = requestAnimationFrame(updateTime)
        return () => cancelAnimationFrame(rafRef.current)
    }, [])

    const currentDate = new Date().toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    })

    return (
        <footer className="app-footer">
            <span>Note Matrix 前端模板</span>
            <span>
                {currentDate} {currentTime}
            </span>
        </footer>
    )
}

export default Footer
