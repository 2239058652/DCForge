import React, { useRef, useState, ChangeEvent, DragEvent } from 'react'
import { Icon } from '@iconify/react'
import { MessageInstance } from 'antd/es/message/interface'
import { Button } from 'antd'

interface UploadBoxProps {
    uploadApi: (data: {
        task_type: 'ocr' | 'review' | 'table'
        file: File
    }) => Promise<{ code: number; data: { file_path: string; relative_path: string } }>
    onSuccess?: (data: { code: number; data: { file_path: string; relative_path: string } }) => void
    messageApi: MessageInstance
}

const UploadBox: React.FC<UploadBoxProps> = ({ uploadApi, onSuccess, messageApi }) => {
    const fileInputRef = useRef<HTMLInputElement | null>(null)
    const [dragActive, setDragActive] = useState(false)
    const [uploading, setUploading] = useState(false)

    // 文件选择
    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (file) {
            await uploadFile(file)
        }
    }

    // 拖入区域
    const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setDragActive(true)
    }

    const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setDragActive(false)
    }

    // 放下文件
    const handleDrop = async (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setDragActive(false)

        const file = e.dataTransfer.files?.[0]
        if (file) {
            await uploadFile(file)
        }
    }

    // 上传文件
    const uploadFile = async (file: File) => {
        // 100MB 限制
        const MAX_SIZE = 10 * 1024 * 1024
        if (file.size > MAX_SIZE) {
            messageApi.error('文件大小不能超过 100MB')
            return
        }

        setUploading(true)

        try {
            const formData = new FormData()
            formData.append('file', file)

            const resp = await uploadApi({ task_type: 'ocr', file: file })

            if (!resp) messageApi.error('Upload failed')

            if (onSuccess) onSuccess(resp)
        } catch (err) {
            console.error(err)
            messageApi.error('上传失败')
        } finally {
            setUploading(false)
        }
    }

    return (
        <div
            className={`bg-gray-50 rounded-md border border-dashed p-8 h-full text-center transition 
                ${dragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300'} flex flex-col justify-center items-center`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
        >
            <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Icon icon="mdi:cloud-upload" className="text-blue-500" width="32" height="32" />
            </div>

            <h3 className="font-medium text-lg text-gray-700 mb-2">上传文档</h3>
            <p className="text-gray-500 mb-4">支持PDF/图片格式</p>

            <input
                type="file"
                className="hidden"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept=".pdf,image/*"
            />

            <Button
                type="primary"
                onClick={() => fileInputRef.current?.click()}
                className="py-2 px-6 rounded-lg font-medium transition w-1/2"
                disabled={uploading}
            >
                {uploading ? '上传中...' : '选择文件'}
            </Button>

            <p className="text-gray-400 text-sm mt-4">或拖放文件到此区域</p>
            <p className="text-gray-400 text-sm mt-4">文件大小：小于10MB</p>
        </div>
    )
}

export default UploadBox
