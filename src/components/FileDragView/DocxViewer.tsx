import { useEffect, useRef } from 'react'
import { renderAsync } from 'docx-preview'

interface Props {
    file: File
}

export default function DocxViewer({ file }: Props) {
    const containerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        ;(async () => {
            const arrayBuffer = await file.arrayBuffer()
            if (containerRef.current) {
                containerRef.current.innerHTML = ''
                await renderAsync(arrayBuffer, containerRef.current)
            }
        })()
    }, [file])

    return <div ref={containerRef} className="h-full overflow-auto p-6" />
}
