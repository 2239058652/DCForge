/// <reference types="vite/client" />

declare module '*.ts'

// CSS 模块的类型声明
declare module '*.css' {
    const content: any
    export default content
}

// 图片等静态资源的类型声明
declare module '*.png' {
    const content: any
    export default content
}

declare module '*.jpg' {
    const content: any
    export default content
}

declare module '*.jpeg' {
    const content: any
    export default content
}

declare module '*.gif' {
    const content: any
    export default content
}

declare module '*.svg' {
    const content: any
    export default content
}
