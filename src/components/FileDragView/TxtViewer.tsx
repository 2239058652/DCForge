import { useEffect, useState } from 'react'

interface Props {
    file: File
}

export default function TxtViewer({ file }: Props) {
    const [text, setText] = useState('')

    useEffect(() => {
        const reader = new FileReader()
        reader.onload = () => setText(reader.result as string)
        reader.readAsText(file)
    }, [file])

    return <pre className="h-full overflow-auto p-4 text-sm whitespace-pre-wrap">{text}</pre>
}
