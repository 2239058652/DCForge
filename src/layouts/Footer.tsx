import React, { useEffect, useState } from 'react'

const Footer: React.FC = () => {
    const [currentDate, setCurrentDate] = useState(() => new Date())

    useEffect(() => {
        const timer = window.setInterval(() => {
            setCurrentDate(new Date())
        }, 1000)

        return () => window.clearInterval(timer)
    }, [])

    return (
        <footer className="app-footer">
            <span>Note Matrix 前端模板</span>
            <span>
                {currentDate.toLocaleDateString('zh-CN', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                })}{' '}
                {currentDate.toLocaleTimeString('zh-CN', {
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit'
                })}
            </span>
        </footer>
    )
}

export default Footer
