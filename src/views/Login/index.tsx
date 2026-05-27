import { Icon } from '@iconify/react'
import { App, Button, Checkbox, Form, Input, Segmented } from 'antd'
import { useState, useEffect } from 'react' // 引入 useEffect
import { useLocation, useNavigate } from 'react-router-dom'
import { userApi, type UserLoginResult, type UserRegisterPayload } from '@/api/user'
import './index.css'

interface AuthForm {
    username: string
    password: string
    confirmPassword?: string
    nickname?: string
    avatar?: string
    remember?: boolean
    captchaCode?: string // 新增验证码字段
}

type AuthMode = 'login' | 'register'

const Login = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<AuthForm>()
    const [mode, setMode] = useState<AuthMode>('login')
    const [loading, setLoading] = useState(false)

    // 新增验证码状态
    const [captchaUuid, setCaptchaUuid] = useState('')
    const [captchaImage, setCaptchaImage] = useState('')
    const [captchaLoading, setCaptchaLoading] = useState(false)

    const navigate = useNavigate()
    const location = useLocation()
    const from = (location.state as { from?: string } | null)?.from || '/notes'

    // 获取验证码
    const getCaptcha = async () => {
        try {
            setCaptchaLoading(true)
            const res = await userApi.getCaptcha()
            if (res.code === 200 && res.data) {
                setCaptchaUuid(res.data.captchaKey) // 对应后端 captchaKey
                setCaptchaImage(res.data.base64Img) // 对应后端 base64Img
            } else {
                message.error(res.message || '验证码获取失败')
            }
        } catch {
            message.error('验证码请求失败')
        } finally {
            setCaptchaLoading(false)
        }
    }
    // 模式切换或初始化时，如果是登录模式则获取验证码
    useEffect(() => {
        if (mode === 'login') {
            getCaptcha()
        }
    }, [mode])

    const saveLogin = (user: UserLoginResult) => {
        const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
        localStorage.setItem(tokenName, user.token)
        localStorage.setItem('userInfo', JSON.stringify(user))
    }

    // 登录函数扩展，接收验证码
    const login = async (username: string, password: string, captchaCode: string, captchaUuid: string) => {
        const result = await userApi.login({
            username,
            password,
            captchaCode,
            captchaUuid
        })
        if (result.code !== 200 || !result.data?.token) {
            message.error(result.message || '登录失败')
            // 登录失败刷新验证码
            getCaptcha()
            return false
        }

        saveLogin(result.data)
        message.success(result.message || '登录成功')
        navigate(from, { replace: true })
        return true
    }

    const handleSubmit = async (values: AuthForm) => {
        const username = values.username.trim()
        const password = values.password
        setLoading(true)

        try {
            if (mode === 'login') {
                // 校验验证码是否已填写
                if (!values.captchaCode) {
                    message.error('请输入验证码')
                    setLoading(false)
                    return
                }
                await login(username, password, values.captchaCode.trim(), captchaUuid)
                return
            }

            // 注册逻辑保持不变
            if (values.confirmPassword !== password) {
                message.error('两次输入的密码不一致')
                return
            }

            const payload: UserRegisterPayload = {
                username,
                password,
                nickname: values.nickname?.trim(),
                avatar: values.avatar?.trim()
            }
            const registerResult = await userApi.register(payload)
            if (registerResult.code !== 200) {
                message.error(registerResult.message || '注册失败')
                return
            }

            // 优先使用注册接口返回的 token 直接登录（无需验证码）
            const data = registerResult.data as Record<string, unknown> | undefined
            if (data && typeof data.token === 'string' && data.token) {
                saveLogin(data as unknown as UserLoginResult)
                message.success(registerResult.message || '注册并登录成功')
                navigate(from, { replace: true })
                return
            }

            // 注册接口未返回 token，回退：切到登录模式让用户输入验证码后手动登录
            message.success(registerResult.message || '注册成功，请输入验证码登录')
            setMode('login')
            form.resetFields()
            form.setFieldsValue({ username, password })
        } catch {
            message.error(mode === 'login' ? '登录失败，请检查后端服务' : '注册失败，请检查后端服务')
            if (mode === 'login') getCaptcha()
        } finally {
            setLoading(false)
        }
    }

    const handleModeChange = (value: string) => {
        setMode(value as AuthMode)
        form.resetFields()
        // 切换到注册时清空验证码状态（不必须，但保持干净）
        if (value === 'register') {
            setCaptchaUuid('')
            setCaptchaImage('')
        }
    }

    return (
        <div className="login-page">
            {/* 左侧品牌区域保持不变 */}
            <section className="login-hero">
                <div className="brand-row">
                    <div className="brand-icon">
                        <Icon icon="solar:notes-bold-duotone" width={26} />
                    </div>
                    <div>
                        <div className="brand-title">Note Matrix</div>
                        <div className="brand-subtitle">REACT ADMIN TEMPLATE</div>
                    </div>
                </div>

                <div className="login-hero-main">
                    <div className="login-badge">Frontend Preview</div>
                    <h1 className="login-title">Notes, cleanly managed.</h1>
                    <p className="login-desc">
                        A focused note workspace with login, list, search, pagination and CRUD interactions ready for
                        API integration.
                    </p>
                </div>

                <div className="login-metrics">
                    {[
                        ['Auth', 'Login and register'],
                        ['Users', 'Account workspace'],
                        ['API', 'Token ready']
                    ].map(([title, desc]) => (
                        <div key={title} className="login-metric">
                            <div className="login-metric-title">{title}</div>
                            <div className="login-metric-desc">{desc}</div>
                        </div>
                    ))}
                </div>
            </section>

            <section className="login-form-wrap">
                <div className="login-card">
                    <div className="login-card-kicker">System Auth</div>
                    <h2 className="login-card-title">{mode === 'login' ? '进入控制台' : '创建账号'}</h2>

                    <Segmented
                        block
                        className="login-mode"
                        value={mode}
                        options={[
                            { label: '登录', value: 'login' },
                            { label: '注册', value: 'register' }
                        ]}
                        onChange={handleModeChange}
                    />

                    <Form<AuthForm>
                        form={form}
                        layout="vertical"
                        initialValues={{ remember: true }}
                        onFinish={handleSubmit}
                    >
                        <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
                            <Input
                                size="large"
                                prefix={<Icon icon="solar:user-rounded-linear" />}
                                placeholder="请输入账号"
                            />
                        </Form.Item>
                        <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                            <Input.Password
                                size="large"
                                prefix={<Icon icon="solar:password-linear" />}
                                placeholder="请输入密码"
                            />
                        </Form.Item>

                        {/* ========== 验证码区域（仅登录模式显示） ========== */}
                        {mode === 'login' && (
                            <Form.Item
                                name="captchaCode"
                                label="验证码"
                                rules={[{ required: true, message: '请输入验证码' }]}
                            >
                                <div style={{ display: 'flex', gap: 8 }}>
                                    <Input
                                        size="large"
                                        prefix={<Icon icon="solar:shield-check-linear" />}
                                        placeholder="验证码"
                                        style={{ flex: 1 }}
                                    />
                                    <div
                                        onClick={getCaptcha}
                                        style={{
                                            cursor: 'pointer',
                                            borderRadius: 6,
                                            overflow: 'hidden',
                                            height: 40,
                                            width: 120,
                                            border: '1px solid #d9d9d9',
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            background: '#fafafa'
                                        }}
                                    >
                                        {captchaLoading ? (
                                            <Icon
                                                icon="solar:refresh-circle-bold"
                                                width={20}
                                                style={{ opacity: 0.5 }}
                                            />
                                        ) : captchaImage ? (
                                            <img
                                                src={captchaImage}
                                                alt="验证码"
                                                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                            />
                                        ) : (
                                            <span style={{ fontSize: 12, color: '#999' }}>点击获取</span>
                                        )}
                                    </div>
                                </div>
                            </Form.Item>
                        )}
                        {/* ================================================ */}

                        {mode === 'register' && (
                            <>
                                <Form.Item
                                    name="confirmPassword"
                                    label="确认密码"
                                    rules={[{ required: true, message: '请再次输入密码' }]}
                                >
                                    <Input.Password
                                        size="large"
                                        prefix={<Icon icon="solar:password-linear" />}
                                        placeholder="请再次输入密码"
                                    />
                                </Form.Item>
                                <Form.Item name="nickname" label="昵称">
                                    <Input
                                        size="large"
                                        prefix={<Icon icon="solar:user-id-linear" />}
                                        placeholder="可选"
                                    />
                                </Form.Item>
                                <Form.Item name="avatar" label="头像地址">
                                    <Input
                                        size="large"
                                        prefix={<Icon icon="solar:gallery-linear" />}
                                        placeholder="可选"
                                    />
                                </Form.Item>
                            </>
                        )}

                        <div className="login-options">
                            <Form.Item name="remember" valuePropName="checked" noStyle>
                                <Checkbox>保持登录</Checkbox>
                            </Form.Item>
                            <span className="login-auth-tip">Real API</span>
                        </div>
                        <Button
                            type="primary"
                            htmlType="submit"
                            size="large"
                            block
                            loading={loading}
                            icon={
                                <Icon icon={mode === 'login' ? 'solar:login-3-bold' : 'solar:user-plus-rounded-bold'} />
                            }
                        >
                            {mode === 'login' ? '登录' : '注册并登录'}
                        </Button>
                    </Form>
                </div>
            </section>
        </div>
    )
}

export default Login
