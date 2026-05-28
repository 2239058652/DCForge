import { useMemo, useState } from 'react'
import { Image, Popover } from 'antd'
import type { RichTextPreviewProps } from './types'
import './index.css'

const RichTextPreview = ({ content, maxLength = 100, showPopover = false }: RichTextPreviewProps) => {
    const [popoverOpen, setPopoverOpen] = useState(false)

    // 提取纯文本用于显示（去除 HTML 标签）
    const plainText = useMemo(() => {
        if (!content) return ''

        // 创建一个临时 div 来解析 HTML
        const tempDiv = document.createElement('div')
        tempDiv.innerHTML = content

        // 移除图片标签（缩略图单独展示）
        tempDiv.querySelectorAll('img').forEach((img) => img.remove())

        return tempDiv.textContent || tempDiv.innerText || ''
    }, [content])

    // 提取图片列表
    const images = useMemo(() => {
        if (!content) return []

        const tempDiv = document.createElement('div')
        tempDiv.innerHTML = content
        const imgElements = tempDiv.querySelectorAll('img')

        return Array.from(imgElements).map((img, index) => ({
            id: index,
            src: img.src || img.getAttribute('src') || ''
        }))
    }, [content])

    // 截取文本
    const truncatedText = useMemo(() => {
        if (plainText.length <= maxLength) return plainText
        return plainText.slice(0, maxLength) + '...'
    }, [plainText, maxLength])

    const imageCount = images.length
    const hasImages = imageCount > 0

    // 渲染内容
    const renderContent = () => {
        return (
            <div className="richtext-preview-text">
                <span>{truncatedText}</span>
                {hasImages && (
                    <span className="richtext-preview-thumbnails">
                        {images.map((img) => (
                            <Image
                                key={img.id}
                                src={img.src}
                                alt=""
                                width={28}
                                height={28}
                                style={{ objectFit: 'cover', borderRadius: 2, marginLeft: 4, verticalAlign: 'middle' }}
                                preview={{ mask: '查看' }}
                            />
                        ))}
                    </span>
                )}
            </div>
        )
    }

    // 渲染完整内容（用于 Popover）
    const renderFullContent = () => {
        return (
            <div className="richtext-preview-full">
                <div className="richtext-preview-html" dangerouslySetInnerHTML={{ __html: content }} />
                {hasImages && (
                    <div className="richtext-preview-images">
                        <div className="richtext-preview-images-title">包含 {imageCount} 张图片：</div>
                        <div className="richtext-preview-images-grid">
                            {images.map((img) => (
                                <Image
                                    key={img.id}
                                    src={img.src}
                                    alt=""
                                    width={80}
                                    height={80}
                                    style={{ objectFit: 'cover', borderRadius: 4 }}
                                    preview={{
                                        mask: '点击查看'
                                    }}
                                />
                            ))}
                        </div>
                    </div>
                )}
            </div>
        )
    }

    // 如果启用了 Popover 模式
    if (showPopover && content && plainText.length > maxLength) {
        return (
            <Popover
                content={renderFullContent()}
                title="内容预览"
                trigger="hover"
                open={popoverOpen}
                onOpenChange={setPopoverOpen}
                placement="right"
                overlayStyle={{ maxWidth: 600 }}
            >
                <span style={{ cursor: 'pointer', color: '#1677ff' }} onClick={() => setPopoverOpen(!popoverOpen)}>
                    {renderContent()}
                </span>
            </Popover>
        )
    }

    return renderContent()
}

export default RichTextPreview
