import { useEffect, useMemo, useState } from 'react';

import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { App, Alert, Button, DatePicker, Empty, Form, Input, Modal, Select, Space, Switch, Table, Tag, Typography } from 'antd';
import type { TableProps } from 'antd';
import { useNavigate } from 'react-router-dom';

import {
  changeGlobalStatus,
  deleteBranchSession,
  deleteGlobalSession,
  forceDeleteBranchSession,
  forceDeleteGlobalSession,
  queryGlobalSessions,
  sendCommitOrRollback,
  startBranchSession,
  startGlobalSession,
  stopBranchSession,
  stopGlobalSession
} from '@/api/console';
import { fetchNamespaces } from '@/api/naming';
import type { BranchSession, BranchSessionAction, ConsoleRequestContext, GlobalSession, GlobalSessionQuery } from '@/api/types';
import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';
import { buildNamespaceNodes } from '@/utils/namespaceHelper';
import type { NamespaceNode } from '@/utils/namespaceHelper';

type DateLike = {
  valueOf: () => number;
};

type SearchFormValues = {
  namespace?: string;
  cluster?: string;
  vgroup?: string;
  xid?: string;
  applicationId?: string;
  transactionName?: string;
  status?: number;
  createTime?: [DateLike | null, DateLike | null] | null;
  withBranch?: boolean;
};

type BranchModalState = {
  open: boolean;
  xid?: string;
  rows: BranchSession[];
};

type StatusOption = {
  value: number;
  label: string;
  color: 'default' | 'processing' | 'success' | 'error' | 'warning';
};

const PAGE_SIZE_OPTIONS = [10, 20, 30, 40, 50];

const GLOBAL_STATUS_OPTIONS: StatusOption[] = [
  { value: 0, label: 'UnKnown', color: 'warning' },
  { value: 1, label: 'Begin', color: 'processing' },
  { value: 2, label: 'Committing', color: 'processing' },
  { value: 3, label: 'CommitRetrying', color: 'processing' },
  { value: 4, label: 'Rollbacking', color: 'processing' },
  { value: 5, label: 'RollbackRetrying', color: 'processing' },
  { value: 6, label: 'TimeoutRollbacking', color: 'processing' },
  { value: 7, label: 'TimeoutRollbackRetrying', color: 'processing' },
  { value: 8, label: 'AsyncCommitting', color: 'processing' },
  { value: 9, label: 'Committed', color: 'success' },
  { value: 10, label: 'CommitFailed', color: 'error' },
  { value: 11, label: 'Rollbacked', color: 'error' },
  { value: 12, label: 'RollbackFailed', color: 'error' },
  { value: 13, label: 'TimeoutRollbacked', color: 'error' },
  { value: 14, label: 'TimeoutRollbackFailed', color: 'error' },
  { value: 15, label: 'Finished', color: 'success' },
  { value: 16, label: 'CommitRetryTimeout', color: 'error' },
  { value: 17, label: 'RollbackRetryTimeout', color: 'error' },
  { value: 18, label: 'Deleting', color: 'warning' },
  { value: 19, label: 'StopCommitRetry', color: 'processing' },
  { value: 20, label: 'StopRollbackRetry', color: 'processing' }
];

const BRANCH_STATUS_OPTIONS: StatusOption[] = [
  { value: 0, label: 'UnKnown', color: 'warning' },
  { value: 1, label: 'Registered', color: 'processing' },
  { value: 2, label: 'PhaseOne_Done', color: 'processing' },
  { value: 3, label: 'PhaseOne_Failed', color: 'error' },
  { value: 4, label: 'PhaseOne_Timeout', color: 'error' },
  { value: 5, label: 'PhaseTwo_Committed', color: 'success' },
  { value: 6, label: 'PhaseTwo_CommitFailed_Retryable', color: 'processing' },
  { value: 7, label: 'PhaseTwo_CommitFailed_Unretryable', color: 'error' },
  { value: 8, label: 'PhaseTwo_Rollbacked', color: 'error' },
  { value: 9, label: 'PhaseTwo_RollbackFailed_Retryable', color: 'processing' },
  { value: 10, label: 'PhaseTwo_RollbackFailed_Unretryable', color: 'error' },
  { value: 11, label: 'PhaseTwo_CommitFailed_XAER_NOTA_Retryable', color: 'processing' },
  { value: 12, label: 'PhaseTwo_RollbackFailed_XAER_NOTA_Retryable', color: 'processing' },
  { value: 13, label: 'PhaseOne_RDONLY', color: 'success' },
  { value: 14, label: 'Stop_Retry', color: 'processing' }
];

const GLOBAL_STATUS_BY_VALUE = new Map(GLOBAL_STATUS_OPTIONS.map((status) => [status.value, status]));
const BRANCH_STATUS_BY_VALUE = new Map(BRANCH_STATUS_OPTIONS.map((status) => [status.value, status]));

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

function readRowsAndTotal(value: unknown): { rows: GlobalSession[]; total: number } | undefined {
  if (Array.isArray(value)) {
    return { rows: value as GlobalSession[], total: value.length };
  }
  if (!isRecord(value)) {
    return undefined;
  }

  const total = readNumber(value.total) ?? readNumber(value.totalCount) ?? readNumber(value.count);
  if (Array.isArray(value.data)) {
    const rows = value.data as GlobalSession[];
    return { rows, total: total ?? rows.length };
  }
  if (Array.isArray(value.list)) {
    const rows = value.list as GlobalSession[];
    return { rows, total: total ?? rows.length };
  }
  return readRowsAndTotal(value.data);
}

function normalizeGlobalSessions(payload: unknown) {
  const normalized = readRowsAndTotal(payload) ?? { rows: [], total: 0 };
  const rows = normalized.rows.map((row) => ({
    ...row,
    branchSessionVOs: normalizeBranchSessions(row)
  }));
  return { rows, total: normalized.total };
}

function normalizeBranchSessions(row: GlobalSession) {
  const branches = Array.isArray(row.branchSessionVOs) ? row.branchSessionVOs : [];
  return branches.map((branch) => ({ ...branch, xid: branch.xid ?? row.xid }));
}

function formatTimestamp(value: GlobalSession['beginTime']) {
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

function renderStatus(value: unknown, options: Map<number, StatusOption>) {
  const statusValue = Number(value);
  const status = options.get(statusValue);
  if (!status) {
    return <Tag>{renderText(value)}</Tag>;
  }
  return <Tag color={status.color}>{status.label}</Tag>;
}

function buildQuery(values: SearchFormValues, pageNum: number, pageSize: number): GlobalSessionQuery {
  const [startTime, endTime] = values.createTime ?? [];
  return {
    pageNum,
    pageSize,
    xid: trimToUndefined(values.xid),
    applicationId: trimToUndefined(values.applicationId),
    transactionName: trimToUndefined(values.transactionName),
    status: values.status,
    withBranch: Boolean(values.withBranch),
    timeStart: startTime ? startTime.valueOf() : undefined,
    timeEnd: endTime ? endTime.valueOf() : undefined
  };
}

export function TransactionsPage() {
  const { message } = App.useApp();
  const { messages } = useConsoleI18n();
  const navigate = useNavigate();
  const [form] = Form.useForm<SearchFormValues>();
  const [query, setQuery] = useState<GlobalSessionQuery>({ pageNum: 1, pageSize: 10, withBranch: false });
  const [rows, setRows] = useState<GlobalSession[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>();
  const [branchModal, setBranchModal] = useState<BranchModalState>({ open: false, rows: [] });
  const [namespaceNodes, setNamespaceNodes] = useState<NamespaceNode[]>([]);
  const [pageContext, setPageContext] = useState<ConsoleRequestContext>({ namespace: '' });

  async function loadSessions(nextQuery: GlobalSessionQuery, ctx: ConsoleRequestContext) {
    setLoading(true);
    setErrorMessage(undefined);
    try {
      const response = await queryGlobalSessions(ctx, nextQuery);
      const normalized = normalizeGlobalSessions(response);
      setRows(normalized.rows);
      setTotal(normalized.total);
      setBranchModal((current) => {
        if (!current.open || !current.xid) {
          return current;
        }
        const selectedRow = normalized.rows.find((row) => row.xid === current.xid);
        return { ...current, rows: selectedRow?.branchSessionVOs ?? [] };
      });
    } catch (error) {
      setRows([]);
      setTotal(0);
      setErrorMessage(error instanceof Error ? error.message : messages.transactions.loadFailure);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchNamespaces().then(res => {
      if (res.success && res.data) {
        const nodes = buildNamespaceNodes(res.data);
        setNamespaceNodes(nodes);
        if (nodes.length > 0) {
          const firstNs = nodes[0];
          const defaultValues = {
            namespace: firstNs.namespace,
            cluster: firstNs.cluster,
            vgroup: undefined
          };
          form.setFieldsValue(defaultValues);
          
          const initialCtx: ConsoleRequestContext = { 
            namespace: defaultValues.namespace,
            cluster: defaultValues.cluster
          };
          setPageContext(initialCtx);
          
          const nextQuery = buildQuery(form.getFieldsValue(), 1, query.pageSize);
          setQuery(nextQuery);
          void loadSessions(nextQuery, initialCtx);
        }
      }
    }).catch((e) => {
      message.error(e.message);
    });
  }, []);

  function handleSearch(values: SearchFormValues) {
    const nextQuery = buildQuery(values, 1, query.pageSize);
    setQuery(nextQuery);
    const nextCtx: ConsoleRequestContext = { 
      namespace: values.namespace || '', 
      cluster: values.cluster, 
      vgroup: values.vgroup 
    };
    setPageContext(nextCtx);
    void loadSessions(nextQuery, nextCtx);
  }

  function handleReset() {
    form.resetFields();
    // Re-apply default options based on namespaceNodes if possible, or just keep what was there.
    const resetCtx: ConsoleRequestContext = { namespace: '' };
    if (namespaceNodes.length > 0) {
      const firstNs = namespaceNodes[0];
      form.setFieldsValue({
        namespace: firstNs.namespace,
        cluster: firstNs.cluster,
        vgroup: undefined
      });
      resetCtx.namespace = firstNs.namespace;
      resetCtx.cluster = firstNs.cluster;
    }
    const nextQuery = buildQuery({ withBranch: false }, 1, query.pageSize);
    setQuery(nextQuery);
    setPageContext(resetCtx);
    void loadSessions(nextQuery, resetCtx);
  }

  function handlePageChange(pageNum: number, pageSize: number) {
    const nextQuery = buildQuery(form.getFieldsValue(), pageNum, pageSize);
    setQuery(nextQuery);
    void loadSessions(nextQuery, pageContext);
  }

  function openBranchSessions(record: GlobalSession) {
    setBranchModal({ open: true, xid: record.xid, rows: normalizeBranchSessions(record) });
  }

  function openGlobalLocks(record: GlobalSession) {
    const xid = requireXid(record);
    if (!xid) {
      return;
    }
    const params: Record<string, string> = { xid };
    if (pageContext.namespace) params.namespace = pageContext.namespace;
    if (pageContext.cluster) params.cluster = pageContext.cluster;
    if (pageContext.vgroup) params.vgroup = pageContext.vgroup;
    const search = new URLSearchParams(params).toString();
    navigate({ pathname: '/globallock/list', search: `?${search}` });
  }

  function warningContent(description: string, warnings: string[]) {
    return (
      <Space direction="vertical" size={8}>
        <Typography.Text>{description}</Typography.Text>
        {warnings.length > 0 ? (
          <Alert
            type="warning"
            showIcon
            message={messages.transactions.warningTitle}
            description={
              <Space direction="vertical" size={4}>
                <Typography.Text>{messages.transactions.warningCommon}</Typography.Text>
                <ul className="transaction-warning-list">
                  {warnings.map((warning) => (
                    <li key={warning}>{warning}</li>
                  ))}
                </ul>
              </Space>
            }
          />
        ) : null}
      </Space>
    );
  }

  function confirmAction(description: string, action: () => Promise<unknown>, warnings: string[] = [], danger = false) {
    Modal.confirm({
      title: messages.transactions.confirmTitle,
      content: warningContent(description, warnings),
      okButtonProps: { danger },
      onOk: async () => {
        await action();
        message.success(messages.transactions.success);
        await loadSessions(query, pageContext);
      }
    });
  }

  function requireXid(record: GlobalSession) {
    if (!record.xid) {
      setErrorMessage(messages.transactions.xidRequired);
      return undefined;
    }
    return record.xid;
  }

  function branchAction(record: BranchSession): BranchSessionAction | undefined {
    const xid = record.xid ?? branchModal.xid;
    const branchId = record.branchId === undefined || record.branchId === null ? undefined : String(record.branchId);
    if (!xid) {
      setErrorMessage(messages.transactions.xidRequired);
      return undefined;
    }
    if (!branchId) {
      setErrorMessage(messages.transactions.branchIdRequired);
      return undefined;
    }
    return { xid, branchId };
  }

  function globalWarnings(forceDelete: boolean) {
    return forceDelete ? ['AT: ' + messages.transactions.forceDeleteWarning, 'XA: ' + messages.transactions.forceDeleteWarning, 'TCC: ' + messages.transactions.forceDeleteWarning, 'SAGA: ' + messages.transactions.forceDeleteWarning] : [];
  }

  function branchWarnings(record: BranchSession, operation: 'delete' | 'forceDelete' | 'stop') {
    if (operation === 'forceDelete') {
      return [messages.transactions.forceDeleteWarning];
    }
    if (operation === 'delete') {
      if (record.branchType === 'AT') {
        return [messages.transactions.deleteBranchAtWarning];
      }
      if (record.branchType === 'XA') {
        return [messages.transactions.deleteBranchXaWarning];
      }
      return [];
    }
    return record.branchType === 'TCC' ? [messages.transactions.stopBranchTccWarning] : [];
  }

  const globalColumns = useMemo<TableProps<GlobalSession>['columns']>(() => [
    { title: 'xid', dataIndex: 'xid', width: 260, fixed: 'left', render: (value) => renderText(value, true) },
    { title: 'transactionId', dataIndex: 'transactionId', width: 160, render: (value) => renderText(value) },
    { title: 'applicationId', dataIndex: 'applicationId', width: 180, render: (value) => renderText(value) },
    { title: 'transactionServiceGroup', dataIndex: 'transactionServiceGroup', width: 220, render: (value) => renderText(value) },
    { title: 'transactionName', dataIndex: 'transactionName', width: 200, render: (value) => renderText(value) },
    { title: 'status', dataIndex: 'status', width: 180, render: (value) => renderStatus(value, GLOBAL_STATUS_BY_VALUE) },
    { title: 'timeout', dataIndex: 'timeout', width: 110, render: (value) => renderText(value) },
    { title: 'beginTime', dataIndex: 'beginTime', width: 180, render: formatTimestamp },
    { title: 'applicationData', dataIndex: 'applicationData', width: 220, render: (value) => renderText(value) },
    {
      title: messages.transactions.operations,
      key: 'operations',
      width: 380,
      fixed: 'right',
      render: (_, record) => {
        const canOpenBranches = Boolean(query.withBranch && record.branchSessionVOs?.length);
        const retryStopped = record.status === 19 || record.status === 20;
        return (
          <Space size={8} wrap>
            {canOpenBranches ? <Button size="small" onClick={() => openBranchSessions(record)}>{messages.transactions.branchSessions}</Button> : null}
            <Button size="small" onClick={() => openGlobalLocks(record)}>{messages.transactions.globalLocks}</Button>
            <Button
              size="small"
              danger
              onClick={() => {
                const xid = requireXid(record);
                if (xid) {
                  confirmAction(messages.transactions.confirmDeleteGlobal, () => deleteGlobalSession(pageContext, xid), globalWarnings(false), true);
                }
              }}
            >
              {messages.transactions.deleteGlobal}
            </Button>
            <Button
              size="small"
              danger
              onClick={() => {
                const xid = requireXid(record);
                if (xid) {
                  confirmAction(messages.transactions.confirmForceDeleteGlobal, () => forceDeleteGlobalSession(pageContext, xid), globalWarnings(true), true);
                }
              }}
            >
              {messages.transactions.forceDeleteGlobal}
            </Button>
            <Button
              size="small"
              onClick={() => {
                const xid = requireXid(record);
                if (xid) {
                  confirmAction(
                    retryStopped ? messages.transactions.confirmStartGlobal : messages.transactions.confirmStopGlobal,
                    () => (retryStopped ? startGlobalSession(pageContext, xid) : stopGlobalSession(pageContext, xid))
                  );
                }
              }}
            >
              {retryStopped ? messages.transactions.startGlobalRetry : messages.transactions.stopGlobalRetry}
            </Button>
            <Button
              size="small"
              onClick={() => {
                const xid = requireXid(record);
                if (xid) {
                  confirmAction(messages.transactions.confirmSend, () => sendCommitOrRollback(pageContext, xid));
                }
              }}
            >
              {messages.transactions.sendCommitOrRollback}
            </Button>
            <Button
              size="small"
              onClick={() => {
                const xid = requireXid(record);
                if (xid) {
                  confirmAction(messages.transactions.confirmChangeStatus, () => changeGlobalStatus(pageContext, xid));
                }
              }}
            >
              {messages.transactions.changeGlobalStatus}
            </Button>
          </Space>
        );
      }
    }
  ], [pageContext, messages.transactions, navigate, query.withBranch, query]);

  const branchColumns = useMemo<TableProps<BranchSession>['columns']>(() => [
    { title: 'transactionId', dataIndex: 'transactionId', width: 160, render: (value) => renderText(value) },
    { title: 'branchId', dataIndex: 'branchId', width: 160, render: (value) => renderText(value, true) },
    { title: 'resourceGroupId', dataIndex: 'resourceGroupId', width: 180, render: (value) => renderText(value) },
    { title: 'branchType', dataIndex: 'branchType', width: 120, render: (value) => renderText(value) },
    { title: 'status', dataIndex: 'status', width: 220, render: (value) => renderStatus(value, BRANCH_STATUS_BY_VALUE) },
    { title: 'resourceId', dataIndex: 'resourceId', width: 220, render: (value) => renderText(value) },
    { title: 'clientId', dataIndex: 'clientId', width: 180, render: (value) => renderText(value) },
    { title: 'applicationData', dataIndex: 'applicationData', width: 220, render: (value) => renderText(value) },
    {
      title: messages.transactions.operations,
      key: 'operations',
      width: 320,
      fixed: 'right',
      render: (_, record) => {
        const retryStopped = record.status === 14;
        return (
          <Space size={8} wrap>
            <Button
              size="small"
              danger
              onClick={() => {
                const action = branchAction(record);
                if (action) {
                  confirmAction(messages.transactions.confirmDeleteBranch, () => deleteBranchSession(pageContext, action), branchWarnings(record, 'delete'), true);
                }
              }}
            >
              {messages.transactions.deleteBranch}
            </Button>
            <Button
              size="small"
              danger
              onClick={() => {
                const action = branchAction(record);
                if (action) {
                  confirmAction(messages.transactions.confirmForceDeleteBranch, () => forceDeleteBranchSession(pageContext, action), branchWarnings(record, 'forceDelete'), true);
                }
              }}
            >
              {messages.transactions.forceDeleteBranch}
            </Button>
            <Button
              size="small"
              onClick={() => {
                const action = branchAction(record);
                if (action) {
                  confirmAction(
                    retryStopped ? messages.transactions.confirmStartBranch : messages.transactions.confirmStopBranch,
                    () => (retryStopped ? startBranchSession(pageContext, action) : stopBranchSession(pageContext, action)),
                    retryStopped ? [] : branchWarnings(record, 'stop')
                  );
                }
              }}
            >
              {retryStopped ? messages.transactions.startBranchRetry : messages.transactions.stopBranchRetry}
            </Button>
          </Space>
        );
      }
    }
  ], [branchModal.xid, pageContext, messages.transactions, query]);

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack transaction-page">
        <div>
          <Typography.Title level={2}>{messages.pages.transactions}</Typography.Title>
          <Space>
            <Typography.Text type="secondary">{messages.transactions.subtitle}</Typography.Text>
            <Button type="link" size="small" onClick={() => navigate('/transaction-group')}>
              {messages.transactions.transactionGroupLink}
            </Button>
          </Space>
        </div>

        {errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}

        <Form<SearchFormValues>
          form={form}
          layout="inline"
          className="transaction-filter"
          initialValues={{ withBranch: false }}
          onFinish={handleSearch}
        >
          <Form.Item name="namespace" label="namespace">
            <Select
              allowClear
              placeholder="namespace"
              options={Array.from(new Set(namespaceNodes.map(n => n.namespace))).map(ns => ({ label: ns, value: ns }))}
              onChange={() => form.setFieldsValue({ cluster: undefined, vgroup: undefined })}
            />
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.namespace !== curr.namespace}
          >
            {() => {
              const ns = form.getFieldValue('namespace');
              const clusters = Array.from(new Set(namespaceNodes.filter(n => n.namespace === ns).map(n => n.cluster)));
              return (
                <Form.Item name="cluster" label="cluster">
                  <Select
                    allowClear
                    placeholder="cluster"
                    options={clusters.map(c => ({ label: c, value: c }))}
                    onChange={() => form.setFieldsValue({ vgroup: undefined })}
                  />
                </Form.Item>
              );
            }}
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.namespace !== curr.namespace || prev.cluster !== curr.cluster}
          >
            {() => {
              const ns = form.getFieldValue('namespace');
              const cluster = form.getFieldValue('cluster');
              const vgroups = namespaceNodes.find(n => n.namespace === ns && n.cluster === cluster)?.vgroups || [];
              return (
                <Form.Item name="vgroup" label="vgroup">
                  <Select
                    allowClear
                    placeholder="vgroup"
                    options={vgroups.map(v => ({ label: v, value: v }))}
                  />
                </Form.Item>
              );
            }}
          </Form.Item>
          <Form.Item name="createTime" label={messages.transactions.createTime}>
            <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm:ss" />
          </Form.Item>
          <Form.Item name="xid" label="xid">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="applicationId" label="applicationId">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="transactionName" label="transactionName">
            <Input allowClear placeholder={messages.transactions.placeholder} />
          </Form.Item>
          <Form.Item name="status" label={messages.transactions.status}>
            <Select
              allowClear
              placeholder={messages.transactions.status}
              options={GLOBAL_STATUS_OPTIONS.map((status) => ({ label: status.label, value: status.value }))}
            />
          </Form.Item>
          <Form.Item name="withBranch" label={messages.transactions.withBranch} valuePropName="checked">
            <Switch />
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

        <Table<GlobalSession>
          className="transaction-table"
          rowKey={(record) => record.xid ?? String(record.transactionId ?? '')}
          columns={globalColumns}
          dataSource={rows}
          loading={loading}
          scroll={{ x: 1890 }}
          locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={messages.transactions.emptyTitle} /> }}
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

        <Modal
          width="min(1180px, calc(100vw - 32px))"
          title={messages.transactions.branchSessionsTitle}
          open={branchModal.open}
          footer={null}
          onCancel={() => setBranchModal({ open: false, rows: [] })}
        >
          <Table<BranchSession>
            rowKey={(record) => String(record.branchId ?? record.transactionId ?? '')}
            columns={branchColumns}
            dataSource={branchModal.rows}
            loading={loading}
            scroll={{ x: 1580 }}
            pagination={false}
            locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={messages.transactions.emptyDescription} /> }}
          />
        </Modal>
      </Space>
    </ConsoleLayout>
  );
}