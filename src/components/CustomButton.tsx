import React, { ButtonHTMLAttributes, useState } from 'react'

interface CustomButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'success' | 'warning' | 'danger' | 'outline' | 'ghost' | 'gradient' | 'text' | 'link'
    size?: 'small' | 'middle' | 'large'
    loading?: boolean
    disabled?: boolean
    icon?: React.ReactNode
    iconPosition?: 'left' | 'right'
    fullWidth?: boolean
    ripple?: boolean
    href?: string // link 类型的链接地址
    target?: string // link 类型的打开方式
}

const CustomButton: React.FC<CustomButtonProps> = ({
    children,
    variant = 'primary',
    size = 'middle',
    loading = false,
    disabled = false,
    icon,
    iconPosition = 'left',
    fullWidth = false,
    ripple = true,
    href,
    target = '_self',
    className = '',
    onClick,
    style,
    ...props
}) => {
    const [rippleEffect, setRippleEffect] = useState<{ x: number; y: number } | null>(null)

    const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
        if (ripple) {
            const rect = e.currentTarget.getBoundingClientRect()
            const x = e.clientX - rect.left
            const y = e.clientY - rect.top
            setRippleEffect({ x, y })

            setTimeout(() => setRippleEffect(null), 600)
        }

        onClick?.(e)
    }

    const baseClasses =
        'relative inline-flex items-center justify-center font-medium transition-all duration-200 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden'

    // Ant Design 标准尺寸
    const sizeClasses = {
        small: 'h-7 text-sm px-3 rounded',
        middle: 'h-9 text-base px-4 rounded',
        large: 'h-10 text-base px-6 rounded'
    }

    // 完整的按钮变体，包含 text 和 link 类型
    const variantClasses = {
        // 主要按钮类型
        primary:
            'bg-blue-600 hover:bg-blue-700 text-white border border-blue-600 focus:ring-2 focus:ring-blue-300 shadow-sm',
        success:
            'bg-green-600 hover:bg-green-700 text-white border border-green-600 focus:ring-2 focus:ring-green-300 shadow-sm',
        warning:
            'bg-orange-500 hover:bg-orange-600 text-white border border-orange-500 focus:ring-2 focus:ring-orange-300 shadow-sm',
        danger: 'bg-red-600 hover:bg-red-700 text-white border border-red-600 focus:ring-2 focus:ring-red-300 shadow-sm',

        // 轮廓按钮
        outline:
            'border border-blue-600 text-blue-600 hover:bg-blue-50 bg-transparent focus:ring-2 focus:ring-blue-300',

        // 幽灵按钮
        ghost: 'border border-gray-300 text-gray-700 hover:border-blue-600 hover:text-blue-600 bg-transparent focus:ring-2 focus:ring-blue-300',

        // 渐变按钮
        gradient:
            'bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0 focus:ring-2 focus:ring-blue-300 shadow-sm',

        // 文字按钮 (Ant Design 的 text 类型)
        text: 'text-gray-600 hover:text-gray-800 hover:bg-gray-100 bg-transparent border-0 px-2 focus:ring-0',

        // 链接按钮 (Ant Design 的 link 类型)
        link: 'text-blue-600 hover:text-blue-800 bg-transparent border-0 px-2 focus:ring-0'
    }

    const widthClass = fullWidth ? 'w-full' : ''

    const LoadingSpinner = () => (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            ></path>
        </svg>
    )

    const combinedClasses = `
        ${baseClasses}
        ${variant !== 'text' && variant !== 'link' ? sizeClasses[size] : ''}
        ${variantClasses[variant]}
        ${widthClass}
        ${className}
    `
        .replace(/\s+/g, ' ')
        .trim()

    // 如果是 link 类型且有 href，渲染为 a 标签
    if (variant === 'link' && href) {
        return (
            <a
                href={href}
                target={target}
                className={combinedClasses}
                onClick={handleClick as any}
                style={style}
                {...(props as any)}
            >
                {/* 按钮内容 */}
                {loading && <LoadingSpinner />}
                {!loading && icon && iconPosition === 'left' && <span className="mr-2">{icon}</span>}
                {children}
                {!loading && icon && iconPosition === 'right' && <span className="ml-2">{icon}</span>}
            </a>
        )
    }

    return (
        <button
            className={combinedClasses}
            disabled={disabled || loading}
            onClick={handleClick}
            style={style}
            {...props}
        >
            {/* 涟漪效果 - 不适用于 text 和 link 类型 */}
            {rippleEffect && variant !== 'text' && variant !== 'link' && (
                <span
                    className="absolute bg-white rounded-full animate-ripple"
                    style={{
                        left: rippleEffect.x,
                        top: rippleEffect.y,
                        width: '100px',
                        height: '100px',
                        transform: 'translate(-50%, -50%) scale(0)',
                        opacity: 0.3
                    }}
                />
            )}

            {/* 按钮内容 */}
            {loading && <LoadingSpinner />}
            {!loading && icon && iconPosition === 'left' && <span className="mr-2">{icon}</span>}
            {children}
            {!loading && icon && iconPosition === 'right' && <span className="ml-2">{icon}</span>}
        </button>
    )
}

export default CustomButton
