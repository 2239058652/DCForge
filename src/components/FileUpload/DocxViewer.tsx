import { useEffect, useRef } from 'react'
import { renderAsync } from 'docx-preview'

interface Props {
    url: string
}

export default function DocxViewer({ url }: Props) {
    const containerRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        fetch(url)
            .then((res) => res.arrayBuffer())
            .then((buffer) => {
                renderAsync(buffer, containerRef.current!, containerRef.current!, {
                    inWrapper: true,
                    ignoreWidth: false,
                    ignoreHeight: false,
                    breakPages: false
                })
            })
    }, [url])

    return <div ref={containerRef} className="p-4 w-full h-full overflow-auto"></div>
}
