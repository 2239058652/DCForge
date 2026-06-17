import { Icon } from '@iconify/react'
import { App, Button, Form, Input, Modal, Select, Typography, Upload, message } from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { aiImageApi, type SaveImageHistoryPayload, type TaskResult } from '@/api/ai-image'
import { useWebSocket } from '@/hooks/useWebSocket'
import RichTextPreview from '@/components/RichTextPreview/index'
import './index.css'

const { Text } = Typography

const SIZES = [
    { label: '1024 x 768', value: '1024x768' },
    { label: '768 x 1024', value: '768x1024' },
    { label: '512 x 512', value: '512x512' },
    { label: '1024 x 1024', value: '1024x1024' }
]

const PROMPT_TIPS = '推荐结构：[主体] + [场景] + [风格] + [光照] + [构图] + [质量]'

interface TaskSession {
    taskId: number | null
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
    imageUrl: string | null
    revisedPrompt: string | null
    errorMessage: string | null
    prompt: string
    size: string
}

interface TextToImageForm {
    prompt: string
    size: string
}

interface ImageToImageForm {
    prompt: string
    size: string
    urlInput: string
}

const AiImage = () => {
    const { message: msg } = App.useApp()
    const navigate = useNavigate()
    const [tab, setTab] = useState<'text' | 'image'>('text')
    const [saveModalOpen, setSaveModalOpen] = useState(false)
    const [saving, setSaving] = useState(false)

    // Text-to-image
    const [textForm] = Form.useForm<TextToImageForm>()
    const [textSession, setTextSession] = useState<TaskSession | null>(null)

    // Image-to-image
    const [imageForm] = Form.useForm<ImageToImageForm>()
    const [imageSession, setImageSession] = useState<TaskSession | null>(null)
    const [uploadList, setUploadList] = useState<UploadFile[]>([])
    const [uploadFiles, setUploadFiles] = useState<File[]>([])
    const imageResultRef = useRef<HTMLDivElement>(null)
    const wsTaskRef = useRef<Map<number, TaskSession>>(new Map())
    const msgRef = useRef(message)
    msgRef.current = msg

    // WebSocket 回调 - 只处理 PROCESSING/COMPLETED/FAILED
    const unsubscribeRef = useRef<((taskId: number) => void) | undefined>(undefined)

    const handleTaskUpdate = useCallback((notification: { taskId: number; status: string; imageUrl?: string; revisedPrompt?: string; errorMessage?: string }) => {
        const { taskId: nTaskId, status, imageUrl, revisedPrompt, errorMessage } = notification
        const session = wsTaskRef.current.get(nTaskId)
        if (!session) return

        const updated = { ...session, status: status as TaskSession['status'], imageUrl: imageUrl || null, revisedPrompt: revisedPrompt || null, errorMessage: errorMessage || null }
        wsTaskRef.current.set(nTaskId, updated)

        const showMsg = msgRef.current
        if (tab === 'text') {
            setTextSession(updated)
            if (status === 'COMPLETED') {
                showMsg.success('图片生成成功')
                setTimeout(() => unsubscribeRef.current?.(nTaskId), 5000)
            } else if (status === 'FAILED') {
                showMsg.error(updated.errorMessage || '生成失败')
            }
        } else {
            setImageSession(updated)
            if (status === 'COMPLETED') {
                showMsg.success('图片生成成功')
                setTimeout(() => {
                    unsubscribeRef.current?.(nTaskId)
                    imageResultRef.current?.scrollIntoView({ behavior: 'smooth' })
                }, 500)
            } else if (status === 'FAILED') {
                showMsg.error(updated.errorMessage || '生成失败')
            }
        }
    }, [tab])

    const { subscribe, unsubscribe } = useWebSocket({ onTaskUpdate: handleTaskUpdate })
    unsubscribeRef.current = unsubscribe

    const setCurrentTaskId = useCallback((taskId: number) => {
        subscribe(taskId)
    }, [subscribe])

    const handleTextGenerate = useCallback(async () => {
        const values = await textForm.validateFields()
        setTextSession({ taskId: null, status: 'PENDING', imageUrl: null, revisedPrompt: null, errorMessage: null, prompt: values.prompt, size: values.size })
        try {
            const result = await aiImageApi.submitTask({ type: 'text2img', prompt: values.prompt, size: values.size })
            if (result.code !== 200) {
                msg.error(result.message || '提交任务失败')
                setTextSession(null)
                return
            }
            const taskResult = result.data as TaskResult
            const session: TaskSession = {
                taskId: taskResult.id,
                status: 'PENDING',
                imageUrl: null,
                revisedPrompt: null,
                errorMessage: null,
                prompt: values.prompt,
                size: values.size
            }
            wsTaskRef.current.set(taskResult.id, session)
            setTextSession(session)
            setCurrentTaskId(taskResult.id)
        } catch {
            msg.error('提交任务失败，请检查后端服务')
            setTextSession(null)
        }
    }, [textForm, msg, setCurrentTaskId])

    const handleImageGenerate = useCallback(async () => {
        const values = await imageForm.validateFields()
        const urlInput = values.urlInput || ''
        const urlImages = urlInput
            .split(/[,，]/)
            .map(u => u.trim())
            .filter(u => u.length > 0)
        const uploadedUrls: string[] = []
        if (uploadFiles.length > 0) {
            try {
                const results = await Promise.all(
                    uploadFiles.map(file => aiImageApi.uploadImage(file))
                )
                results.forEach(r => {
                    if (r && r.code === 200 && r.data) {
                        uploadedUrls.push(r.data)
                    }
                })
            } catch {
                msg.error('上传参考图片失败')
                return
            }
        }
        const allImages = [...uploadedUrls, ...urlImages]
        if (allImages.length === 0) {
            msg.error('请上传或输入参考图片')
            return
        }
        setImageSession({ taskId: null, status: 'PENDING', imageUrl: null, revisedPrompt: null, errorMessage: null, prompt: values.prompt, size: values.size })
        try {
            const result = await aiImageApi.submitTask({ type: 'img2img', prompt: values.prompt, size: values.size, images: allImages })
            if (result.code !== 200) {
                msg.error(result.message || '提交任务失败')
                setImageSession(null)
                return
            }
            const taskResult = result.data as TaskResult
            const session: TaskSession = {
                taskId: taskResult.id,
                status: 'PENDING',
                imageUrl: null,
                revisedPrompt: null,
                errorMessage: null,
                prompt: values.prompt,
                size: values.size
            }
            wsTaskRef.current.set(taskResult.id, session)
            setImageSession(session)
            setCurrentTaskId(taskResult.id)
        } catch {
            msg.error('提交任务失败，请检查后端服务')
            setImageSession(null)
        }
    }, [imageForm, uploadFiles, msg, setCurrentTaskId])

    const handleUploadChange = useCallback(({ fileList }: { fileList: UploadFile[] }) => {
        setUploadList(fileList)
        const files: File[] = []
        fileList.forEach(f => {
            if (f.originFileObj) {
                files.push(f.originFileObj as File)
            }
        })
        setUploadFiles(files)
    }, [])

    const handleSave = async () => {
        setSaving(true)
        try {
            const isText = tab === 'text'
            const currentSession = isText ? textSession : imageSession
            if (!currentSession || !currentSession.taskId) {
                msg.error('没有可保存的内容')
                setSaveModalOpen(false)
                return
            }

            const payload: SaveImageHistoryPayload = {
                taskId: currentSession.taskId
            }

            if (!isText && uploadList.length > 0) {
                payload.sourceImageUrls = uploadList.map(f => f.thumbUrl || f.url || '').filter(Boolean)
            }

            const result = await aiImageApi.saveImageHistory(payload)
            if (result.code !== 200) {
                msg.error(result.message || '保存失败')
                return
            }
            msg.success('已保存到相册')
            setSaveModalOpen(false)
            navigate('/ai/image/history')
        } catch {
            msg.error('保存失败')
        } finally {
            setSaving(false)
        }
    }

    useEffect(() => {
        if (tab === 'text') {
            textForm.resetFields()
            imageForm.resetFields()
            setUploadList([])
            setUploadFiles([])
        } else {
            imageForm.resetFields()
            textForm.resetFields()
            setUploadList([])
            setUploadFiles([])
        }
    }, [tab, textForm, imageForm])

    // 任务状态标签
    const getStatusLabel = (status: string) => {
        switch (status) {
            case 'PENDING': return '任务已提交'
            case 'PROCESSING': return '正在生成图片'
            case 'COMPLETED': return '生成完成'
            case 'FAILED': return '生成失败'
            default: return status
        }
    }

    const renderSession = (session: TaskSession | null, isText: boolean) => {
        if (!session) return null
        const hasError = session.status === 'FAILED'

        if (hasError) {
            return (
                <div className="ai-image-result">
                    <h3 className="ai-image-result-title">生成结果</h3>
                    <div className="ai-image-error">
                        <Icon icon="solar:danger-triangle-bold" width={24} color="#ef4444" />
                        <div style={{ color: '#ef4444', marginTop: 8 }}>{getStatusLabel(session.status)}</div>
                        {session.errorMessage && <div style={{ color: '#64748b', marginTop: 4 }}>{session.errorMessage}</div>}
                    </div>
                </div>
            )
        }

        if (session.status === 'COMPLETED') {
            return (
                <div className="ai-image-result" ref={isText ? undefined : imageResultRef}>
                    <h3 className="ai-image-result-title">生成结果</h3>
                    <div className="ai-image-result-meta">
                        <div className="ai-image-meta-item">
                            <strong>prompt:</strong>
                            <Text className="ai-image-meta-text" title={session.prompt}>
                                {session.prompt}
                            </Text>
                        </div>
                        {session.revisedPrompt && (
                            <div className="ai-image-meta-item">
                                <strong>revised:</strong>
                                <RichTextPreview content={session.revisedPrompt} />
                            </div>
                        )}
                    </div>
                    <div className="ai-image-result-image">
                        <img src={session.imageUrl || ''} alt="生成结果" />
                    </div>
                    <div className="ai-image-result-actions">
                        <Button type="primary" icon={<Icon icon="solar:disk-bold" width={18} />} onClick={() => setSaveModalOpen(true)}>
                            💾 保存到相册
                        </Button>
                        <a href={session.imageUrl || ''} target="_blank" rel="noreferrer">
                            <Icon icon="solar:link-bold" width={16} />
                            打开原图
                        </a>
                    </div>
                </div>
            )
        }

        // Loading 状态（PENDING 或 PROCESSING）
        return (
            <div className="ai-image-result">
                <h3 className="ai-image-result-title">生成结果</h3>
                <div className="ai-image-loading">
                    <Icon icon="solar:spinner-bold" width={24} color="#8b5cf6" style={{ animation: 'spin 1s linear infinite' }} />
                    <div style={{ marginTop: 8, color: '#64748b' }}>{getStatusLabel(session.status)}</div>
                    <div style={{ marginTop: 4, color: '#94a3b8', fontSize: 13 }}>正在调用 Agnes AI 图像生成服务，请稍候...</div>
                </div>
            </div>
        )
    }

    return (
        <div className="ai-image-page">
            <div className="ai-image-container">
                <section className="ai-image-card">
                    <div className="ai-image-hero">
                        <div>
                            <div className="ai-image-kicker">
                                <Icon icon="solar:palette-bold-duotone" width={20} color="#8b5cf6" />
                                AI Image Generation
                            </div>
                            <h1 className="ai-image-title">图像管理</h1>
                            <p className="ai-image-desc">使用 Agnes AI 图像生成服务（agnes-image-2.1-flash 模型）创建和变换图像。</p>
                        </div>
                        <Button
                            icon={<Icon icon="solar:trash-bin-trash-bold" width={18} />}
                            onClick={() => navigate('/ai/image/history')}
                        >
                            查看历史
                        </Button>
                    </div>

                    <div className="ai-image-tabs">
                        <div
                            className={`ai-image-tab ${tab === 'text' ? 'active' : ''}`}
                            onClick={() => setTab('text')}
                        >
                            <Icon icon="solar:pen-2-bold-duotone" width={18} />
                            文生图
                        </div>
                        <div
                            className={`ai-image-tab ${tab === 'image' ? 'active' : ''}`}
                            onClick={() => setTab('image')}
                        >
                            <Icon icon="solar:photo-bold-duotone" width={18} />
                            图生图
                        </div>
                    </div>

                    {tab === 'text' && (
                        <div className="ai-image-content">
                            <div className="ai-image-form-section">
                                <Form
                                    form={textForm}
                                    layout="vertical"
                                    initialValues={{ size: '1024x768' }}
                                >
                                    <Form.Item
                                        name="prompt"
                                        label="图像描述"
                                        rules={[{ required: true, message: '请输入图像描述' }]}
                                    >
                                        <Input.TextArea
                                            rows={4}
                                            placeholder="例如：一只橘猫坐在窗台上看夕阳，水彩风格，暖色调，柔和光线"
                                            showCount
                                            maxLength={1000}
                                        />
                                    </Form.Item>
                                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
                                        <Form.Item name="size" label="图片尺寸" style={{ flex: 1, marginBottom: 0 }}>
                                            <Select options={SIZES} />
                                        </Form.Item>
                                        <Button
                                            type="primary"
                                            icon={<Icon icon="solar:wand-bold" />}
                                            loading={textSession && textSession.status !== 'COMPLETED' && textSession.status !== 'FAILED'}
                                            onClick={handleTextGenerate}
                                            style={{ height: 40 }}
                                        >
                                            {textSession?.status === 'PROCESSING' ? '生成中...' : textSession?.status === 'PENDING' ? '排队中...' : '生成图片'}
                                        </Button>
                                    </div>
                                </Form>
                                <div className="ai-image-prompt-tip">
                                    <Icon icon="solar:lightbulger-solid-duotone" width={16} color="#f59e0b" />
                                    <span>{PROMPT_TIPS}</span>
                                    <div style={{ marginTop: 6, color: '#64748b', fontSize: 13, lineHeight: 1.6 }}>
                                        <div>一只橘猫坐在窗台上看夕阳，水彩风格，暖色调，柔和光线</div>
                                        <div>未来城市市场，飞行汽车，霓虹灯，赛博朋克风格，超细节</div>
                                        <div>白色背景上的玻璃立方体产品照片，柔和阴影，高清晰度</div>
                                    </div>
                                </div>
                            </div>

                            {renderSession(textSession, true)}
                        </div>
                    )}

                    {tab === 'image' && (
                        <div className="ai-image-content">
                            <div className="ai-image-form-section">
                                <Form
                                    form={imageForm}
                                    layout="vertical"
                                    initialValues={{ size: '1024x768' }}
                                >
                                    <Form.Item label="参考图片">
                                        <div className="ai-image-image-input">
                                            <Upload
                                                listType="picture-card"
                                                fileList={uploadList}
                                                onChange={handleUploadChange}
                                                accept="image/*"
                                                maxCount={5}
                                                customRequest={({ onSuccess }) => {
                                                    setTimeout(() => onSuccess('ok'), 0)
                                                }}
                                            >
                                                {uploadList.length >= 5 ? null : (
                                                    <div>
                                                        <Icon icon="solar:upload-bold-duotone" width={20} color="#94a3b8" />
                                                        <div style={{ marginTop: 4, color: '#94a3b8', fontSize: 12 }}>上传图片</div>
                                                    </div>
                                                )}
                                            </Upload>
                                            <Form.Item
                                                name="urlInput"
                                                style={{ marginTop: 8, marginBottom: 0 }}
                                            >
                                                <Input placeholder="输入图片公开 URL（多个用逗号分隔）" allowClear />
                                            </Form.Item>
                                        </div>
                                    </Form.Item>
                                    <Form.Item
                                        name="prompt"
                                        label="变换描述"
                                        rules={[{ required: true, message: '请输入变换描述' }]}
                                    >
                                        <Input.TextArea
                                            rows={3}
                                            placeholder="例如：将场景转换为赛博朋克雨夜风格，保留原始构图"
                                            showCount
                                            maxLength={1000}
                                        />
                                    </Form.Item>
                                    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
                                        <Form.Item name="size" label="输出尺寸" style={{ flex: 1, marginBottom: 0 }}>
                                            <Select options={SIZES} />
                                        </Form.Item>
                                        <Button
                                            type="primary"
                                            icon={<Icon icon="solar:swap-bold-duotone" />}
                                            loading={imageSession && imageSession.status !== 'COMPLETED' && imageSession.status !== 'FAILED'}
                                            onClick={handleImageGenerate}
                                            style={{ height: 40 }}
                                        >
                                            {imageSession?.status === 'PROCESSING' ? '生成中...' : imageSession?.status === 'PENDING' ? '排队中...' : '生成图片'}
                                        </Button>
                                    </div>
                                </Form>
                                <div className="ai-image-prompt-tip">
                                    <Icon icon="solar:lightbulger-solid-duotone" width={16} color="#f59e0b" />
                                    <span>图生图时明确说明"要改变什么"和"要保留什么"</span>
                                    <div style={{ marginTop: 6, color: '#64748b', fontSize: 13, lineHeight: 1.6 }}>
                                        <div>将画面转为冬季雪景，添加雪花和温暖窗户灯光，保留原始建筑结构和镜头角度</div>
                                    </div>
                                </div>
                            </div>

                            {imageSession && (
                                <div className="ai-image-result" ref={imageResultRef}>
                                    <h3 className="ai-image-result-title">生成结果</h3>

                                    {imageSession.status === 'COMPLETED' && (
                                        <>
                                            <div className="ai-image-result-meta">
                                                <div className="ai-image-meta-item">
                                                    <strong>prompt:</strong>
                                                    <Text className="ai-image-meta-text" title={imageSession.prompt}>
                                                        {imageSession.prompt}
                                                    </Text>
                                                </div>
                                                {imageSession.revisedPrompt && (
                                                    <div className="ai-image-meta-item">
                                                        <strong>revised:</strong>
                                                        <RichTextPreview content={imageSession.revisedPrompt} />
                                                    </div>
                                                )}
                                            </div>
                                            <div className="ai-image-compare">
                                                {uploadList.length > 0 && (
                                                    <div className="ai-image-compare-item">
                                                        <div className="ai-image-compare-label">原图</div>
                                                        <img src={uploadList[0]?.thumbUrl || ''} alt="原图" />
                                                    </div>
                                                )}
                                                {uploadList.length > 0 && (
                                                    <>
                                                        <div className="ai-image-compare-arrow">
                                                            <Icon icon="solar:arrow-right-bold" width={24} color="#8b5cf6" />
                                                        </div>
                                                        <div className="ai-image-compare-item">
                                                            <div className="ai-image-compare-label">生成图</div>
                                                            <img src={imageSession.imageUrl || ''} alt="生成结果" />
                                                        </div>
                                                    </>
                                                )}
                                                {uploadList.length === 0 && (
                                                    <div className="ai-image-compare-item" style={{ width: '100%' }}>
                                                        <div className="ai-image-compare-label">生成图</div>
                                                        <img src={imageSession.imageUrl || ''} alt="生成结果" />
                                                    </div>
                                                )}
                                            </div>
                                            <div className="ai-image-result-actions">
                                                <Button type="primary" icon={<Icon icon="solar:disk-bold" width={18} />} onClick={() => setSaveModalOpen(true)}>
                                                    💾 保存到相册
                                                </Button>
                                                <a href={imageSession.imageUrl || ''} target="_blank" rel="noreferrer">
                                                    <Icon icon="solar:link-bold" width={16} />
                                                    打开原图
                                                </a>
                                            </div>
                                        </>
                                    )}

                                    {imageSession.status !== 'COMPLETED' && imageSession.status !== 'FAILED' && (
                                        <div className="ai-image-loading">
                                            <Icon icon="solar:spinner-bold" width={24} color="#8b5cf6" style={{ animation: 'spin 1s linear infinite' }} />
                                            <div style={{ marginTop: 8, color: '#64748b' }}>{getStatusLabel(imageSession.status)}</div>
                                            <div style={{ marginTop: 4, color: '#94a3b8', fontSize: 13 }}>正在调用 Agnes AI 图像生成服务，请稍候...</div>
                                        </div>
                                    )}

                                    {imageSession.status === 'FAILED' && (
                                        <div className="ai-image-error">
                                            <Icon icon="solar:danger-triangle-bold" width={24} color="#ef4444" />
                                            <div style={{ color: '#ef4444', marginTop: 8 }}>{getStatusLabel(imageSession.status)}</div>
                                            {imageSession.errorMessage && <div style={{ color: '#64748b', marginTop: 4 }}>{imageSession.errorMessage}</div>}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}
                </section>
            </div>

            <Modal
                title="保存到相册"
                open={saveModalOpen}
                okText="确认保存"
                cancelText="取消"
                confirmLoading={saving}
                onOk={handleSave}
                onCancel={() => setSaveModalOpen(false)}
            >
                <p style={{ color: '#64748b' }}>将下载当前图片并上传到 MinIO 永久存储，创建历史记录。</p>
            </Modal>
        </div>
    )
}

export default AiImage
