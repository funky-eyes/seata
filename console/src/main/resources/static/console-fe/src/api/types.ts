export type ApiResponse<T> = {
  code?: number;
  success?: boolean;
  message?: string;
  data: T;
};

export type QueryValue = string | number | boolean | null | undefined;

export type QueryParams = Record<string, QueryValue>;

export type ConsoleRoutingMode = 'cluster' | 'vgroup';

export type ConsoleRequestContext = {
  namespace: string;
  cluster?: string;
  vgroup?: string;
  mode?: ConsoleRoutingMode;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type PaginationQuery = {
  pageNum: number;
  pageSize: number;
};

export type GlobalSessionQuery = PaginationQuery & {
  xid?: string;
  applicationId?: string;
  status?: number;
  transactionName?: string;
  withBranch?: boolean;
  timeStart?: number;
  timeEnd?: number;
};

export type BranchSessionAction = {
  xid?: string;
  branchId?: string;
};

export type BranchSession = {
  xid?: string;
  transactionId?: string | number;
  branchId?: string | number;
  resourceGroupId?: string;
  branchType?: string;
  status?: number;
  resourceId?: string;
  clientId?: string;
  applicationData?: string;
};

export type GlobalSession = {
  xid?: string;
  transactionId?: string | number;
  applicationId?: string;
  transactionServiceGroup?: string;
  transactionName?: string;
  status?: number;
  timeout?: number;
  beginTime?: string | number | null;
  applicationData?: string;
  branchSessionVOs?: BranchSession[];
};

export type GlobalSessionListPayload = {
  data?: GlobalSession[];
  list?: GlobalSession[];
  total?: number;
  totalCount?: number;
  count?: number;
};

export type GlobalLockQuery = PaginationQuery & {
  xid?: string;
  tableName?: string;
  transactionId?: string;
  branchId?: string;
  pk?: string;
  resourceId?: string;
  timeStart?: number;
  timeEnd?: number;
};

export type GlobalLock = {
  xid?: string;
  transactionId?: string | number;
  branchId?: string | number;
  resourceId?: string;
  tableName?: string;
  pk?: string;
  rowKey?: string;
  gmtCreate?: string | number | null;
  gmtModified?: string | number | null;
};

export type GlobalLockListPayload = {
  data?: GlobalLock[];
  list?: GlobalLock[];
  total?: number;
  totalCount?: number;
  count?: number;
};

export type NamingEndpoint = {
  host?: string;
  port?: string | number;
};

export type NamingInstance = {
  control?: NamingEndpoint | string;
  transaction?: NamingEndpoint | string;
  internal?: NamingEndpoint | string;
  weight?: string | number;
  healthy?: boolean;
  term?: string | number;
  role?: string;
  unit?: string;
  version?: string;
  metadata?: unknown;
};

export type ClusterUnit = {
  namingInstanceList?: NamingInstance[];
};

export type ClusterData = {
  clusterType?: string;
  unitData?: Record<string, ClusterUnit>;
};

export type AddGroupPayload = {
  namespace: string;
  clusterName: string;
  vGroup: string;
  unitName?: string;
};

export type ChangeGroupPayload = {
  originalNamespace: string;
  originalCluster: string;
  namespace: string;
  clusterName: string;
  vGroup: string;
  unitName?: string;
};