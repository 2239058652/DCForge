import { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { Editor, Toolbar } from '@wangeditor/editor-for-react'
import '@wangeditor/editor/dist/css/style.css'
import { useEffect, forwardRef, useImperativeHandle, useRef, useState } from 'react'

export interface RichTextEditorProps {
    value?: string
    onChange?: (value: string) => void
    placeholder?: string
    height?: number
    readonly?: boolean
}

export interface RichTextEditorRef {
    isEmpty: () => boolean
    getHtml: () => string
    setHtml: (html: string) => void
    clear: () => void
}

const RichTextEditor = forwardRef<RichTextEditorRef, RichTextEditorProps>(
    ({ value = '', onChange, placeholder = '请输入内容...', height = 400, readonly = false }, ref) => {
        const [editor, setEditor] = useState<IDomEditor | null>(null)
        const [html, setHtml] = useState(value)
        const editorRef = useRef<IDomEditor | null>(null)

        // 暴露方法给父组件
        useImperativeHandle(ref, () => ({
            isEmpty: () => editor?.isEmpty() ?? true,
            getHtml: () => editor?.getHtml() ?? '',
            setHtml: (newHtml: string) => editor?.setHtml(newHtml),
            clear: () => editor?.clear()
        }))

        // 初始化时设置 HTML
        useEffect(() => {
            setHtml(value)
        }, [value])

        // 及时销毁 editor，重要！
        useEffect(() => {
            return () => {
                if (editorRef.current) {
                    editorRef.current.destroy()
                    editorRef.current = null
                }
            }
        }, [])

        // 工具栏配置
        const toolbarConfig: Partial<IToolbarConfig> = {
            excludeKeys: ['group-video']
        }

        // 编辑器配置
        const editorConfig: Partial<IEditorConfig> = {
            placeholder,
            readOnly: readonly,
            MENU_CONF: {
                uploadImage: {
                    // 自定义图片上传，使用 base64 格式
                    async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
                        // 将文件转换为 base64
                        const reader = new FileReader()
                        reader.onload = () => {
                            const base64 = reader.result as string
                            insertFn(base64, file.name, '')
                        }
                        reader.readAsDataURL(file)
                    }
                }
            }
        }

        // 编辑器内容变化回调
        const handleChange = (editorInstance: IDomEditor) => {
            const newHtml = editorInstance.getHtml()
            setHtml(newHtml)
            onChange?.(newHtml)
        }

        // 编辑器创建完成回调
        const handleCreated = (editorInstance: IDomEditor) => {
            editorRef.current = editorInstance
            setEditor(editorInstance)
        }

        return (
            <div style={{ border: '1px solid #d9d9d9', borderRadius: 6, overflow: 'hidden' }}>
                {!readonly && (
                    <Toolbar
                        editor={editor}
                        defaultConfig={toolbarConfig}
                        mode="default"
                        style={{ borderBottom: '1px solid #d9d9d9', backgroundColor: '#fafafa' }}
                    />
                )}
                <Editor
                    defaultConfig={editorConfig}
                    value={html}
                    onCreated={handleCreated}
                    onChange={handleChange}
                    mode="default"
                    style={{ height: `${height}px`, overflowY: 'hidden' }}
                />
            </div>
        )
    }
)

RichTextEditor.displayName = 'RichTextEditor'

export default RichTextEditor
