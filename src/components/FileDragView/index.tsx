import React, { useRef, useState } from 'react'
import { Modal, Button } from 'antd'
import PDFViewer from './PDFViewer'
import DocxViewer from './DocxViewer'
import TxtViewer from './TxtViewer'
import { useAppMessage } from '@/contexts/MessageContext'

export default function FileDragView(props: IFileDragViewProps) {
    const { file, fileType, pdfPage, onFileChange, onPdfPageChange, onPdfLoad, onCustomRequest } = props
    const messageApi = useAppMessage()

    const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

    const fileInputRef = useRef<HTMLInputElement | null>(null)

    const [isModalOpen, setIsModalOpen] = useState(false)
    const [pdfTotalPages, setPdfTotalPages] = useState(1)
    const [dragging, setDragging] = useState(false)

    const validateFile = (f: File) => {
        if (f.size > MAX_FILE_SIZE) {
            messageApi.error('文件大小不能超过 10MB')
            return false
        }

        const ext = f.name.split('.').pop()?.toLowerCase()
        if (!ext) {
            messageApi.error('无法识别文件格式')
            return false
        }

        if (!['pdf', 'docx', 'txt'].includes(ext)) {
            messageApi.error('不支持的格式')
            return false
        }

        return ext as 'pdf' | 'docx' | 'txt'
    }

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault()
        setDragging(false)

        const f = e.dataTransfer.files[0]
        if (!f) return

        const ext = validateFile(f)
        if (!ext) return

        onFileChange(f, ext)
        if (onCustomRequest) onCustomRequest(f, ext)
    }

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const f = e.target.files?.[0]
        if (!f) return

        const ext = validateFile(f)
        if (!ext) {
            e.target.value = ''
            return
        }

        onFileChange(f, ext)
        if (onCustomRequest) onCustomRequest(f, ext)

        // 清空 input，否则无法连续选择同一个文件
        e.target.value = ''
    }

    const Viewer = () => {
        if (!file) return <div className="text-center pt-20 text-gray-400">暂无文件</div>

        switch (fileType) {
            case 'pdf':
                return (
                    <PDFViewer
                        file={file}
                        page={pdfPage}
                        onPageChange={onPdfPageChange}
                        onLoaded={(total) => {
                            setPdfTotalPages(total)
                            onPdfLoad(total)
                        }}
                    />
                )
            case 'docx':
                return <DocxViewer file={file} />
            case 'txt':
                return <TxtViewer file={file} />
            default:
                return <div className="p-6 text-center">不支持的文件格式</div>
        }
    }

    const handleDragEnter = (e: React.DragEvent) => {
        e.preventDefault()
        setDragging(true)
    }

    const handleDragOver = (e: React.DragEvent) => {
        e.preventDefault()
        setDragging(true)
    }

    const handleDragLeave = (e: React.DragEvent) => {
        e.preventDefault()
        setDragging(false)
    }

    const handleClickUpload = () => {
        if (dragging) return // 处理拖拽进入状态误触
        fileInputRef.current?.click()
    }

    const dropAreaClass = dragging
        ? 'border-2 border-dashed p-6 text-center rounded-md cursor-pointer text-gray-600 bg-gray-100'
        : 'border-2 border-dashed p-6 text-center rounded-md cursor-pointer text-gray-600 hover:bg-gray-100'

    return (
        <div className="w-full h-full">
            {/* 拖拽上传 */}
            <div
                className={dropAreaClass}
                onClick={handleClickUpload}
                onDragEnter={handleDragEnter}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
            >
                拖放文件到此区域 或 点击上传：支持 PDF / Word / TXT
            </div>

            <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.docx,.txt"
                className="hidden"
                onChange={handleFileSelect}
            />

            {/* 内嵌预览 */}
            <div className="mt-4 w-full border rounded-lg h-[50vh] overflow-hidden relative bg-white">
                <Viewer />

                {/* 放大预览按钮 */}
                {file && (
                    <Button
                        ghost
                        type="primary"
                        className="absolute bottom-1 right-4 z-10"
                        onClick={() => setIsModalOpen(true)}
                    >
                        弹窗预览
                    </Button>
                )}
            </div>

            {/* PDF 分页控制条 */}
            {fileType === 'pdf' && (
                <div className="flex justify-center items-center gap-3 mt-3">
                    <Button disabled={pdfPage <= 1} onClick={() => onPdfPageChange(pdfPage - 1)}>
                        上一页
                    </Button>
                    <div>
                        {pdfPage} / {pdfTotalPages}
                    </div>
                    <Button disabled={pdfPage >= pdfTotalPages} onClick={() => onPdfPageChange(pdfPage + 1)}>
                        下一页
                    </Button>
                </div>
            )}

            {/* 全屏 Modal 预览 */}
            <Modal
                title="文件预览"
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                footer={null}
                destroyOnHidden
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
