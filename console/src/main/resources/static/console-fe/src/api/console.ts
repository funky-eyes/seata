import { buildUrl, requestJson, RequestError } from './request';
import type {
  ApiResponse,
  BranchSessionAction,
  ConsoleRequestContext,
  GlobalLock,
  GlobalLockListPayload,
  GlobalLockQuery,
  GlobalSession,
  GlobalSessionListPayload,
  GlobalSessionQuery,
  QueryParams
} from './types';

const CONSOLE_API_PREFIX = '/api/v1/console';

function normalize(value?: string) {
  return value?.trim() ?? '';
}

export function assertConsoleContext(context: ConsoleRequestContext) {
  const namespace = normalize(context.namespace);
  const cluster = normalize(context.cluster);
  const vgroup = normalize(context.vgroup);
  const mode = context.mode ?? (vgroup ? 'vgroup' : 'cluster');
  if (!namespace) {
    throw new RequestError('Namespace is required before sending a console request.');
  }
  if (mode === 'cluster' && !cluster) {
    throw new RequestError('Cluster is required before sending a cluster-routed console request.');
  }
  if (mode === 'vgroup' && !vgroup) {
    throw new RequestError('VGroup is required before sending a vgroup-routed console request.');
  }
}

function withConsoleContext(context: ConsoleRequestContext, query?: QueryParams) {
  assertConsoleContext(context);
  const namespace = normalize(context.namespace);
  const cluster = normalize(context.cluster);
  const vgroup = normalize(context.vgroup);
  const headers: Record<string, string> = {
    'x-seata-namespace': namespace
  };
  if (cluster) {
    headers['x-seata-cluster'] = cluster;
  }
  const nextQuery = { ...query };
  if (vgroup) {
    nextQuery.vgroup = vgroup;
  } else {
    delete nextQuery.vgroup;
  }
  return {
    headers,
    query: nextQuery
  };
}

function consoleRequest<T>(path: string, context: ConsoleRequestContext, method: string, query?: QueryParams) {
  const request = withConsoleContext(context, query);
  return requestJson<ApiResponse<T>>(buildUrl(`${CONSOLE_API_PREFIX}${path}`, request.query), {
    method,
    headers: request.headers
  });
}

export function queryGlobalSessions(context: ConsoleRequestContext, query: GlobalSessionQuery) {
  return consoleRequest<GlobalSessionListPayload | GlobalSession[]>('/globalSession/query', context, 'GET', query);
}

export function deleteGlobalSession(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/deleteGlobalSession', context, 'DELETE', { xid });
}

export function forceDeleteGlobalSession(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/forceDeleteGlobalSession', context, 'DELETE', { xid });
}

export function stopGlobalSession(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/stopGlobalSession', context, 'PUT', { xid });
}

export function startGlobalSession(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/startGlobalSession', context, 'PUT', { xid });
}

export function sendCommitOrRollback(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/sendCommitOrRollback', context, 'PUT', { xid });
}

export function changeGlobalStatus(context: ConsoleRequestContext, xid: string) {
  return consoleRequest('/globalSession/changeGlobalStatus', context, 'PUT', { xid });
}

export function deleteBranchSession(context: ConsoleRequestContext, action: BranchSessionAction) {
  return consoleRequest('/branchSession/deleteBranchSession', context, 'DELETE', action);
}

export function forceDeleteBranchSession(context: ConsoleRequestContext, action: BranchSessionAction) {
  return consoleRequest('/branchSession/forceDeleteBranchSession', context, 'DELETE', action);
}

export function stopBranchSession(context: ConsoleRequestContext, action: BranchSessionAction) {
  return consoleRequest('/branchSession/stopBranchSession', context, 'PUT', action);
}

export function startBranchSession(context: ConsoleRequestContext, action: BranchSessionAction) {
  return consoleRequest('/branchSession/startBranchSession', context, 'PUT', action);
}

export function queryGlobalLocks(context: ConsoleRequestContext, query: GlobalLockQuery) {
  return consoleRequest<GlobalLockListPayload | GlobalLock[]>('/globalLock/query', context, 'GET', query);
}

export function deleteGlobalLock(context: ConsoleRequestContext, query: QueryParams) {
  return consoleRequest('/globalLock/delete', context, 'DELETE', query);
}

export function checkGlobalLock(context: ConsoleRequestContext, query: Pick<GlobalLockQuery, 'xid' | 'branchId'>) {
  return consoleRequest<boolean | { data?: boolean }>('/globalLock/check', context, 'GET', query);
}