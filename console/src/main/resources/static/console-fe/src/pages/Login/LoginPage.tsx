import { useState } from 'react';

import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Space, Typography, Segmented } from 'antd';
import { useNavigate } from 'react-router-dom';

import { login } from '@/api/auth';
import { useConsoleI18n } from '@/i18n';

export function LoginPage() {
  const navigate = useNavigate();
  const { language, setLanguage, messages } = useConsoleI18n();
  const [errorMessage, setErrorMessage] = useState<string>();
  const [submitting, setSubmitting] = useState(false);

  async function handleFinish(values: { username: string; password: string }) {
    setSubmitting(true);
    setErrorMessage(undefined);
    try {
      await login(values);
      navigate('/transaction/list', { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : messages.auth.failure);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="login-shell">
      <Card className="login-card" variant="borderless">
        <Space direction="vertical" size={24} className="login-stack">
          <div style={{ textAlign: 'right' }}>
            <Segmented
              size="small"
              options={[
                { label: 'EN', value: 'en-us' },
                { label: '中', value: 'zh-cn' }
              ]}
              value={language}
              onChange={(val) => setLanguage(val as 'en-us' | 'zh-cn')}
            />
          </div>
          <div>
            <Typography.Title level={2}>{messages.auth.title}</Typography.Title>
            <Typography.Text type="secondary">{messages.auth.subtitle}</Typography.Text>
          </div>
          {errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}
          <Form layout="vertical" requiredMark={false} onFinish={handleFinish} autoComplete="off">
            <Form.Item label={messages.auth.username} name="username" rules={[{ required: true, message: messages.auth.usernameRequired }]}>
              <Input prefix={<UserOutlined />} aria-label={messages.auth.username} autoFocus autoComplete="username" />
            </Form.Item>
            <Form.Item label={messages.auth.password} name="password" rules={[{ required: true, message: messages.auth.passwordRequired }]}>
              <Input.Password prefix={<LockOutlined />} aria-label={messages.auth.password} autoComplete="current-password" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={submitting}>
              {messages.auth.submit}
            </Button>
          </Form>
        </Space>
      </Card>
    </main>
  );
}