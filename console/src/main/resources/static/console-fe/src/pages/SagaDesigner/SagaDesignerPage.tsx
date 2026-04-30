import { Space, Typography } from 'antd';

import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';

export function SagaDesignerPage() {
  const { messages } = useConsoleI18n();

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack">
        <div>
          <Typography.Title level={2}>{messages.pages.saga}</Typography.Title>
          <Typography.Text type="secondary">{messages.pages.sagaSubtitle}</Typography.Text>
        </div>
        <iframe
          className="saga-designer-frame"
          title="Saga State Machine Designer"
          src="./saga-statemachine-designer/designer.html"
          data-testid="saga-designer-frame"
        />
      </Space>
    </ConsoleLayout>
  );
}