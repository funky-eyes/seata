import { clearAuthorization, requestJson, RequestError, setAuthorization } from './request';
import type { ApiResponse, LoginPayload } from './types';

export async function login(payload: LoginPayload) {
  const response = await requestJson<ApiResponse<string>>('/api/v1/auth/login', {
    method: 'POST',
    body: payload,
    skipAuthorization: true
  });
  const authorization = response.data;
  if (!authorization) {
    throw new RequestError('Sign in response did not include an authorization value.');
  }
  setAuthorization(authorization);
  return authorization;
}

export function logout() {
  clearAuthorization();
  window.location.hash = '#/login';
}