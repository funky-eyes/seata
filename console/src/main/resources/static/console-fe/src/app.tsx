import { useEffect } from 'react';

import { App as AntdApp, ConfigProvider, theme } from 'antd';
import enUS from 'antd/locale/en_US';
import zhCN from 'antd/locale/zh_CN';

import { configureRequestFeedback } from '@/api/request';
import { ConsoleI18nProvider, useConsoleI18n } from '@/i18n';
import { ConsoleRouter } from '@/router';
import '@/styles/tokens.css';
import '@/styles/global.css';

function RequestFeedbackBridge() {
  const { message } = AntdApp.useApp();

  useEffect(() => {
    configureRequestFeedback(message);
  }, [message]);

  return <ConsoleRouter />;
}

function ConsoleAppShell() {
  const { language } = useConsoleI18n();

  return (
    <ConfigProvider
      locale={language === 'zh-cn' ? zhCN : enUS}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#0054d1',
          colorInfo: '#02bcfb',
          colorSuccess: '#0054d1',
          colorWarning: '#faad14',
          colorError: '#ff4d4f',
          borderRadius: 6,
          boxShadow: '0 6px 16px rgba(0, 84, 209, 0.08)',
          controlHeight: 36,
          fontFamily:
            'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif'
        }
      }}
    >
      <AntdApp>
        <RequestFeedbackBridge />
      </AntdApp>
    </ConfigProvider>
  );
}

export default function App() {
  return (
    <ConsoleI18nProvider>
      <ConsoleAppShell />
    </ConsoleI18nProvider>
  );
}
