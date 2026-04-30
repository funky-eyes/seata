import { useEffect, useMemo, useState } from 'react';

import { EyeOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { Alert, Button, Empty, Form, Modal, Select, Space, Table, Tag, Typography } from 'antd';
import type { TableProps } from 'antd';

import { fetchClusterData, fetchNamespaces } from '@/api/naming';
import { buildNamespaceNodes, NamespaceNode } from '@/utils/namespaceHelper';
import type { ApiResponse, ClusterData, ClusterUnit, ConsoleRequestContext, NamingEndpoint, NamingInstance } from '@/api/types';
import { PageState } from '@/components/PageState';
import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';

type ClusterUnitRow = {
  name: string;
  unit: ClusterUnit;
  clusterType?: string;
};

type UnitModalState = {
  open: boolean;
  unitName?: string;
  instances: NamingInstance[];
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function readClusterData(response: ApiResponse<ClusterData> | ClusterData | unknown): ClusterData {
  if (isRecord(response) && isRecord(response.data)) {
    return response.data as ClusterData;
  }
  return isRecord(response) ? response as ClusterData : {};
}

function normalizedText(value: unknown) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}

function renderText(value: unknown, copyable = false) {
  const content = normalizedText(value);
  return (
    <Typography.Text copyable={copyable && content !== '-'} ellipsis={{ tooltip: content }}>
      {content}
    </Typography.Text>
  );
}

function formatEndpoint(value: NamingEndpoint | string | undefined) {
  if (typeof value === 'string') {
    return value || '-';
  }
  if (!value) {
    return '-';
  }
  const host = value.host ?? '';
  const port = value.port === undefined || value.port === null ? '' : String(value.port);
  if (!host && !port) {
    return '-';
  }
  return port ? `${host}:${port}` : host;
}

function metadataToText(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2);
    } catch {
      return value;
    }
  }
  return JSON.stringify(value, null, 2);
}

export function ClusterPage() {
  const { messages } = useConsoleI18n();
  const [form] = Form.useForm();
  const namespace = Form.useWatch('namespace', form)?.trim() ?? '';
  const clusterName = Form.useWatch('cluster', form)?.trim() ?? '';
  const [nodes, setNodes] = useState<NamespaceNode[]>([]);
  const [activeContext, setActiveContext] = useState<ConsoleRequestContext>({ namespace: '' });
  
  useEffect(() => {
    fetchNamespaces().then((res) => {
      if (res && res.data) {
        const nextNodes = buildNamespaceNodes(res.data);
        setNodes(nextNodes);
        if (!form.getFieldValue('namespace') && nextNodes.length > 0) {
          const firstNode = nextNodes[0];
          const initialContext: ConsoleRequestContext = {
            namespace: firstNode.namespace,
            cluster: firstNode.cluster
          };
          form.setFieldsValue(initialContext);
          setActiveContext(initialContext);
          void loadClusterData(initialContext);
        }
      }
    }).catch(console.error);
  }, [form]);

  const contextReady = Boolean(namespace && clusterName);
  const namespaceOptions = Array.from(new Set(nodes.map(n => n.namespace))).map(ns => ({ label: ns, value: ns }));
  const clusterOptions = Array.from(new Set(nodes.filter(n => n.namespace === namespace).map(n => n.cluster))).map(c => ({ label: c, value: c }));
  const [clusterData, setClusterData] = useState<ClusterData>({});
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>();
  const [unitModal, setUnitModal] = useState<UnitModalState>({ open: false, instances: [] });
  const [metadataModal, setMetadataModal] = useState<{ open: boolean; text: string }>({ open: false, text: '-' });

  async function loadClusterData(context: ConsoleRequestContext) {
    if (!context.namespace || !context.cluster) {
      setClusterData({});
      setErrorMessage(undefined);
      setUnitModal({ open: false, instances: [] });
      setMetadataModal({ open: false, text: '-' });
      return;
    }
    setLoading(true);
    setErrorMessage(undefined);
    try {
      const response = await fetchClusterData(context.namespace, context.cluster);
      setClusterData(readClusterData(response));
    } catch (error) {
      setClusterData({});
      setErrorMessage(error instanceof Error ? error.message : messages.cluster.loadFailure);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (contextReady) {
      return;
    }
    setClusterData({});
    setErrorMessage(undefined);
  }, [contextReady]);

  function handleQuery() {
    const nextContext: ConsoleRequestContext = { namespace, cluster: clusterName };
    setActiveContext(nextContext);
    void loadClusterData(nextContext);
  }

  function handleReload() {
    void loadClusterData(activeContext);
  }

  const unitRows = useMemo<ClusterUnitRow[]>(() => Object.entries(clusterData.unitData ?? {}).map(([name, unit]) => ({
    name,
    unit,
    clusterType: clusterData.clusterType
  })), [clusterData]);

  const unitColumns = useMemo<TableProps<ClusterUnitRow>['columns']>(() => [
    { title: messages.cluster.unitName, dataIndex: 'name', width: 240, fixed: 'left', render: (value) => renderText(value, true) },
    {
      title: messages.cluster.members,
      key: 'members',
      width: 130,
      render: (_, record) => renderText(record.unit.namingInstanceList?.length ?? 0)
    },
    { title: messages.cluster.clusterType, dataIndex: 'clusterType', width: 180, render: (value) => renderText(value) },
    {
      title: messages.cluster.operations,
      key: 'operations',
      width: 140,
      fixed: 'right',
      render: (_, record) => (
        <Button
          size="small"
          icon={<EyeOutlined />}
          onClick={() => setUnitModal({ open: true, unitName: record.name, instances: record.unit.namingInstanceList ?? [] })}
        >
          {messages.cluster.view}
        </Button>
      )
    }
  ], [messages.cluster]);

  const instanceColumns = useMemo<TableProps<NamingInstance>['columns']>(() => [
    { title: messages.cluster.control, dataIndex: 'control', width: 200, render: (value) => renderText(formatEndpoint(value), true) },
    { title: messages.cluster.transaction, dataIndex: 'transaction', width: 220, render: (value) => renderText(formatEndpoint(value), true) },
    { title: messages.cluster.internal, dataIndex: 'internal', width: 200, render: (value) => renderText(formatEndpoint(value), true) },
    { title: messages.cluster.weight, dataIndex: 'weight', width: 110, render: (value) => renderText(value) },
    {
      title: messages.cluster.healthy,
      dataIndex: 'healthy',
      width: 120,
      render: (value) => typeof value === 'boolean' ? <Tag color={value ? 'success' : 'error'}>{value ? messages.cluster.healthyYes : messages.cluster.healthyNo}</Tag> : renderText(value)
    },
    { title: messages.cluster.term, dataIndex: 'term', width: 120, render: (value) => renderText(value) },
    { title: messages.cluster.role, dataIndex: 'role', width: 140, render: (value) => renderText(value) },
    { title: messages.cluster.unit, dataIndex: 'unit', width: 160, render: (value) => renderText(value) },
    { title: messages.cluster.version, dataIndex: 'version', width: 160, render: (value) => renderText(value) },
    {
      title: messages.cluster.metadata,
      dataIndex: 'metadata',
      width: 150,
      fixed: 'right',
      render: (value) => value === undefined || value === null || value === '' ? renderText(value) : (
        <Button size="small" icon={<EyeOutlined />} onClick={() => setMetadataModal({ open: true, text: metadataToText(value) })}>
          {messages.cluster.viewMetadata}
        </Button>
      )
    }
  ], [messages.cluster]);

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack cluster-page">
        <div>
          <Typography.Title level={2}>{messages.pages.cluster}</Typography.Title>
          <Typography.Text type="secondary">{messages.cluster.subtitle}</Typography.Text>
        </div>

        <Form form={form} layout="inline" className="cluster-filter">
          <Form.Item name="namespace" label={'Namespace'}>
            <Select
              options={namespaceOptions}
              allowClear
              placeholder="Namespace"
              style={{ width: 140 }}
              onChange={() => form.setFieldsValue({ cluster: undefined })}
            />
          </Form.Item>
          <Form.Item name="cluster" label={'Cluster'}>
            <Select
              options={clusterOptions}
              allowClear
              placeholder="Cluster"
              style={{ width: 140 }}
            />
          </Form.Item>
          <Form.Item>
            <Space size={8}>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleQuery} disabled={!contextReady}>
                {messages.cluster.query}
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReload} loading={loading} disabled={!activeContext.namespace || !activeContext.cluster}>
                {messages.cluster.reload}
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {!contextReady ? (
          <PageState status="partial" title={messages.cluster.contextRequiredTitle} description={messages.cluster.contextRequiredDescription} />
        ) : null}
        {contextReady && errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}

        <Table<ClusterUnitRow>
          className="cluster-table"
          rowKey="name"
          columns={unitColumns}
          dataSource={activeContext.namespace && activeContext.cluster ? unitRows : []}
          loading={loading}
          scroll={{ x: 690 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={messages.cluster.emptyTitle} /> }}
          pagination={false}
        />

        <Modal
          width="min(1180px, calc(100vw - 32px))"
          title={`${messages.cluster.unitTitle}: ${unitModal.unitName ?? '-'}`}
          open={unitModal.open}
          footer={null}
          onCancel={() => setUnitModal({ open: false, instances: [] })}
        >
          <Table<NamingInstance>
            className="cluster-unit-table"
            rowKey={(record, index) => `${formatEndpoint(record.control)}-${formatEndpoint(record.transaction)}-${index ?? 0}`}
            columns={instanceColumns}
            dataSource={unitModal.instances}
            scroll={{ x: 1580 }}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={messages.cluster.emptyDescription} /> }}
          />
        </Modal>

        <Modal
          width="min(760px, calc(100vw - 32px))"
          title={messages.cluster.metadataTitle}
          open={metadataModal.open}
          footer={null}
          onCancel={() => setMetadataModal({ open: false, text: '-' })}
        >
          <pre className="cluster-metadata-json">{metadataModal.text}</pre>
        </Modal>
      </Space>
    </ConsoleLayout>
  );
}