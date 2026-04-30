import { requestJson } from './request';
import type { AddGroupPayload, ApiResponse, ChangeGroupPayload, ClusterData } from './types';

const NAMING_API_PREFIX = '/api/v1/naming';

function formBody(payload: Record<string, string | undefined>) {
  const params = new URLSearchParams();
  Object.entries(payload).forEach(([key, value]) => {
    if (value !== undefined) {
      params.set(key, value);
    }
  });
  return params.toString();
}

export function fetchNamespaces() {
  return requestJson<ApiResponse<unknown>>(`${NAMING_API_PREFIX}/namespace`);
}

export function fetchClusterData(namespace: string, clusterName: string) {
  return requestJson<ApiResponse<ClusterData>>(`${NAMING_API_PREFIX}/clusterData`, {
    query: { namespace, clusterName }
  });
}

export function addGroup(payload: AddGroupPayload) {
  return requestJson<ApiResponse<unknown>>(`${NAMING_API_PREFIX}/addGroup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formBody(payload)
  });
}

export function changeGroup(payload: ChangeGroupPayload) {
  return requestJson<ApiResponse<unknown>>(`${NAMING_API_PREFIX}/changeGroup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: formBody(payload)
  });
}