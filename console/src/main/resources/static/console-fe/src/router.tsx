import { Space, Typography } from 'antd';
import { HashRouter, Navigate, Route, Routes } from 'react-router-dom';

import type { ConsoleRequestContext } from '@/api/types';
import { PageState } from '@/components/PageState';
import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';
import { ClusterPage } from '@/pages/Cluster';
import { GlobalLocksPage } from '@/pages/GlobalLocks';
import { LoginPage } from '@/pages/Login';
import { SagaDesignerPage } from '@/pages/SagaDesigner';
import { TransactionGroupPage } from '@/pages/TransactionGroup';
import { TransactionsPage } from '@/pages/Transactions';

type RuntimePageProps = {
  title: string;
  status: 'empty' | 'partial';
};

function RuntimePage({ title, status }: RuntimePageProps) {
  const { messages } = useConsoleI18n();

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack">
        <div>
          <Typography.Title level={2}>{title}</Typography.Title>
        </div>
        <PageState status={status} title={messages.pages.runtimeEmpty} description={messages.pages.runtimeHint} />
      </Space>
    </ConsoleLayout>
  );
}

export function ConsoleRouter() {
  const { messages } = useConsoleI18n();

  return (
    <HashRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/transaction/list" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/overview" element={<RuntimePage title={messages.pages.overview} status="partial" />} />
        <Route path="/transaction-group" element={<TransactionGroupPage />} />
        <Route path="/transaction/list" element={<TransactionsPage />} />
        <Route path="/globallock/list" element={<GlobalLocksPage />} />
        <Route path="/cluster/list" element={<ClusterPage />} />
        <Route path="/sagastatemachinedesigner" element={<SagaDesignerPage />} />
        <Route path="*" element={<Navigate to="/transaction/list" replace />} />
      </Routes>
    </HashRouter>
  );
}
