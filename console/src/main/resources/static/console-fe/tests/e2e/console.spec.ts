import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { expect, test as base, type Page, type Request, type Route } from '@playwright/test';

const AUTHORIZATION_STORAGE_KEY = 'Authorization';
const LANGUAGE_STORAGE_KEY = 'seata-console-language';
const MOCK_TOKEN = 'mock-token';
const TEST_FILE_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(TEST_FILE_DIR, '../../../../../../../../');

type ApiCall = {
  method: string;
  path: string;
  url: string;
  headers: Record<string, string>;
  query: Record<string, string>;
  body?: string;
};

type ApiCalls = {
  auth: ApiCall[];
  console: ApiCall[];
  naming: ApiCall[];
  unexpected: ApiCall[];
};

type ConsoleFixtures = {
  apiCalls: ApiCalls;
  browserErrors: string[];
};

type MockClusterDefinition = {
  type: string;
  vgroups: string[];
  units: string[];
};

type MockNamespaceTree = Record<string, { clusters: Record<string, MockClusterDefinition> }>;

type MockState = {
  namespaces: MockNamespaceTree;
};

const test = base.extend<ConsoleFixtures>({
  apiCalls: [async ({ page }, use) => {
    const apiCalls: ApiCalls = { auth: [], console: [], naming: [], unexpected: [] };
    const mockState = createMockState();

    await page.addInitScript(({ authorizationKey, languageKey, token }) => {
      window.localStorage.setItem(authorizationKey, token);
      window.localStorage.setItem(languageKey, 'en-us');
    }, { authorizationKey: AUTHORIZATION_STORAGE_KEY, languageKey: LANGUAGE_STORAGE_KEY, token: MOCK_TOKEN });

    await page.route('**/api/v1/**', async (route) => handleApiRoute(route, apiCalls, mockState));
    await page.route('**/saga-statemachine-designer/designer.html', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<!doctype html><html lang="en"><title>Saga Designer</title><body>Saga Designer Mock</body></html>'
      });
    });

    await use(apiCalls);
  }, { auto: true }],
  browserErrors: [async ({ page }, use) => {
    const browserErrors: string[] = [];
    page.on('pageerror', (error) => browserErrors.push(error.message));
    page.on('console', (message) => {
      if (message.type() === 'error') {
        browserErrors.push(message.text());
      }
    });
    await use(browserErrors);
  }, { auto: true }]
});

test.afterEach(async ({ apiCalls, browserErrors }) => {
  expect(browserErrors).toEqual([]);
  expect(apiCalls.unexpected).toEqual([]);
});

test('登录成功写入 Authorization 并跳转到全局事务页', async ({ page, apiCalls }) => {
  await page.goto('/#/login');
  await page.evaluate((authorizationKey) => window.localStorage.removeItem(authorizationKey), AUTHORIZATION_STORAGE_KEY);

  await expect(page.getByRole('heading', { name: 'Apache Seata Console' })).toBeVisible();
  await page.getByLabel('Username').fill('seata');
  await page.getByLabel('Password').fill('seata');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/#\/transaction\/list$/);
  await expect(page.getByRole('heading', { name: 'Global Transactions' })).toBeVisible();
  await expect(page.getByText('xid-001')).toBeVisible();

  const header = page.locator('header');
  await expect(header).toBeVisible();
  await expect(header.getByText('Namespace:')).not.toBeVisible();
  await expect(header.getByText('Cluster:')).not.toBeVisible();

  await expect(page.getByText('EN', { exact: true })).toBeVisible();
  
  await expect(page.getByLabel('namespace')).toBeVisible();
  await expect(page.getByLabel('cluster')).toBeVisible();
  await expect(page.getByLabel('vgroup')).toBeVisible();

  await expectStoredAuthorization(page, MOCK_TOKEN);
  await expectTokenNotExposed(page, MOCK_TOKEN);
  expect(apiCalls.auth).toHaveLength(1);
  expect(apiCalls.auth[0].method).toBe('POST');
});

test('旧 hash 路由可打开非空页面并保持 Saga iframe 指向设计器资源', async ({ page }) => {
  const routes = [
    { path: '/transaction/list', heading: 'Global Transactions', content: 'xid-001' },
    { path: '/transaction-group', heading: 'Transaction Group Management' },
    { path: '/globallock/list', heading: 'Global Locks' },
    { path: '/cluster/list', heading: 'Cluster Management' },
    { path: '/sagastatemachinedesigner', heading: 'Saga State Machine Designer', content: 'Visual state machine editor' }
  ];

  for (const route of routes) {
    await page.goto(`/#${route.path}`);
    await expect(page.getByRole('heading', { name: route.heading })).toBeVisible();
    if (route.content) {
      await expect(page.getByText(route.content)).toBeVisible();
    }
  }

  const sagaFrame = page.getByTestId('saga-designer-frame');
  await expect(sagaFrame).toBeVisible();
  await expect(sagaFrame).toHaveAttribute('src', /saga-statemachine-designer\/designer\.html$/);
});

test('vgroup 场景下 console 请求仍保留 cluster header 且 query 可带 vgroup', async ({ page, apiCalls }) => {
  await page.goto('/#/transaction/list');
  await expect(page.getByText('xid-001')).toBeVisible();

  await page.getByLabel('vgroup').click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Search' }).click();

  await expect(page.getByText('xid-001')).toBeVisible();

  const sessionCalls = apiCalls.console.filter((call) => call.path === '/api/v1/console/globalSession/query');
  expect(sessionCalls.length).toBeGreaterThanOrEqual(2);
  const lastCall = sessionCalls.at(-1);
  expect(lastCall?.headers['x-seata-namespace']).toBe('default');
  expect(lastCall?.headers['x-seata-cluster']).toBe('default-cluster');
  expect(lastCall?.query.vgroup).toBe('default_tx_group');
});

test('GlobalLocks 直接访问时不会因空 context 自动报错，手动搜索后才请求', async ({ page, apiCalls }) => {
  await page.goto('/#/globallock/list');
  await expect(page.getByRole('heading', { name: 'Global Locks' })).toBeVisible();
  await page.waitForLoadState('networkidle');

  expect(apiCalls.console.filter((call) => call.path === '/api/v1/console/globalLock/query')).toHaveLength(0);
  await expect(page.getByRole('alert')).toHaveCount(0);

  await page.getByLabel('Namespace').click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await page.getByLabel('Cluster').click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Search' }).click();

  await expect(page.getByText('lock-xid-001')).toBeVisible();
  expect(apiCalls.console.filter((call) => call.path === '/api/v1/console/globalLock/query')).toHaveLength(1);
});

test('Cluster 页面首屏加载默认数据，并保留 Query 与 Reload 按钮语义', async ({ page, apiCalls }) => {
  await page.goto('/#/cluster/list');

  await expect(page.getByRole('heading', { name: 'Cluster Management' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Query' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Reload' })).toBeVisible();
  await expect(page.getByText('default-unit')).toBeVisible();

  expect(apiCalls.naming.filter((call) => call.path === '/api/v1/naming/clusterData')).toHaveLength(1);
});

test('Console 与 Naming API 请求路径和 header 保持隔离', async ({ page, apiCalls }) => {
  await page.goto('/#/transaction/list');
  await expect(page.getByText('xid-001')).toBeVisible();

  await page.goto('/#/cluster/list');
  await expect(page.getByText('default-unit')).toBeVisible();

  expect(apiCalls.console.length).toBeGreaterThanOrEqual(2);
  for (const call of apiCalls.console) {
    expect(call.path.startsWith('/api/v1/console/')).toBe(true);
    expect(call.headers.authorization).toBe(MOCK_TOKEN);
    expect(call.headers['x-seata-namespace']).toBeDefined();
  }

  expect(apiCalls.naming.length).toBeGreaterThanOrEqual(1);
  for (const call of apiCalls.naming) {
    expect(call.path.startsWith('/api/v1/naming/')).toBe(true);
    expect(call.path.startsWith('/api/v1/console/')).toBe(false);
  }
});

test('414px 视口下登录页与主控制台关键操作可见且无全局横向溢出', async ({ page }) => {
  await page.setViewportSize({ width: 414, height: 896 });
  await page.goto('/#/login');
  await page.evaluate((authorizationKey) => window.localStorage.removeItem(authorizationKey), AUTHORIZATION_STORAGE_KEY);

  await expect(page.getByRole('heading', { name: 'Apache Seata Console' })).toBeVisible();
  await expect(page.getByLabel('Username')).toBeVisible();
  await expect(page.getByLabel('Password')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  await expectNoGlobalOverflow(page);

  await page.getByLabel('Username').fill('seata');
  await page.getByLabel('Password').fill('seata');
  await page.getByRole('button', { name: 'Sign in' }).click();

  await expect(page).toHaveURL(/#\/transaction\/list$/);
  await expect(page.getByRole('heading', { name: 'Global Transactions' })).toBeVisible();
  await expect(page.getByLabel('Namespace')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Search' })).toBeVisible();
  await expectNoGlobalOverflow(page);
});

test('事务分组修改弹窗保留 original/target 维度字段，并对 change mutation 做显式断言', async ({ page, apiCalls }) => {
  await page.goto('/#/transaction-group');
  
  await expect(page.getByRole('heading', { name: 'Transaction Group Management' })).toBeVisible();
  await expect(page.getByPlaceholder('Namespace')).toBeVisible();
  await expect(page.getByPlaceholder('Cluster')).toBeVisible();
  await expect(page.getByPlaceholder('VGroup')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Search' })).toBeVisible();

  await page.getByPlaceholder('Namespace').fill('def');
  await page.getByRole('button', { name: 'Search' }).click();

  await page.getByRole('button', { name: 'Add Transaction Group' }).click();
  await expect(page.getByRole('dialog', { name: 'Add Transaction Group' })).toBeVisible();
  await page.getByRole('button', { name: 'Cancel' }).click();
  
  await expect(page.getByText('default_tx_group')).toBeVisible();
  await page.getByRole('button', { name: 'Edit' }).first().click();

  const dialog = page.getByRole('dialog', { name: 'Change Transaction Group' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByLabel('Original Namespace')).toBeVisible();
  await expect(dialog.getByLabel('Original Cluster')).toBeVisible();
  await expect(dialog.getByLabel('Transaction Group')).toBeVisible();
  await expect(dialog.getByLabel('Target Namespace')).toBeVisible();
  await expect(dialog.getByLabel('Target Cluster')).toBeVisible();
  await expect(dialog.getByLabel('Target Unit')).toBeVisible();

  await dialog.getByRole('button', { name: 'Save' }).click();

  expect(apiCalls.naming.filter((call) => call.path === '/api/v1/naming/namespace').length).toBeGreaterThan(1);

  const changeCalls = apiCalls.naming.filter((call) => call.path === '/api/v1/naming/changeGroup');
  expect(changeCalls).toHaveLength(1);
  const payload = parseFormBody(changeCalls[0].body ?? '');
  expect(payload).toMatchObject({
    originalNamespace: 'default',
    originalCluster: 'default-cluster',
    vGroup: 'default_tx_group',
    namespace: 'default',
    clusterName: 'default-cluster',
    unitName: 'default-unit'
  });
});

test('多 unit 事务分组编辑首次打开不预填 target unit，显式选择后才发送 changeGroup', async ({ page, apiCalls }) => {
  await page.goto('/#/transaction-group');

  await expect(page.getByText('multi_unit_tx_group')).toBeVisible();
  await page.getByRole('button', { name: 'Edit' }).nth(1).click();

  const dialog = page.getByRole('dialog', { name: 'Change Transaction Group' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByLabel('Target Unit')).toBeVisible();

  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(apiCalls.naming.filter((call) => call.path === '/api/v1/naming/changeGroup')).toHaveLength(0);

  await dialog.getByLabel('Target Unit').click();
  await page.keyboard.press('ArrowDown');
  await page.keyboard.press('Enter');
  await dialog.getByRole('button', { name: 'Save' }).click();

  const changeCalls = apiCalls.naming.filter((call) => call.path === '/api/v1/naming/changeGroup');
  expect(changeCalls).toHaveLength(1);
  const payload = parseFormBody(changeCalls[0].body ?? '');
  expect(payload).toMatchObject({
    originalNamespace: 'default',
    originalCluster: 'multi-unit-cluster',
    namespace: 'default',
    clusterName: 'multi-unit-cluster',
    vGroup: 'multi_unit_tx_group'
  });
  expect(['blue-unit', 'green-unit']).toContain(payload.unitName);
});

test('Saga 字体静态资源后缀受 application.yml 白名单覆盖', async () => {
  const applicationYaml = await fs.readFile(path.join(REPO_ROOT, 'namingserver/src/main/resources/application.yml'), 'utf8');
  const sagaBundleCss = await fs.readFile(
    path.join(REPO_ROOT, 'console/src/main/resources/static/saga-statemachine-designer/bundle.css'),
    'utf8'
  );

  const ignoreUrlsLine = applicationYaml.match(/ignore:\s*[\r\n]+\s*urls:\s*([^\n]+)/)?.[1] ?? '';
  const ignoreUrls = ignoreUrlsLine.split(',').map((entry) => entry.trim()).filter(Boolean);
  const referencedFontSuffixes = Array.from(sagaBundleCss.matchAll(/url\([^)]*\.([a-z0-9]+)(?:[?#][^)]*)?\)/gi))
    .map((match) => match[1].toLowerCase())
    .filter((suffix) => ['eot', 'woff2', 'woff', 'ttf', 'svg'].includes(suffix));
  const referencedFontSuffixSet = new Set(referencedFontSuffixes);

  expect(referencedFontSuffixSet).toEqual(new Set(['eot', 'woff2', 'woff', 'ttf', 'svg']));
  for (const suffix of ['eot', 'woff2', 'woff', 'ttf', 'svg']) {
    expect(ignoreUrls).toContain(`/**/*.${suffix}`);
  }
});

async function handleApiRoute(route: Route, apiCalls: ApiCalls, mockState: MockState) {
  const request = route.request();
  const call = summarizeRequest(request);

  if (call.path === '/api/v1/auth/login') {
    apiCalls.auth.push(call);
    await fulfillJson(route, { code: 200, success: true, data: MOCK_TOKEN });
    return;
  }

  if (call.path.startsWith('/api/v1/console/')) {
    apiCalls.console.push(call);
    await fulfillJson(route, consolePayload(call.path));
    return;
  }

  if (call.path.startsWith('/api/v1/naming/')) {
    apiCalls.naming.push(call);
    await fulfillJson(route, namingPayload(call, mockState));
    return;
  }

  apiCalls.unexpected.push(call);
  await fulfillJson(route, { code: 404, success: false, message: `Unexpected API path: ${call.path}` }, 404);
}

function summarizeRequest(request: Request): ApiCall {
  const url = new URL(request.url());
  return {
    method: request.method(),
    path: url.pathname,
    url: request.url(),
    headers: request.headers(),
    query: Object.fromEntries(url.searchParams.entries()),
    body: request.postData() ?? undefined
  };
}

async function fulfillJson(route: Route, payload: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body: JSON.stringify(payload)
  });
}

function consolePayload(path: string) {
  if (path === '/api/v1/console/globalSession/query') {
    return {
      code: 200,
      success: true,
      data: {
        data: [
          {
            xid: 'xid-001',
            transactionId: 1001,
            applicationId: 'order-service',
            transactionServiceGroup: 'default_tx_group',
            transactionName: 'createOrder',
            status: 1,
            timeout: 60000,
            beginTime: 1777441169018,
            applicationData: 'mock-app-data',
            branchSessionVOs: []
          }
        ],
        total: 1
      }
    };
  }

  if (path === '/api/v1/console/globalLock/query') {
    return {
      code: 200,
      success: true,
      data: {
        data: [
          {
            xid: 'lock-xid-001',
            transactionId: '1001',
            branchId: '2001',
            resourceId: 'jdbc:mysql://127.0.0.1:3306/seata',
            tableName: 'orders',
            pk: '1',
            rowKey: 'orders:1',
            gmtCreate: 1777441169018,
            gmtModified: 1777441169018
          }
        ],
        total: 1
      }
    };
  }

  return { code: 404, success: false, message: `Unexpected console API path: ${path}` };
}

function namingPayload(call: ApiCall, mockState: MockState) {
  if (call.path === '/api/v1/naming/namespace') {
    return {
      code: 200,
      success: true,
      data: mockState.namespaces
    };
  }

  if (call.path === '/api/v1/naming/clusterData') {
    const cluster = mockState.namespaces[call.query.namespace ?? '']?.clusters[call.query.clusterName ?? ''];
    return {
      code: 200,
      success: true,
      data: {
        clusterType: cluster?.type ?? 'default',
        unitData: Object.fromEntries((cluster?.units ?? []).map((unitName, index) => [
          unitName,
          {
            namingInstanceList: [
              {
                control: { host: '127.0.0.1', port: 7091 + index },
                transaction: { host: '127.0.0.1', port: 8091 + index },
                internal: { host: '127.0.0.1', port: 7092 + index },
                weight: 1,
                healthy: true,
                term: 1,
                role: 'leader',
                unit: unitName,
                version: 'mock-version',
                metadata: { zone: 'local' }
              }
            ]
          }
        ]))
      }
    };
  }

  if (call.path === '/api/v1/naming/addGroup') {
    const payload = parseFormBody(call.body ?? '');
    expect(payload.namespace).toBeTruthy();
    expect(payload.clusterName).toBeTruthy();
    expect(payload.vGroup).toBeTruthy();
    addGroupToState(mockState, payload.namespace, payload.clusterName, payload.vGroup);
    return { code: 200, success: true, data: {} };
  }

  if (call.path === '/api/v1/naming/changeGroup') {
    const payload = parseFormBody(call.body ?? '');
    expect(payload.originalNamespace).toBeTruthy();
    expect(payload.originalCluster).toBeTruthy();
    expect(payload.vGroup).toBeTruthy();
    expect(payload.namespace).toBeTruthy();
    expect(payload.clusterName).toBeTruthy();
    if (requiresUnit(mockState, payload.namespace, payload.clusterName)) {
      expect(payload.unitName).toBeTruthy();
    }
    moveGroupInState(mockState, payload);
    return { code: 200, success: true, data: {} };
  }

  return { code: 404, success: false, message: `Unexpected naming API path: ${call.path}` };
}

function createMockState(): MockState {
  return {
    namespaces: {
      default: {
        clusters: {
          'default-cluster': {
            type: 'file',
            vgroups: ['default_tx_group'],
            units: ['default-unit']
          },
          'multi-unit-cluster': {
            type: 'file',
            vgroups: ['multi_unit_tx_group'],
            units: ['blue-unit', 'green-unit']
          },
          'target-cluster': {
            type: 'file',
            vgroups: ['target_tx_group'],
            units: ['target-unit']
          }
        }
      },
      ops: {
        clusters: {
          'ops-cluster': {
            type: 'default',
            vgroups: ['ops_tx_group'],
            units: []
          }
        }
      }
    }
  };
}

function parseFormBody(body: string) {
  return Object.fromEntries(new URLSearchParams(body).entries());
}

function addGroupToState(mockState: MockState, namespace: string, clusterName: string, vGroup: string) {
  const cluster = mockState.namespaces[namespace]?.clusters[clusterName];
  if (!cluster || cluster.vgroups.includes(vGroup)) {
    return;
  }
  cluster.vgroups.push(vGroup);
}

function requiresUnit(mockState: MockState, namespace: string, clusterName: string) {
  const cluster = mockState.namespaces[namespace]?.clusters[clusterName];
  return Boolean(cluster && cluster.type !== 'default');
}

function moveGroupInState(mockState: MockState, payload: Record<string, string>) {
  const originalCluster = mockState.namespaces[payload.originalNamespace]?.clusters[payload.originalCluster];
  const targetCluster = mockState.namespaces[payload.namespace]?.clusters[payload.clusterName];
  if (originalCluster) {
    originalCluster.vgroups = originalCluster.vgroups.filter((group) => group !== payload.vGroup);
  }
  if (targetCluster && !targetCluster.vgroups.includes(payload.vGroup)) {
    targetCluster.vgroups.push(payload.vGroup);
  }
}

async function expectNoGlobalOverflow(page: Page) {
  const metrics = await page.evaluate(() => ({
    bodyScrollWidth: document.body.scrollWidth,
    documentScrollWidth: document.documentElement.scrollWidth,
    viewportWidth: window.innerWidth
  }));
  expect(Math.max(metrics.bodyScrollWidth, metrics.documentScrollWidth)).toBeLessThanOrEqual(metrics.viewportWidth + 1);
}

async function expectStoredAuthorization(page: Page, expectedValue: string) {
  const actualValue = await page.evaluate((authorizationKey) => window.localStorage.getItem(authorizationKey), AUTHORIZATION_STORAGE_KEY);
  expect(actualValue).toBe(expectedValue);
}

async function expectTokenNotExposed(page: Page, token: string) {
  const exposed = await page.evaluate((expectedToken) => ({
    inUrl: window.location.href.includes(expectedToken),
    inBodyText: document.body.innerText.includes(expectedToken)
  }), token);
  expect(exposed).toEqual({ inUrl: false, inBodyText: false });
}