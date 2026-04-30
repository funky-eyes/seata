import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

export type ConsoleLanguage = 'en-us' | 'zh-cn';

type ConsoleMessages = {
  common: {
    console: string;
    language: string;
    english: string;
    chinese: string;
    signOut: string;
  };
  auth: {
    title: string;
    subtitle: string;
    username: string;
    password: string;
    submit: string;
    usernameRequired: string;
    passwordRequired: string;
    failure: string;
    missingAuthorization: string;
  };
  nav: {
    transactions: string;
    txGroup: string;
    locks: string;
    cluster: string;
    saga: string;
  };
  pages: {
    overview: string;
    transactions: string;
    locks: string;
    cluster: string;
    saga: string;
    namespacePrefix: string;
    runtimeEmpty: string;
    runtimeHint: string;
    clusterEmpty: string;
    clusterHint: string;
    sagaSubtitle: string;
  };
  transactions: {
    subtitle: string;
    transactionGroupLink: string;
    createTime: string;
    placeholder: string;
    status: string;
    withBranch: string;
    search: string;
    reset: string;
    operations: string;
    branchSessions: string;
    branchSessionsTitle: string;
    globalLocks: string;
    deleteGlobal: string;
    forceDeleteGlobal: string;
    stopGlobalRetry: string;
    startGlobalRetry: string;
    sendCommitOrRollback: string;
    changeGlobalStatus: string;
    deleteBranch: string;
    forceDeleteBranch: string;
    stopBranchRetry: string;
    startBranchRetry: string;
    confirmTitle: string;
    warningTitle: string;
    success: string;
    loadFailure: string;
    emptyTitle: string;
    emptyDescription: string;
    confirmDeleteGlobal: string;
    confirmForceDeleteGlobal: string;
    confirmStopGlobal: string;
    confirmStartGlobal: string;
    confirmSend: string;
    confirmChangeStatus: string;
    confirmDeleteBranch: string;
    confirmForceDeleteBranch: string;
    confirmStopBranch: string;
    confirmStartBranch: string;
    warningCommon: string;
    forceDeleteWarning: string;
    deleteBranchAtWarning: string;
    deleteBranchXaWarning: string;
    stopBranchTccWarning: string;
    xidRequired: string;
    branchIdRequired: string;
  };
  locks: {
    subtitle: string;
    delete: string;
    confirmDelete: string;
    warningTitle: string;
    dirtyWriteWarning: string;
    branchAffectedWarning: string;
    deleteSuccess: string;
    deleteFailure: string;
    loadFailure: string;
    emptyTitle: string;
    deleteKeyRequired: string;
  };
  cluster: {
    subtitle: string;
    query: string;
    unitName: string;
    members: string;
    clusterType: string;
    operations: string;
    view: string;
    reload: string;
    unitTitle: string;
    control: string;
    transaction: string;
    internal: string;
    endpoint: string;
    weight: string;
    healthy: string;
    healthyYes: string;
    healthyNo: string;
    term: string;
    role: string;
    unit: string;
    version: string;
    metadata: string;
    viewMetadata: string;
    metadataTitle: string;
    loadFailure: string;
    emptyTitle: string;
    emptyDescription: string;
    contextRequiredTitle: string;
    contextRequiredDescription: string;
  };
  groups: {
    add: string;
    change: string;
    addSubmit: string;
    changeSubmit: string;
    originalNamespace: string;
    originalCluster: string;
    selectVGroup: string;
    targetNamespace: string;
    targetCluster: string;
    targetUnit: string;
    vgroupInput: string;
    unitInput: string;
    clusterInput: string;
    namespaceInput: string;
    search: string;
    edit: string;
    actions: string;
    cancel: string;
    title: string;
    loadFailure: string;
    addFailure: string;
    changeFailure: string;
    addSuccess: string;
    changeSuccess: string;
  };
};

const STORAGE_KEY = 'seata-console-language';

const messages: Record<ConsoleLanguage, ConsoleMessages> = {
  'en-us': {
    common: {
      console: 'Console',
      language: 'Language',
      english: 'English',
      chinese: '简体中文',
      signOut: 'Sign out'
    },
    auth: {
      title: 'Apache Seata Console',
      subtitle: 'Sign in to manage transaction runtime state.',
      username: 'Username',
      password: 'Password',
      submit: 'Sign in',
      usernameRequired: 'Enter a username.',
      passwordRequired: 'Enter a password.',
      failure: 'Sign in failed. Check the account and try again.',
      missingAuthorization: 'Sign in response did not include an authorization value.'
    },
    nav: {
      transactions: 'Global Transactions',
      txGroup: 'Transaction Group Management',
      locks: 'Global Locks',
      cluster: 'Cluster Management',
      saga: 'Saga Designer'
    },
    pages: {
      overview: 'Overview',
      transactions: 'Global Transactions',
      locks: 'Global Locks',
      cluster: 'Cluster Management',
      saga: 'Saga State Machine Designer',
      namespacePrefix: 'Namespace',
      runtimeEmpty: 'No runtime rows loaded',
      runtimeHint: 'Use the context selector before querying server data.',
      clusterEmpty: 'No cluster rows loaded',
      clusterHint: 'Cluster data will appear after a server query completes.',
      sagaSubtitle: 'Visual state machine editor'
    },
    transactions: {
      subtitle: 'Query and operate global transaction sessions in the selected console context.',
      transactionGroupLink: 'Transaction Group',
      createTime: 'Create time',
      placeholder: 'Enter filter criteria',
      status: 'Status',
      withBranch: 'With branch sessions',
      search: 'Search',
      reset: 'Reset',
      operations: 'Operations',
      branchSessions: 'Branch sessions',
      branchSessionsTitle: 'Branch session info',
      globalLocks: 'Global locks',
      deleteGlobal: 'Delete',
      forceDeleteGlobal: 'Force delete',
      stopGlobalRetry: 'Stop retry',
      startGlobalRetry: 'Start retry',
      sendCommitOrRollback: 'Send commit / rollback',
      changeGlobalStatus: 'Change status',
      deleteBranch: 'Delete',
      forceDeleteBranch: 'Force delete',
      stopBranchRetry: 'Stop retry',
      startBranchRetry: 'Start retry',
      confirmTitle: 'Confirm operation',
      warningTitle: 'Risk warning',
      success: 'Operation succeeded.',
      loadFailure: 'Failed to load global transactions.',
      emptyTitle: 'No global transactions found',
      emptyDescription: 'Adjust filters or confirm the selected namespace and cluster.',
      confirmDeleteGlobal: 'Are you sure you want to delete this global transaction?',
      confirmForceDeleteGlobal: 'Are you sure you want to force delete this global transaction?',
      confirmStopGlobal: 'Are you sure you want to stop retry for this global transaction?',
      confirmStartGlobal: 'Are you sure you want to start retry for this global transaction?',
      confirmSend: 'Are you sure you want to send commit or rollback for this global transaction?',
      confirmChangeStatus: 'Are you sure you want to change this global transaction status?',
      confirmDeleteBranch: 'Are you sure you want to delete this branch session?',
      confirmForceDeleteBranch: 'Are you sure you want to force delete this branch session?',
      confirmStopBranch: 'Are you sure you want to stop retry for this branch session?',
      confirmStartBranch: 'Are you sure you want to start retry for this branch session?',
      warningCommon: 'Global transaction commit or rollback inconsistency may occur.',
      forceDeleteWarning: 'Force delete only removes the session from the server.',
      deleteBranchAtWarning: 'The global lock and undo log will also be deleted, so dirty writes may occur.',
      deleteBranchXaWarning: 'The XA branch will roll back.',
      stopBranchTccWarning: 'Check whether this affects the logic of other branches.',
      xidRequired: 'This operation requires xid.',
      branchIdRequired: 'This operation requires branchId.'
    },
    locks: {
      subtitle: 'Query and safely delete global locks in the selected console context.',
      delete: 'Delete',
      confirmDelete: 'Are you sure you want to delete this global lock?',
      warningTitle: 'Risk warning',
      dirtyWriteWarning: 'Dirty writes may occur after this global lock is deleted.',
      branchAffectedWarning: 'Branch transactions may be affected.',
      deleteSuccess: 'Global lock deleted.',
      deleteFailure: 'Failed to delete global lock.',
      loadFailure: 'Failed to load global locks.',
      emptyTitle: 'No global locks found',
      deleteKeyRequired: 'This operation requires xid and branchId.'
    },
    cluster: {
      subtitle: 'View cluster units and naming instances in the selected console context.',
      query: 'Query',
      unitName: 'Unit name',
      members: 'Members',
      clusterType: 'Cluster type',
      operations: 'Operations',
      view: 'View',
      reload: 'Reload',
      unitTitle: 'Unit',
      control: 'Control',
      transaction: 'Transaction',
      internal: 'Internal',
      endpoint: 'Endpoint',
      weight: 'Weight',
      healthy: 'Healthy',
      healthyYes: 'Yes',
      healthyNo: 'No',
      term: 'Term',
      role: 'Role',
      unit: 'Unit',
      version: 'Version',
      metadata: 'Metadata',
      viewMetadata: 'View JSON',
      metadataTitle: 'Metadata',
      loadFailure: 'Failed to load cluster data.',
      emptyTitle: 'No cluster units found',
      emptyDescription: 'Confirm the selected namespace and cluster, then reload cluster data.',
      contextRequiredTitle: 'Namespace and cluster are required',
      contextRequiredDescription: 'Choose a namespace and cluster on this page before viewing cluster data.'
    },
    groups: {
      add: 'Add Transaction Group',
      change: 'Change Transaction Group',
      addSubmit: 'Add',
      changeSubmit: 'Save',
      originalNamespace: 'Original Namespace',
      originalCluster: 'Original Cluster',
      selectVGroup: 'Transaction Group',
      targetNamespace: 'Target Namespace',
      targetCluster: 'Target Cluster',
      targetUnit: 'Target Unit',
      vgroupInput: 'VGroup',
      unitInput: 'Unit',
      clusterInput: 'Cluster',
      namespaceInput: 'Namespace',
      search: 'Search',
      edit: 'Edit',
      actions: 'Actions',
      cancel: 'Cancel',
      title: 'Transaction Group Management',
      loadFailure: 'Failed to load transaction groups.',
      addFailure: 'Failed to add transaction group.',
      changeFailure: 'Failed to change transaction group.',
      addSuccess: 'Transaction group added.',
      changeSuccess: 'Transaction group changed.'
    }
  },
  'zh-cn': {
    common: {
      console: '控制台',
      language: '语言',
      english: 'English',
      chinese: '简体中文',
      signOut: '退出登录'
    },
    auth: {
      title: 'Apache Seata 控制台',
      subtitle: '登录后管理分布式事务运行状态。',
      username: '用户名',
      password: '密码',
      submit: '登录',
      usernameRequired: '请输入用户名。',
      passwordRequired: '请输入密码。',
      failure: '登录失败，请检查账号后重试。',
      missingAuthorization: '登录响应缺少授权信息。'
    },
    nav: {
      transactions: '全局事务',
      txGroup: '事务分组管理',
      locks: '全局锁',
      cluster: '集群管理',
      saga: 'Saga 设计器'
    },
    pages: {
      overview: '概览',
      transactions: '全局事务',
      locks: '全局锁',
      cluster: '集群管理',
      saga: 'Saga 状态机设计器',
      namespacePrefix: '命名空间',
      runtimeEmpty: '尚未加载运行时数据',
      runtimeHint: '发起服务端查询前，请先确认上下文选择器。',
      clusterEmpty: '尚未加载集群数据',
      clusterHint: '服务端查询完成后会展示集群数据。',
      sagaSubtitle: '可视化状态机编辑器'
    },
    transactions: {
      subtitle: '在当前控制台上下文中查询和操作全局事务会话。',
      transactionGroupLink: '事务分组',
      createTime: '创建时间',
      placeholder: '请输入筛选条件',
      status: '状态',
      withBranch: '包含分支事务',
      search: '搜索',
      reset: '重置',
      operations: '操作',
      branchSessions: '分支事务',
      branchSessionsTitle: '分支事务信息',
      globalLocks: '全局锁',
      deleteGlobal: '删除',
      forceDeleteGlobal: '强制删除',
      stopGlobalRetry: '停止重试',
      startGlobalRetry: '开启重试',
      sendCommitOrRollback: '提交 / 回滚',
      changeGlobalStatus: '更新状态',
      deleteBranch: '删除',
      forceDeleteBranch: '强制删除',
      stopBranchRetry: '停止重试',
      startBranchRetry: '开启重试',
      confirmTitle: '确认操作',
      warningTitle: '风险提示',
      success: '操作成功。',
      loadFailure: '加载全局事务失败。',
      emptyTitle: '暂无全局事务',
      emptyDescription: '请调整筛选条件，或确认当前命名空间与集群。',
      confirmDeleteGlobal: '确定要删除该全局事务吗？',
      confirmForceDeleteGlobal: '确定要强制删除该全局事务吗？',
      confirmStopGlobal: '确定要停止该全局事务重试吗？',
      confirmStartGlobal: '确定要开启该全局事务重试吗？',
      confirmSend: '确定要向该全局事务发送提交或回滚吗？',
      confirmChangeStatus: '确定要更新该全局事务状态吗？',
      confirmDeleteBranch: '确定要删除该分支事务吗？',
      confirmForceDeleteBranch: '确定要强制删除该分支事务吗？',
      confirmStopBranch: '确定要停止该分支事务重试吗？',
      confirmStartBranch: '确定要开启该分支事务重试吗？',
      warningCommon: '全局事务提交或回滚可能出现不一致风险。',
      forceDeleteWarning: '强制删除只会删除服务端会话。',
      deleteBranchAtWarning: '全局锁和 undo log 也会被删除，可能出现脏写风险。',
      deleteBranchXaWarning: 'XA 分支将会回滚。',
      stopBranchTccWarning: '请确认是否会影响其他分支的业务逻辑。',
      xidRequired: '该操作需要 xid。',
      branchIdRequired: '该操作需要 branchId。'
    },
    locks: {
      subtitle: '在当前控制台上下文中查询并安全删除全局锁。',
      delete: '删除',
      confirmDelete: '确定要删除该全局锁吗？',
      warningTitle: '风险提示',
      dirtyWriteWarning: '删除该全局锁后可能出现脏写风险。',
      branchAffectedWarning: '分支事务可能受到影响。',
      deleteSuccess: '全局锁已删除。',
      deleteFailure: '删除全局锁失败。',
      loadFailure: '加载全局锁失败。',
      emptyTitle: '暂无全局锁',
      deleteKeyRequired: '该操作需要 xid 和 branchId。'
    },
    cluster: {
      subtitle: '在当前控制台上下文中查看集群单元和 Naming 实例。',
      query: '查询',
      unitName: 'Unit 名称',
      members: '成员数',
      clusterType: '集群类型',
      operations: '操作',
      view: '查看',
      reload: '重新加载',
      unitTitle: 'Unit',
      control: 'Control',
      transaction: 'Transaction',
      internal: 'Internal',
      endpoint: 'Endpoint',
      weight: '权重',
      healthy: '健康状态',
      healthyYes: '是',
      healthyNo: '否',
      term: 'Term',
      role: '角色',
      unit: 'Unit',
      version: '版本',
      metadata: 'Metadata',
      viewMetadata: '查看 JSON',
      metadataTitle: 'Metadata',
      loadFailure: '加载集群数据失败。',
      emptyTitle: '暂无集群 Unit',
      emptyDescription: '请确认当前命名空间和集群后重新加载集群数据。',
      contextRequiredTitle: '需要命名空间和集群',
      contextRequiredDescription: '请先在当前页面选择命名空间和集群，再查看集群数据。'
    },
    groups: {
      add: '添加事务分组',
      change: '修改事务分组',
      addSubmit: '提交',
      changeSubmit: '保存',
      originalNamespace: '原命名空间',
      originalCluster: '原集群',
      selectVGroup: '事务分组',
      targetNamespace: '目标命名空间',
      targetCluster: '目标集群',
      targetUnit: '目标单元',
      vgroupInput: '分组名称 (vGroup)',
      unitInput: '单元名称 (Unit)',
      clusterInput: '集群名称 (Cluster)',
      namespaceInput: '命名空间 (Namespace)',
      search: '搜索',
      edit: '编辑',
      actions: '操作',
      cancel: '取消',
      title: '事务分组管理',
      loadFailure: '加载事务分组失败。',
      addFailure: '添加事务分组失败。',
      changeFailure: '修改事务分组失败。',
      addSuccess: '事务分组添加成功。',
      changeSuccess: '事务分组修改成功。'
    }
  }
};

type I18nContextValue = {
  language: ConsoleLanguage;
  setLanguage: (language: ConsoleLanguage) => void;
  messages: ConsoleMessages;
};

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

function initialLanguage(): ConsoleLanguage {
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === 'zh-cn' ? 'zh-cn' : 'en-us';
}

export function ConsoleI18nProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<ConsoleLanguage>(initialLanguage);
  const value = useMemo<I18nContextValue>(() => {
    function setLanguage(nextLanguage: ConsoleLanguage) {
      window.localStorage.setItem(STORAGE_KEY, nextLanguage);
      setLanguageState(nextLanguage);
    }

    return { language, setLanguage, messages: messages[language] };
  }, [language]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useConsoleI18n() {
  const value = useContext(I18nContext);
  if (!value) {
    throw new Error('Console i18n context is missing.');
  }
  return value;
}