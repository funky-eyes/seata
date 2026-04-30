import { useEffect, useMemo, useState } from 'react';

import { DeleteOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { App, Alert, Button, DatePicker, Empty, Form, Input, Modal, Select, Space, Table, Typography } from 'antd';
import type { TableProps } from 'antd';
import { useLocation } from 'react-router-dom';

import { checkGlobalLock, deleteGlobalLock, queryGlobalLocks } from '@/api/console';
import { fetchNamespaces } from '@/api/naming';
import { buildNamespaceNodes, NamespaceNode } from '@/utils/namespaceHelper';
import type { ApiResponse, GlobalLock, GlobalLockQuery, QueryParams } from '@/api/types';
import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';

type DateLike = {
  valueOf: () => number;
};

type SearchFormValues = {
  namespace?: string;
  cluster?: string;
  vgroup?: string;
  xid?: string;
  tableName?: string;
  transactionId?: string;
  branchId?: string;
  pk?: string;
  resourceId?: string;
  createTime?: [DateLike | null, DateLike | null] | null;
};

type NormalizedRows = {
  rows: GlobalLock[];
  total: number;
};

const PAGE_SIZE_OPTIONS = [10, 20, 30, 40, 50];

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function trimToUndefined(value?: string) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function readNumber(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

function readRowsAndTotal(value: unknown): NormalizedRows | undefined {
  if (Array.isArray(value)) {
    return { rows: value as GlobalLock[], total: value.length };
  }
  if (!isRecord(value)) {
    return undefined;
  }

  const total = readNumber(value.total) ?? readNumber(value.totalCount) ?? readNumber(value.count);
  if (Array.isArray(value.data)) {
    const rows = value.data as GlobalLock[];
    return { rows, total: total ?? rows.length };
  }
  if (Array.isArray(value.list)) {
    const rows = value.list as GlobalLock[];
    return { rows, total: total ?? rows.length };
  }
  return readRowsAndTotal(value.data);
}

function normalizeGlobalLocks(response: ApiResponse<unknown> | unknown) {
  const envelopeData = isRecord(response) && 'data' in response ? readRowsAndTotal(response.data) : undefined;
  return envelopeData ?? readRowsAndTotal(response) ?? { rows: [], total: 0 };
}

function formatTimestamp(value: GlobalLock['gmtCreate']) {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  const timestamp = typeof value === 'number' || /^\d+$/.test(String(value)) ? Number(value) : Date.parse(String(value));
  if (!Number.isFinite(timestamp)) {
    return String(value);
  }
  const date = new Date(timestamp);
  const pad = (part: number) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function renderText(value: unknown, copyable = false) {
  const content = value === null || value === undefined || value === '' ? '-' : String(value);
  return (
    <Typography.Text copyable={copyable && content !== '-'} ellipsis={{ tooltip: content }}>
      {content}
    </Typography.Text>
  );
}

function buildQuery(values: SearchFormValues, pageNum: number, pageSize: number): GlobalLockQuery {
  const [startTime, endTime] = values.createTime ?? [];
  return {
    pageNum,
    pageSize,
    xid: trimToUndefined(values.xid),
    tableName: trimToUndefined(values.tableName),
    transactionId: trimToUndefined(values.transactionId),
    branchId: trimToUndefined(values.branchId),
    pk: trimToUndefined(values.pk),
    resourceId: trimToUndefined(values.resourceId),
    timeStart: startTime ? startTime.valueOf() : undefined,
    timeEnd: endTime ? endTime.valueOf() : undefined
  };
}

function readString(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

function readInitialFilters(search: string, state: unknown): Partial<SearchFormValues> {
  const params = new URLSearchParams(search);
  const stateRecord = isRecord(state) ? state : undefined;
  return {
    namespace: readString(params.get('namespace')) ?? readString(stateRecord?.namespace),
    cluster: readString(params.get('cluster')) ?? readString(stateRecord?.cluster),
    vgroup: readString(params.get('vgroup')) ?? readString(stateRecord?.vgroup),
    xid: readString(params.get('xid')) ?? readString(stateRecord?.xid),
    branchId: readString(params.get('branchId')) ?? readString(stateRecord?.branchId)
  };
}

function readBooleanPayload(response: ApiResponse<unknown> | unknown) {
  if (typeof response === 'boolean') {
    return response;
  }
  if (isRecord(response)) {
    if (typeof response.data === 'boolean') {
      return response.data;
    }
    if (isRecord(response.data) && typeof response.data.data === 'boolean') {
      return response.data.data;
    }
  }
  return false;
}

function buildDeleteQuery(record: GlobalLock): QueryParams | undefined {
  const xid = record.xid === undefined || record.xid === null ? undefined : String(record.xid);
  const branchId = record.branchId === undefined || record.branchId === null ? undefined : String(record.branchId);
  if (!xid || !branchId) {
    return undefined;
  }
  return {
    xid,
    branchId,
    transactionId: record.transactionId,
    resourceId: record.resourceId,
    tableName: record.tableName,
    pk: record.pk,
    rowKey: record.rowKey
  };
}

function buildContext(values: Partial<SearchFormValues>) {
  return {
    namespace: trimToUndefined(values.namespace) ?? '',
    cluster: trimToUndefined(values.cluster),
    vgroup: trimToUndefined(values.vgroup)
  };
}

function hasConsoleContext(context: ReturnType<typeof buildContext>) {
  return Boolean(context.namespace && (context.cluster || context.vgroup));
}

function hasInitialSearch(filters: Partial<SearchFormValues>) {
  return Boolean(
    trimToUndefined(filters.namespace)
      || trimToUndefined(filters.cluster)
      || trimToUndefined(filters.vgroup)
      || trimToUndefined(filters.xid)
      || trimToUndefined(filters.branchId)
  );
}

export function GlobalLocksPage() {
  const { message } = App.useApp();
  const { messages } = useConsoleI18n();
  const location = useLocation();
  const initialFilters = useMemo(() => readInitialFilters(location.search, location.state), [location.search, location.state]);
  const [form] = Form.useForm<SearchFormValues>();
  const [pageContext, setPageContext] = useState(() => buildContext(initialFilters));
  const [query, setQuery] = useState<GlobalLockQuery>(() => buildQuery(initialFilters, 1, 10));
  const [rows, setRows] = useState<GlobalLock[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [deletingKey, setDeletingKey] = useState<string>();
  const [errorMessage, setErrorMessage] = useState<string>();
  const [nodes, setNodes] = useState<NamespaceNode[]>([]);

  useEffect(() => {
    fetchNamespaces().then((res) => {
      if (res && res.data) {
        setNodes(buildNamespaceNodes(res.data));
      }
    }).catch(console.error);
  }, []);

  const selectedNamespace = Form.useWatch('namespace', form);
  const selectedCluster = Form.useWatch('cluster', form);

  const namespaceOptions = Array.from(new Set(nodes.map(n => n.namespace))).map(ns => ({ label: ns, value: ns }));
  const clusterOptions = Array.from(new Set(nodes.filter(n => n.namespace === selectedNamespace).map(n => n.cluster))).map(c => ({ label: c, value: c }));
  const vgroupOptions = Array.from(new Set(nodes.filter(n => n.namespace === selectedNamespace && n.cluster === selectedCluster).flatMap(n => n.vgroups))).map(v => ({ label: v, value: v }));


  async function loadLocks(nextQuery: GlobalLockQuery, contextToUse = pageContext) {
    setLoading(true);
    setErrorMessage(undefined);
    try {
      const response = await queryGlobalLocks({ namespace: contextToUse.namespace, cluster: contextToUse.cluster, vgroup: contextToUse.vgroup }, nextQuery);
      const normalized = normalizeGlobalLocks(response);
      setRows(normalized.rows);
      setTotal(normalized.total);
    } catch (error) {
      setRows([]);
      setTotal(0);
      setErrorMessage(error instanceof Error ? error.message : messages.locks.loadFailure);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    const initialContext = buildContext(initialFilters);
    if (!hasInitialSearch(initialFilters) || !hasConsoleContext(initialContext)) {
      return;
    }
    const nextQuery = buildQuery(initialFilters, 1, 10);
    setPageContext(initialContext);
    setQuery(nextQuery);
    void loadLocks(nextQuery, initialContext);
  }, [initialFilters]);

  function handleSearch(values: SearchFormValues) {
    const nextQuery = buildQuery(values, 1, query.pageSize);
    const newContext = buildContext(values);
    setPageContext(newContext);
    setQuery(nextQuery);
    void loadLocks(nextQuery, newContext);
  }

  function handleReset() {
    form.setFieldsValue({
      namespace: undefined,
      cluster: undefined,
      vgroup: undefined,
      createTime: undefined,
      xid: undefined,
      tableName: undefined,
      transactionId: undefined,
      branchId: undefined,
      pk: undefined,
      resourceId: undefined
    });
    const nextQuery = buildQuery({}, 1, query.pageSize);
    const newContext = { namespace: '', cluster: '', vgroup: '' };
    setPageContext(newContext);
    setQuery(nextQuery);
    setRows([]);
    setTotal(0);
    setErrorMessage(undefined);
  }

  function handlePageChange(pageNum: number, pageSize: number) {
    const nextQuery = buildQuery(form.getFieldsValue(), pageNum, pageSize);
    setQuery(nextQuery);
    void loadLocks(nextQuery, pageContext);
  }

  function warningContent(branchAffected: boolean) {
    return (
      <Space direction="vertical" size={8}>
        <Typography.Text>{messages.locks.confirmDelete}</Typography.Text>
        <Alert
          type="warning"
          showIcon
          message={messages.locks.warningTitle}
          description={
            <Space direction="vertical" size={4}>
              <Typography.Text>{messages.locks.dirtyWriteWarning}</Typography.Text>
              {branchAffected ? <Typography.Text>{messages.locks.branchAffectedWarning}</Typography.Text> : null}
            </Space>
          }
        />
      </Space>
    );
  }

  async function confirmDelete(record: GlobalLock) {
    const deleteQuery = buildDeleteQuery(record);
    if (!deleteQuery) {
      setErrorMessage(messages.locks.deleteKeyRequired);
      return;
    }

    const key = `${deleteQuery.xid}-${deleteQuery.branchId}`;
    setDeletingKey(key);
    setErrorMessage(undefined);
    try {
      const checkResponse = await checkGlobalLock({ namespace: pageContext.namespace, cluster: pageContext.cluster, vgroup: pageContext.vgroup }, { xid: String(deleteQuery.xid), branchId: String(deleteQuery.branchId) });
      const branchAffected = readBooleanPayload(checkResponse);
      Modal.confirm({
        title: messages.transactions.confirmTitle,
        content: warningContent(branchAffected),
        okButtonProps: { danger: true },
        onOk: async () => {
          await deleteGlobalLock({ namespace: pageContext.namespace, cluster: pageContext.cluster, vgroup: pageContext.vgroup }, deleteQuery);
          message.success(messages.locks.deleteSuccess);
          await loadLocks(query, pageContext);
        }
      });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : messages.locks.deleteFailure);
    } finally {
      setDeletingKey(undefined);
    }
  }

  const columns = useMemo<TableProps<GlobalLock>['columns']>(() => [
    { title: 'xid', dataIndex: 'xid', width: 260, fixed: 'left', render: (value) => renderText(value, true) },
    { title: 'transactionId', dataIndex: 'transactionId', width: 160, render: (value) => renderText(value) },
    { title: 'branchId', dataIndex: 'branchId', width: 160, render: (value) => renderText(value, true) },
    { title: 'resourceId', dataIndex: 'resourceId', width: 220, render: (value) => renderText(value) },
    { title: 'tableName', dataIndex: 'tableName', width: 180, render: (value) => renderText(value) },
    { title: 'pk', dataIndex: 'pk', width: 160, render: (value) => renderText(value) },
    { title: 'rowKey', dataIndex: 'rowKey', width: 240, render: (value) => renderText(value) },
    { title: 'gmtCreate', dataIndex: 'gmtCreate', width: 180, render: formatTimestamp },
    { title: 'gmtModified', dataIndex: 'gmtModified', width: 180, render: formatTimestamp },
    {
      title: messages.transactions.operations,
      key: 'operations',
      width: 120,
      fixed: 'right',
      render: (_, record) => {
        const rowKey = `${record.xid ?? ''}-${record.branchId ?? ''}`;
        return (
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            loading={deletingKey === rowKey}
            onClick={() => void confirmDelete(record)}
          >
            {messages.locks.delete}
          </Button>
        );
      }
    }
  ], [deletingKey, messages.locks, messages.transactions]);

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack global-lock-page">
        <div>
          <Typography.Title level={2}>{messages.pages.locks}</Typography.Title>
          <Typography.Text type="secondary">{messages.locks.subtitle}</Typography.Text>
        </div>

        {errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}

        <Form<SearchFormValues>
          form={form}
          layout="inline"
          className="global-lock-filter"
          initialValues={initialFilters}
          onFinish={handleSearch}
        >
          <Form.Item name="createTime" label={messages.transactions.createTime}>
            <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm:ss" />
          </Form.Item>
          <Form.Item name="namespace" label={'Namespace'}>
            <Select
              options={namespaceOptions}
              allowClear
              placeholder="Namespace"
              style={{ width: 140 }}
              onChange={() => form.setFieldsValue({ cluster: undefined, vgroup: undefined })}
            />
          </Form.Item>
          <Form.Item name="cluster" label={'Cluster'}>
            <Select
              options={clusterOptions}
              allowClear
              placeholder="Cluster"
              style={{ width: 140 }}
              onChange={() => form.setFieldsValue({ vgroup: undefined })}
            />
          </Form.Item>
          <Form.Item name="vgroup" label={'VGroup'}>
            <Select
              options={vgroupOptions}
              allowClear
              placeholder="vGroup"
              style={{ width: 140 }}
            />
          </Form.Item>
          <Form.Item name="xid" label="xid">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="tableName" label="tableName">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="transactionId" label="transactionId">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="branchId" label="branchId">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="pk" label="pk">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="resourceId" label="resourceId">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item>
            <Space size={8}>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                {messages.transactions.search}
              </Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>
                {messages.transactions.reset}
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Table<GlobalLock>
          className="global-lock-table"
          rowKey={(record) => `${record.xid ?? ''}-${record.branchId ?? ''}-${record.rowKey ?? record.pk ?? ''}`}
          columns={columns}
          dataSource={rows}
          loading={loading}
          scroll={{ x: 1860 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={messages.locks.emptyTitle} /> }}
          pagination={{
            total,
            current: query.pageNum,
            pageSize: query.pageSize,
            showSizeChanger: true,
            pageSizeOptions: PAGE_SIZE_OPTIONS,
            showTotal: (rowTotal) => `${rowTotal}`,
            onChange: handlePageChange
          }}
        />
      </Space>
    </ConsoleLayout>
  );
}