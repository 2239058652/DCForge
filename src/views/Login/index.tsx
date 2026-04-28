import { Icon } from '@iconify/react'
import { Button, Checkbox, Form, Input } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import './index.css'

interface LoginForm {
    username: string
    password: string
    remember?: boolean
}

const Login = () => {
    const navigate = useNavigate()
    const location = useLocation()
    const from = (location.state as { from?: string } | null)?.from || '/notes'

    const handleLogin = (values: LoginForm) => {
        const tokenName = import.meta.env.VITE_TOKEN_NAME || 'token'
        localStorage.setItem(tokenName, `mock-token-${values.username}`)
        navigate(from, { replace: true })
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
                        ['CRUD', 'Create and edit'],
                        ['Search', 'Quick filtering'],
                        ['API', 'Ready to wire']
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
                    <div className="login-card-kicker">System Sign In</div>
                    <h2 className="login-card-title">进入控制台</h2>

                    <Form<LoginForm> layout="vertical" initialValues={{ username: 'admin', password: '123456', remember: true }} onFinish={handleLogin}>
                        <Form.Item name="username" label="账号" rules={[{ required: true, message: '请输入账号' }]}>
                            <Input size="large" prefix={<Icon icon="solar:user-rounded-linear" />} placeholder="admin" />
                        </Form.Item>
                        <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
                            <Input.Password size="large" prefix={<Icon icon="solar:password-linear" />} placeholder="123456" />
                        </Form.Item>
                        <div className="login-options">
                            <Form.Item name="remember" valuePropName="checked" noStyle>
                                <Checkbox>保持登录</Checkbox>
                            </Form.Item>
                            <span className="login-auth-tip">Mock auth</span>
                        </div>
                        <Button type="primary" htmlType="submit" size="large" block icon={<Icon icon="solar:login-3-bold" />}>
                            登录
                        </Button>
                    </Form>
                </div>
            </section>
        </div>
    )
}

export default Login
