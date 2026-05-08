import { Icon } from '@iconify/react'
import { App, Button, Checkbox, Form, Input, Segmented } from 'antd'
import { useState } from 'react'
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
}

type AuthMode = 'login' | 'register'

const Login = () => {
    const { message } = App.useApp()
    const [form] = Form.useForm<AuthForm>()
    const [mode, setMode] = useState<AuthMode>('login')
    const [loading, setLoading] = useState(false)
    const navigate = useNavigate()
    const location = useLocation()
    const from = (location.state as { from?: string } | null)?.from || '/notes'

    const saveLogin = (user: UserLoginResult) => {
        const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
        localStorage.setItem(tokenName, user.token)
        localStorage.setItem('userInfo', JSON.stringify(user))
    }

    const login = async (username: string, password: string) => {
        const result = await userApi.login({ username, password })
        if (result.code !== 200 || !result.data?.token) {
            message.error(result.message || '登录失败')
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
                await login(username, password)
                return
            }

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

            await login(username, password)
        } catch {
            message.error(mode === 'login' ? '登录失败，请检查后端服务' : '注册失败，请检查后端服务')
        } finally {
            setLoading(false)
        }
    }

    const handleModeChange = (value: string) => {
        setMode(value as AuthMode)
        form.resetFields()
    }

    return (
        <div className="login-page">
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
                    <p className="login-desc">A focused note workspace with login, list, search, pagination and CRUD interactions ready for API integration.</p>
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

                    <Form<AuthForm> form={form} layout="vertical" initialValues={{ remember: true }} onFinish={handleSubmit}>
                        <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
                            <Input size="large" prefix={<Icon icon="solar:user-rounded-linear" />} placeholder="请输入账号" />
                        </Form.Item>
                        <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                            <Input.Password size="large" prefix={<Icon icon="solar:password-linear" />} placeholder="请输入密码" />
                        </Form.Item>

                        {mode === 'register' && (
                            <>
                                <Form.Item name="confirmPassword" label="确认密码" rules={[{ required: true, message: '请再次输入密码' }]}>
                                    <Input.Password size="large" prefix={<Icon icon="solar:password-linear" />} placeholder="请再次输入密码" />
                                </Form.Item>
                                <Form.Item name="nickname" label="昵称">
                                    <Input size="large" prefix={<Icon icon="solar:user-id-linear" />} placeholder="可选" />
                                </Form.Item>
                                <Form.Item name="avatar" label="头像地址">
                                    <Input size="large" prefix={<Icon icon="solar:gallery-linear" />} placeholder="可选" />
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
                            icon={<Icon icon={mode === 'login' ? 'solar:login-3-bold' : 'solar:user-plus-rounded-bold'} />}
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
