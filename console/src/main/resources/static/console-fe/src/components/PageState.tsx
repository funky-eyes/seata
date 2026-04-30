import { Alert, Empty, Result, Skeleton } from 'antd';

type PageStateStatus = 'loading' | 'empty' | 'error' | 'success' | 'partial';

type PageStateProps = {
  status: PageStateStatus;
  title: string;
  description?: string;
};

export function PageState({ status, title, description }: PageStateProps) {
  if (status === 'loading') {
    return <Skeleton className="state-panel" active paragraph={{ rows: 6 }} />;
  }
  if (status === 'empty') {
    return <Empty className="state-panel" image={Empty.PRESENTED_IMAGE_SIMPLE} description={title} />;
  }
  if (status === 'error') {
    return <Result className="state-panel" status="error" title={title} subTitle={description} />;
  }
  if (status === 'partial') {
    return <Alert className="state-panel state-panel-alert" type="warning" showIcon message={title} description={description} />;
  }
  return <Result className="state-panel" status="success" title={title} subTitle={description} />;
}