import { Image } from 'antd'

interface Props {
    url: string
}

export default function ImageViewer({ url }: Props) {
    return <Image src={url} alt="preview" className="max-w-full h-auto overflow-auto" />
}
