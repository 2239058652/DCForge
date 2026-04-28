import React from 'react'
import { Button, Result } from 'antd'

const NotFound: React.FC = () => (
    <Result
        status="404"
        title="404"
        subTitle="Sorry, the page you visited does not exist."
        extra={
            <Button type="primary" onClick={() => window.history.go(-1)}>
                Back
            </Button>
        }
    />
)

export default NotFound
