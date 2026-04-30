import type { ReactNode } from 'react';

import { Button, Layout, Menu, Space, Typography, Segmented } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';

import { logout } from '@/api/auth';
import { useConsoleI18n } from '@/i18n';
import { navigationItems } from '@/layout/navigation';

const { Header, Content, Sider } = Layout;

type ConsoleLayoutProps = {
  children: ReactNode;
};

export function ConsoleLayout({ children }: ConsoleLayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { language, setLanguage, messages } = useConsoleI18n();
  const items = navigationItems(messages);
  const selectedKey = items.some((item) => item.path === location.pathname) ? location.pathname : '/transaction/list';

  return (
    <Layout className="console-shell">
      <Sider
        width={248}
        breakpoint="lg"
        collapsedWidth={0}
        className="console-sider"
        zeroWidthTriggerStyle={{
          top: 16,
          width: 40,
          height: 40,
          borderStartEndRadius: 8,
          borderEndEndRadius: 8,
          background: '#0054d1',
          boxShadow: '0 4px 12px rgba(0, 84, 209, 0.2)'
        }}
      >
        <div className="console-brand">
          <Typography.Title level={4}>Apache Seata</Typography.Title>
          <Typography.Text>{messages.common.console}</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedKey]}
          items={items.map(({ path, ...item }) => item)}
          onClick={({ key }) => navigate(String(key))}
        />
      </Sider>
      <Layout>
        <Header className="console-header">
          <div style={{ flex: 1 }} />
          <Space size={12} wrap>
            <Segmented
              size="small"
              options={[
                { label: 'EN', value: 'en-us' },
                { label: '中', value: 'zh-cn' }
              ]}
              value={language}
              onChange={(val) => setLanguage(val as 'en-us' | 'zh-cn')}
            />
            <Button onClick={logout}>{messages.common.signOut}</Button>
          </Space>
        </Header>
        <Content className="console-content">{children}</Content>
      </Layout>
    </Layout>
  );
}