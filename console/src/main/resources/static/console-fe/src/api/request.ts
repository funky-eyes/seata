import type { MessageInstance } from 'antd/es/message/interface';

import type { QueryParams, QueryValue } from './types';

const AUTHORIZATION_STORAGE_KEY = 'Authorization';
const REQUEST_TIMEOUT_MS = 30000;

let messageApi: MessageInstance | undefined;

export class RequestError extends Error {
  readonly status?: number;
  readonly payload?: unknown;

  constructor(message: string, status?: number, payload?: unknown) {
    super(message);
    this.name = 'RequestError';
    this.status = status;
    this.payload = payload;
  }
}

export type RequestOptions = {
  method?: string;
  headers?: HeadersInit;
  query?: QueryParams;
  body?: unknown;
  skipAuthorization?: boolean;
  signal?: AbortSignal;
};

export function configureRequestFeedback(instance: MessageInstance) {
  messageApi = instance;
}

export function getAuthorization() {
  return window.localStorage.getItem(AUTHORIZATION_STORAGE_KEY) ?? '';
}

export function setAuthorization(value: string) {
  window.localStorage.setItem(AUTHORIZATION_STORAGE_KEY, value);
}

export function clearAuthorization() {
  window.localStorage.removeItem(AUTHORIZATION_STORAGE_KEY);
}

function appendQueryValue(params: URLSearchParams, key: string, value: QueryValue) {
  if (value === undefined || value === null || value === '') {
    return;
  }
  params.set(key, String(value));
}

export function buildUrl(pathname: string, query?: QueryParams) {
  const params = new URLSearchParams();
  Object.entries(query ?? {}).forEach(([key, value]) => appendQueryValue(params, key, value));
  const queryString = params.toString();
  return queryString ? `${pathname}?${queryString}` : pathname;
}

function parseErrorMessage(payload: unknown, fallback: string) {
  if (payload && typeof payload === 'object') {
    const candidate = payload as { message?: unknown; msg?: unknown; error?: unknown };
    for (const value of [candidate.message, candidate.msg, candidate.error]) {
      if (typeof value === 'string' && value.trim()) {
        return value;
      }
    }
  }
  return fallback;
}

function isBusinessCode(value: unknown): value is number | string {
  return typeof value === 'number' || (typeof value === 'string' && /^-?\d+$/.test(value));
}

function isBusinessEnvelope(payload: unknown): payload is { code?: number | string; success?: boolean } {
  if (!payload || typeof payload !== 'object') {
    return false;
  }
  const candidate = payload as { code?: unknown; success?: unknown };
  return isBusinessCode(candidate.code) || typeof candidate.success === 'boolean';
}

function getBusinessErrorMessage(payload: unknown) {
  if (!isBusinessEnvelope(payload)) {
    return undefined;
  }
  if (payload.code !== undefined && String(payload.code) !== '200') {
    return parseErrorMessage(payload, `Request failed with business code ${payload.code}.`);
  }
  if (payload.success === false) {
    return parseErrorMessage(payload, 'Request failed.');
  }
  return undefined;
}

async function readPayload(response: Response) {
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return response.json();
  }
  const text = await response.text();
  return text ? { message: text } : undefined;
}

function redirectToLogin(status: number) {
  if (status !== 401 && status !== 403) {
    return;
  }
  clearAuthorization();
  if (window.location.hash !== '#/login') {
    window.location.hash = '#/login';
  }
}

export async function requestJson<T>(pathname: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const headers = new Headers(options.headers);
  const method = options.method ?? (options.body === undefined ? 'GET' : 'POST');
  const authorization = getAuthorization();

  if (authorization && !options.skipAuthorization) {
    headers.set(AUTHORIZATION_STORAGE_KEY, authorization);
  }
  if (options.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  try {
    const response = await fetch(buildUrl(pathname, options.query), {
      method,
      headers,
      body: options.body === undefined ? undefined : typeof options.body === 'string' ? options.body : JSON.stringify(options.body),
      signal: options.signal ?? controller.signal,
      credentials: 'same-origin'
    });
    const payload = await readPayload(response);

    if (!response.ok) {
      redirectToLogin(response.status);
      const message = parseErrorMessage(payload, `Request failed with status ${response.status}.`);
      messageApi?.error(message);
      throw new RequestError(message, response.status, payload);
    }

    const businessErrorMessage = getBusinessErrorMessage(payload);
    if (businessErrorMessage) {
      messageApi?.error(businessErrorMessage);
      throw new RequestError(businessErrorMessage, response.status, payload);
    }

    return payload as T;
  } catch (error) {
    if (error instanceof RequestError) {
      throw error;
    }
    const message = error instanceof Error && error.name === 'AbortError' ? 'Request timed out.' : 'Network request failed.';
    messageApi?.error(message);
    throw new RequestError(message, undefined, error);
  } finally {
    window.clearTimeout(timeout);
  }
}