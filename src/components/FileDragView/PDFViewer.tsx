import { useEffect, useRef, useState } from 'react'
import * as pdfjsLib from 'pdfjs-dist'
import { Button } from 'antd'
import { GlobalWorkerOptions } from 'pdfjs-dist'

GlobalWorkerOptions.workerSrc = `${import.meta.env.BASE_URL}pdf.worker.min.js`

export default function PDFViewer({
    file,
    page,
    onPageChange,
    onLoaded
}: {
    file: File
    page: number
    onPageChange: (page: number) => void
    onLoaded: (total: number) => void
}) {
    const [pdf, setPdf] = useState<any>(null)
    const [scale, setScale] = useState(1.1)
    const canvasRef = useRef<HTMLCanvasElement>(null)

    useEffect(() => {
        ;(async () => {
            const arrayBuffer = await file.arrayBuffer()
            const loaded = await pdfjsLib.getDocument({ data: arrayBuffer }).promise
            setPdf(loaded)
            onLoaded(loaded.numPages)
        })()
    }, [file])

    // 渲染当前页
    useEffect(() => {
        if (!pdf) return
        renderPage(page)
    }, [pdf, page, scale])

    const renderPage = async (pageNum: number) => {
        const p = await pdf.getPage(pageNum)
        const viewport = p.getViewport({ scale })
        const canvas = canvasRef.current!
        const ctx = canvas.getContext('2d')!
        canvas.height = viewport.height
        canvas.width = viewport.width
        await p.render({ canvasContext: ctx, viewport }).promise
    }

    return (
        <div className="h-full flex flex-col">
            {/* 工具栏 */}
            <div className="flex items-center gap-4 p-3 border-b bg-gray-50">
                <Button onClick={() => onPageChange(page - 1)} disabled={page <= 1}>
                    上一页
                </Button>
                <Button onClick={() => onPageChange(page + 1)} disabled={!pdf || page >= pdf.numPages}>
                    下一页
                </Button>

                <Button onClick={() => setScale((s) => s + 0.1)}>放大</Button>
                <Button onClick={() => setScale((s) => Math.max(0.6, s - 0.1))}>缩小</Button>

                <div className="text-gray-500">
                    {page} / {pdf?.numPages || 0} 页
                </div>
            </div>

            {/* 主体 */}
            <div className="flex-1 overflow-auto flex justify-center p-4 bg-gray-100">
                <canvas ref={canvasRef} className="bg-white shadow-lg" />
            </div>
        </div>
    )
}
