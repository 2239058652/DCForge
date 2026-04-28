# react-19-template

#### 介绍

react 19 + vite + antd + tailwind-css + router v7 的系统模板，需配置菜单，layout 等等再使用

#### 软件架构

软件架构说明

#### 安装教程

1.  pnpm i # 安装依赖

#### 使用说明

1.  pnpm dev # 开发
2.  pnpm build # 打包
3.  pnpm prew # 预览打包后的文件
4.  pnpm knip # 检查依赖、文件使用情况
5.  pnpm check-deps # 检测依赖可升级情况
 

# 🛠️ 前端调试黑科技清单

> 整理自 Chrome DevTools 控制台实用技巧 · 持续更新

---

## 🔧 一、DOM 编辑（即改即生效）

| 方法 | 说明 |
|------|------|
| `document.designMode = 'on'` | **整页可编辑**（像 Word），设 `'off'` 关闭 |
| `document.body.contentEditable = true` | 仅 `<body>` 可编辑（轻量版） |
| `$0.contentEditable = true` | 仅当前选中元素可编辑 |

> 💡 配合使用：  
> ```js
> document.designMode = 'on';
> document.execCommand('enableObjectResizing', false, 'true'); // 允许拖拽缩放图片/表格
> ```

---

## 🎨 二、UI 调试（视觉问题秒定位）

| 方法 | 说明 |
|------|------|
| `$$('*').forEach(el => el.style.outline = '1px solid #'+Math.floor(Math.random()*16777215).toString(16))` | **所有元素加随机色边框**（布局重叠一目了然）✅ |
| `document.body.style.filter = 'blur(2px)'` | 全局模糊（排查 z-index 层级） |
| `$0.scrollIntoView({ behavior: 'smooth' })` | 平滑滚动到选中元素 |
| `$0.animate({ opacity: 0, scale: 2 }, 800)` | 给元素加临时动画（调试入场/出场） |

---

## ⚡ 三、性能 & 渲染

| 方法 | 说明 |
|------|------|
| `performance.memory` | 查看 JS 堆内存（Chrome only，需开 flag） |
| `getEventListeners($0)` | 查看某元素绑定的所有事件（Chrome only） |
| `monitor(fn)` / `unmonitor(fn)` | 监听函数调用（含参数） |
| `keys(obj)` / `values(obj)` | 快速列出对象 keys/values |
| `$0.getClientRects()` | 获取元素**所有行框**位置（调试 inline 折行） |

---

## 🌐 四、网络 & 缓存

| 方法 | 说明 |
|------|------|
| `navigator.serviceWorker.getRegistrations().then(r => r.forEach(swr => swr.unregister()))` | **清除所有 Service Worker**（解决缓存不更新）✅ |
| `copy(document.documentElement.outerHTML)` | 复制当前页完整 HTML（含 JS 修改后结果） |
| `copy(await (await fetch('/api')).json())` | 一键 fetch + copy JSON |

---

## 🕵️ 五、控制台快捷变量 & 函数

| 名称 | 说明 |
|------|------|
| `$0`, `$1`, `$2`, `$3`, `$4` | Elements 面板最近选中的 5 个元素（`$0` 是最新） |
| `$$("selector")` | 等价 `document.querySelectorAll()`，**返回真实数组**（可直接 `.map()`） |
| `$x("//xpath")` | 用 XPath 查询元素（适合复杂结构） |
| `$_` | 上一次表达式返回值（类似 shell 的 `$_`） |
| `copy(obj)` | 复制任意 JS 对象/字符串到剪贴板 |
| `table(data)` | 将对象数组转为表格展示 |
| `queryObjects(Constructor)` | 查找内存中某类的所有实例（需 heap profiling） |

---

## 🛠️ 六、自动化调试脚本（可封装为 Bookmarklet）

### 解除网页限制
```js
document.oncontextmenu = null;
document.onselectstart = null;
document.ondragstart = null;
document.onkeydown = null;
document.body.style.userSelect = 'auto';
