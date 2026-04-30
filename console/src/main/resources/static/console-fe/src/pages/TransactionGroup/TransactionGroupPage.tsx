import { useEffect, useMemo, useState } from 'react';

import { Alert, App, Button, Card, Col, Form, Input, Modal, Row, Select, Space, Table, Tag, Typography } from 'antd';

import { addGroup, changeGroup, fetchNamespaces } from '@/api/naming';
import type { AddGroupPayload, ChangeGroupPayload } from '@/api/types';
import { useConsoleI18n } from '@/i18n';
import { ConsoleLayout } from '@/layout/ConsoleLayout';
import { buildNamespaceNodes, type NamespaceNode } from '@/utils/namespaceHelper';

type RowData = {
  namespace: string;
  cluster: string;
  vGroup: string;
  type?: string;
  units: string[];
};

function resolveErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback;
}

function resolveInitialTargetUnit(record: RowData) {
  if (!record.type || record.type === 'default') {
    return undefined;
  }
  return record.units.length === 1 ? record.units[0] : undefined;
}

export function TransactionGroupPage() {
  const { message } = App.useApp();
  const { messages } = useConsoleI18n();
  const [data, setData] = useState<NamespaceNode[]>([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>();

  const [filterNamespace, setFilterNamespace] = useState<string>();
  const [filterCluster, setFilterCluster] = useState<string>();
  const [filterVGroup, setFilterVGroup] = useState<string>();

  const [appliedFilters, setAppliedFilters] = useState({
    namespace: '',
    cluster: '',
    vGroup: ''
  });

  const [isAddModalVisible, setIsAddModalVisible] = useState(false);
  const [isChangeModalVisible, setIsChangeModalVisible] = useState(false);

  const [addForm] = Form.useForm();
  const [changeForm] = Form.useForm();

  const addSelectedNamespace = Form.useWatch('namespace', addForm);
  const changeOriginalNamespace = Form.useWatch('originalNamespace', changeForm);
  const changeOriginalCluster = Form.useWatch('originalCluster', changeForm);
  const changeTargetNamespace = Form.useWatch('namespace', changeForm);

  const namespaceOptions = Array.from(new Set(data.map(d => d.namespace))).map(n => ({ label: n, value: n }));

  const addSelectedCluster = Form.useWatch('clusterName', addForm);
  const changeTargetCluster = Form.useWatch('clusterName', changeForm);

  const addClusterOptions = Array.from(
    new Set(data.filter(d => !addSelectedNamespace || d.namespace === addSelectedNamespace).map(d => d.cluster))
  ).map(c => ({ label: c, value: c }));

  const changeOriginalClusterOptions = Array.from(
    new Set(data.filter(d => !changeOriginalNamespace || d.namespace === changeOriginalNamespace).map(d => d.cluster))
  ).map(c => ({ label: c, value: c }));

  const changeTargetClusterOptions = Array.from(
    new Set(data.filter(d => !changeTargetNamespace || d.namespace === changeTargetNamespace).map(d => d.cluster))
  ).map(c => ({ label: c, value: c }));

  const addClusterNode = data.find(d => d.namespace === addSelectedNamespace && d.cluster === addSelectedCluster);
  const changeOriginalNode = data.find(d => d.namespace === changeOriginalNamespace && d.cluster === changeOriginalCluster);
  const changeTargetNode = data.find(d => d.namespace === changeTargetNamespace && d.cluster === changeTargetCluster);
  const addNeedUnit = addClusterNode?.type && addClusterNode.type !== 'default';
  const changeNeedUnit = changeTargetNode?.type && changeTargetNode.type !== 'default';
  const changeVGroupOptions = (changeOriginalNode?.vgroups ?? []).map(vGroup => ({ label: vGroup, value: vGroup }));

  const rowData: RowData[] = useMemo(() => {
    const list: RowData[] = [];
    for (const node of data) {
      if (node.vgroups && node.vgroups.length > 0) {
        for (const vg of node.vgroups) {
          list.push({
            namespace: node.namespace,
            cluster: node.cluster,
            type: node.type,
            vGroup: vg,
            units: node.units || []
          });
        }
      }
    }
    return list;
  }, [data]);

  const filteredData = useMemo(() => {
    return rowData.filter(d => {
      if (appliedFilters.namespace && !d.namespace.includes(appliedFilters.namespace)) return false;
      if (appliedFilters.cluster && !d.cluster.includes(appliedFilters.cluster)) return false;
      if (appliedFilters.vGroup && !d.vGroup.includes(appliedFilters.vGroup)) return false;
      return true;
    });
  }, [rowData, appliedFilters]);

  const handleSearch = () => {
    setAppliedFilters({
      namespace: filterNamespace || '',
      cluster: filterCluster || '',
      vGroup: filterVGroup || ''
    });
  };

  async function loadData() {
    setLoading(true);
    setErrorMessage(undefined);
    try {
      const res = await fetchNamespaces();
      if (res.success) {
        setData(buildNamespaceNodes(res.data));
      }
    } catch (error) {
      setErrorMessage(resolveErrorMessage(error, messages.groups.loadFailure));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleAdd(values: AddGroupPayload) {
    try {
      setErrorMessage(undefined);
      await addGroup(values);
      message.success(messages.groups.addSuccess);
      addForm.resetFields();
      setIsAddModalVisible(false);
      await loadData();
    } catch (error) {
      const description = resolveErrorMessage(error, messages.groups.addFailure);
      setErrorMessage(description);
      message.error(description);
    }
  }

  async function handleChange(values: ChangeGroupPayload) {
    try {
      setErrorMessage(undefined);
      const payload = { ...values };
      await changeGroup(payload);
      message.success(messages.groups.changeSuccess);
      changeForm.resetFields();
      setIsChangeModalVisible(false);
      await loadData();
    } catch (error) {
      const description = resolveErrorMessage(error, messages.groups.changeFailure);
      setErrorMessage(description);
      message.error(description);
    }
  }

  function openEditModal(record: RowData) {
    changeForm.setFieldsValue({
      originalNamespace: record.namespace,
      originalCluster: record.cluster,
      clusterName: record.cluster,
      vGroup: record.vGroup,
      namespace: record.namespace,
      unitName: resolveInitialTargetUnit(record)
    });
    setErrorMessage(undefined);
    setIsChangeModalVisible(true);
  }

  const columns = [
    {
      title: messages.groups.namespaceInput,
      dataIndex: 'namespace',
      key: 'namespace'
    },
    {
      title: messages.groups.clusterInput,
      dataIndex: 'cluster',
      key: 'cluster'
    },
    {
      title: messages.groups.vgroupInput,
      dataIndex: 'vGroup',
      key: 'vGroup',
      render: (val: string) => <Tag color="blue">{val}</Tag>
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (type?: string) => type ? <Tag>{type}</Tag> : <Tag>default</Tag>
    },
    {
      title: messages.groups.unitInput,
      dataIndex: 'units',
      key: 'units',
      render: (units: string[]) => (
        <Space wrap size={[0, 4]}>
          {units.map(u => <Tag color="green" key={u}>{u}</Tag>)}
        </Space>
      )
    },
    {
      title: messages.groups.actions,
      key: 'actions',
      render: (_: any, record: RowData) => (
        <Button type="link" onClick={() => openEditModal(record)}>
          {messages.groups.edit}
        </Button>
      )
    }
  ];

  return (
    <ConsoleLayout>
      <Space direction="vertical" size={16} className="page-stack" style={{ width: '100%' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {messages.groups.title}
        </Typography.Title>
        {errorMessage ? <Alert type="error" showIcon message={errorMessage} /> : null}
        <Card variant="borderless">
          <Row gutter={[16, 16]} align="middle">
            <Col>
              <Input
                placeholder={messages.groups.namespaceInput}
                value={filterNamespace}
                onChange={e => setFilterNamespace(e.target.value)}
                allowClear
              />
            </Col>
            <Col>
              <Input
                placeholder={messages.groups.clusterInput}
                value={filterCluster}
                onChange={e => setFilterCluster(e.target.value)}
                allowClear
              />
            </Col>
            <Col>
              <Input
                placeholder={messages.groups.vgroupInput}
                value={filterVGroup}
                onChange={e => setFilterVGroup(e.target.value)}
                allowClear
              />
            </Col>
            <Col flex="auto" style={{ textAlign: 'right' }}>
              <Space>
                <Button type="primary" onClick={handleSearch}>
                  {messages.groups.search}
                </Button>
                <Button onClick={() => { addForm.resetFields(); setIsAddModalVisible(true); }}>
                  {messages.groups.add}
                </Button>
              </Space>
            </Col>
          </Row>
        </Card>
        <Card variant="borderless">
          <Table
            rowKey={(r) => `${r.namespace}-${r.cluster}-${r.vGroup}`}
            columns={columns}
            dataSource={filteredData}
            loading={loading}
          />
        </Card>
      </Space>

      <Modal
        title={messages.groups.add}
        open={isAddModalVisible}
        onCancel={() => setIsAddModalVisible(false)}
        footer={null}
        destroyOnHidden
      >
        <Form form={addForm} layout="vertical" onFinish={handleAdd}>
          <Form.Item label={messages.groups.namespaceInput} name="namespace" rules={[{ required: true }]}>
            <Select options={namespaceOptions} />
          </Form.Item>
          <Form.Item label={messages.groups.clusterInput} name="clusterName" rules={[{ required: true }]}>
            <Select options={addClusterOptions} />
          </Form.Item>
          {addNeedUnit && (
            <Form.Item label={messages.groups.unitInput} name="unitName" rules={[{ required: true }]}>
              <Select options={addClusterNode?.units?.map(u => ({ label: u, value: u })) || []} />
            </Form.Item>
          )}
          <Form.Item label={messages.groups.vgroupInput} name="vGroup" rules={[{ required: true }]}>
            <Input placeholder={messages.groups.vgroupInput} />
          </Form.Item>
          <Form.Item style={{ textAlign: 'right', marginBottom: 0 }}>
            <Space>
              <Button onClick={() => setIsAddModalVisible(false)}>{messages.groups.cancel}</Button>
              <Button type="primary" htmlType="submit">
                {messages.groups.addSubmit}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={messages.groups.change}
        open={isChangeModalVisible}
        onCancel={() => {
          setIsChangeModalVisible(false);
          changeForm.resetFields();
        }}
        footer={null}
        destroyOnHidden
      >
        <Form form={changeForm} layout="vertical" onFinish={handleChange}>
          <Form.Item label={messages.groups.originalNamespace} name="originalNamespace" rules={[{ required: true }]}>
            <Select
              options={namespaceOptions}
              onChange={() => changeForm.setFieldsValue({ originalCluster: undefined, vGroup: undefined })}
            />
          </Form.Item>
          <Form.Item label={messages.groups.originalCluster} name="originalCluster" rules={[{ required: true }]}>
            <Select
              options={changeOriginalClusterOptions}
              onChange={() => changeForm.setFieldsValue({ vGroup: undefined })}
            />
          </Form.Item>
          <Form.Item label={messages.groups.selectVGroup} name="vGroup" rules={[{ required: true }]}> 
            <Select options={changeVGroupOptions} />
          </Form.Item>
          <Form.Item label={messages.groups.targetNamespace} name="namespace" rules={[{ required: true }]}>
            <Select
              options={namespaceOptions}
              onChange={() => changeForm.setFieldsValue({ clusterName: undefined, unitName: undefined })}
            />
          </Form.Item>
          <Form.Item label={messages.groups.targetCluster} name="clusterName" rules={[{ required: true }]}>
            <Select
              options={changeTargetClusterOptions}
              onChange={() => changeForm.setFieldsValue({ unitName: undefined })}
            />
          </Form.Item>
          {changeNeedUnit && (
            <Form.Item label={messages.groups.targetUnit} name="unitName" rules={[{ required: true }]}>
              <Select options={changeTargetNode?.units?.map(unit => ({ label: unit, value: unit })) || []} />
            </Form.Item>
          )}
          <Form.Item style={{ textAlign: 'right', marginBottom: 0 }}>
            <Space>
              <Button
                onClick={() => {
                  setIsChangeModalVisible(false);
                  changeForm.resetFields();
                }}
              >
                {messages.groups.cancel}
              </Button>
              <Button type="primary" htmlType="submit">
                {messages.groups.changeSubmit}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </ConsoleLayout>
  );
}
