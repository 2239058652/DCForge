import React from 'react'
import { Pagination, PaginationProps } from 'antd'

export interface CommonPaginationProps extends PaginationProps {
    showSizeChanger?: boolean
    showQuickJumper?: boolean
    showTotal?: (total: number, range: [number, number]) => React.ReactNode
    className?: string
    style?: React.CSSProperties
}

const CustomPagination: React.FC<CommonPaginationProps> = ({
    total = 0,
    showSizeChanger = true,
    showQuickJumper = true,
    showTotal,
    className,
    style,
    ...restProps
}) => {
    return (
        <div className={`flex justify-end mt-4 ${className || ''}`} style={{ ...style }}>
            <Pagination
                className="shrink-0 whitespace-nowrap"
                total={total}
                showSizeChanger={showSizeChanger}
                showQuickJumper={showQuickJumper}
                showTotal={showTotal ?? ((total) => `共 ${total} 条`)}
                {...restProps}
            />
        </div>
    )
}

export default CustomPagination
