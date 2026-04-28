import { useEffect, useRef } from 'react'
import * as pdfjsLib from 'pdfjs-dist'

pdfjsLib.GlobalWorkerOptions.workerSrc = `${import.meta.env.BASE_URL}pdf.worker.min.js`

interface Props {
    url: string
}

export default function PDFViewer({ url }: Props) {
    const containerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const container = containerRef.current!
        container.innerHTML = '' // 清空之前的内容

        const loadingTask = pdfjsLib.getDocument(url)

        loadingTask.promise.then((pdf) => {
            for (let pageNum = 1; pageNum <= pdf.numPages; pageNum++) {
                pdf.getPage(pageNum).then((page) => {
                    const viewport = page.getViewport({ scale: 1.3 })
                    const canvas = document.createElement('canvas')
                    const context = canvas.getContext('2d')!
                    canvas.width = viewport.width
                    canvas.height = viewport.height
                    page.render({ canvasContext: context, viewport, canvas })
                    container.appendChild(canvas)
                })
            }
        })
    }, [url])

    return <div className="p-4 w-full h-full overflow-auto" ref={containerRef}></div>
}
