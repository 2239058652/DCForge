import PDFViewer from './PDFViewer'
import DocxViewer from './DocxViewer'
import ImageViewer from './ImageViewer'
import React from 'react'
import { Modal } from 'antd'

const FileUpload: React.FC<{ url: string; open?: boolean; onCancel?: () => void }> = ({
    url,
    open = false,
    onCancel = () => {}
}) => {
    const lower = url.toLowerCase()

    const Viewer = () => {
        if (lower.endsWith('.pdf')) return <PDFViewer url={url} />
        if (lower.endsWith('.docx')) return <DocxViewer url={url} />
        if (/\.(jpg|jpeg|png|gif|webp)$/i.test(lower)) return <ImageViewer url={url} />
        if (lower === '') return <div>文件不存在</div>
        return <div>该文件类型暂不支持预览</div>
    }

    return (
        <div>
            <Viewer />
            <Modal
                title="原始文件"
                open={open}
                onCancel={onCancel}
                destroyOnHidden
                footer={null}
                width="90%"
                style={{ top: '1%' }}
                styles={{
                    body: {
                        padding: 0,
                        maxHeight: '85vh',
                        overflowY: 'auto'
                    }
                }}
            >
                <Viewer />
            </Modal>
        </div>
    )
}

export default FileUpload
