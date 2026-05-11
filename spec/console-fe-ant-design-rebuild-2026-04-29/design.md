# Console 前端 Ant Design 完整重建设计

## 设计方向

用户已确认采用“完整重建 `console-fe` 前端工程”路线。默认技术栈为 Vite + React + TypeScript + Ant Design，目标 Node/npm 固定为 Node.js v24.11.1 与 npm 11.6.2，保留后端 API、HashRouter、静态资源复制与 namingserver/server 转发链路。

## 推荐技术路线

- 构建工具：Vite，输出目录固定为 `console/src/main/resources/static/console-fe/dist`。
- UI 框架：React 18 或后续兼容稳定版，组件库使用 Ant Design 5.x。
- 类型系统：TypeScript 严格模式，保留 `npm run typecheck`。
- 路由：HashRouter，保留现有 URL hash 入口。
- 请求层：统一 axios/fetch 适配层，明确区分 Console API 与 Naming API。
- 状态：优先使用 React Context、hooks 与 URL/query state，不继续引入 Redux，除非实现阶段证明某页面需要跨层复杂共享状态。
- 日期：优先使用 Ant Design 5 默认 dayjs 生态，不继续引入 moment。
- 样式：优先使用 Ant Design token、CSS Modules 或少量全局样式，不继续引入 styled-components。
- Node/npm：目标固定为 Node.js v24.11.1 与 npm 11.6.2，并同步到 `package.json engines`、`console/pom.xml` 的 `frontend-maven-plugin` nodeVersion、本地验证命令与 Task 01/16 验收记录。

## Architecture

### 前端目录结构

重建后的建议结构：

```text
console/src/main/resources/static/console-fe/
  package.json
  package-lock.json
  vite.config.ts
  tsconfig.json
  index.html
  scripts/check-frontend-licenses.mjs
  src/
    main.tsx
    app.tsx
    router.tsx
    api/
      request.ts
      auth.ts
      console.ts
      naming.ts
      types.ts
    layout/
      ConsoleLayout.tsx
      navigation.ts
    pages/
      Login/Login.tsx
      Overview/Overview.tsx
      TransactionInfo/TransactionInfo.tsx
      GlobalLockInfo/GlobalLockInfo.tsx
      ClusterManager/ClusterManager.tsx
      SagaDesigner/SagaDesigner.tsx
    components/
      PageState.tsx
      NamespaceClusterSelector.tsx
      ConfirmActionModal.tsx
    styles/
      tokens.css
      global.css
    locales/
      en-us.ts
      zh-cn.ts
  scripts/
    copy-saga-designer.mjs
```

旧 `build/webpack.*.js`、Alifd 样式、Redux reducer、jQuery/moment/styled-components 相关代码在实现阶段删除或替换。删除动作必须与功能迁移同批验证，避免保留死代码。

### Maven 集成

`console/pom.xml` 继续负责 Node/npm 安装、前端依赖安装、前端构建和资源复制。设计保持：

- workingDirectory 仍为 `src/main/resources/static/console-fe`。
- Node 版本更新为 v24.11.1，并记录捆绑 npm 11.6.2。
- npm 安装命令改为 lockfile 友好的 `npm ci`，构建命令保持 `npm run build`。
- `dist` 仍是 Maven 资源复制输入。
- `src/main/resources` 仍排除 `static/console-fe` 与 `node_modules`，避免源码工程进入 jar。

### 静态资源链路

Vite `base` 必须适配 hash 路由与相对静态资源加载，避免部署到 namingserver/server context 后资源 404。Saga state machine designer 保留为 iframe 页面，真实源路径固定为 `saga/seata-saga-statemachine-designer/dist`，通过 `scripts/copy-saga-designer.mjs` 或等价 Vite plugin 在 `npm run build` 内复制到 `dist/saga-statemachine-designer`。复制过程必须处理旧 `designer.html` 入口重命名或映射，使 iframe 能加载稳定入口；不得依赖提交 `console-fe/public/saga-statemachine-designer/`，因为旧 `.gitignore` 会忽略该目录。

### 请求适配层

请求层拆成三类：

- `src/api/request.ts`：统一超时、错误解析、header 注入、JSON 解析、message 展示与登录态处理。
- `src/api/auth.ts`：只封装 `POST /api/v1/auth/login` 与 logout 本地清理，不把 token 或内部鉴权细节暴露给 UI。
- `src/api/console.ts`：只封装 `/api/v1/console/**`，默认携带 `x-seata-namespace` 与 `x-seata-cluster`。
- `src/api/naming.ts`：只封装 `/api/v1/naming/**`，避免命名管理请求误走 ConsoleRemotingFilter。

请求层必须保留现有 controller 路径，不把 Java API 迁移成新的 REST 命名。危险操作使用 Ant Design `Modal` 或 `Popconfirm` 展示明确的操作对象、影响与后端错误信息，不使用 `dangerouslySetInnerHTML`、`.innerHTML` 或 `.insertAdjacentHTML` 注入服务端文本。

登录兼容契约固定如下：登录表单提交 `username` 与 `password` 到 `POST /api/v1/auth/login`；成功响应的 `data` 原样写入 `localStorage.Authorization`；后续请求从 `localStorage.Authorization` 读取并注入 `Authorization` header；401/403 统一清理当前请求态并跳转 `#/login`；logout 清理 `localStorage.Authorization` 与请求层默认 Authorization 注入。登录 UI 不展示 token、Bearer 前缀、内部 host、完整 API URL 或鉴权实现细节。

Console 请求上下文固定如下：namespace 必填；cluster 模式要求 cluster 有效并发送 `x-seata-cluster`；vgroup 模式允许 cluster 为空，但必须保留 query `vgroup` 并继续发送 Console API；cluster 与 vgroup 同时缺失时前端阻断请求。该规则与 `ConsoleRemotingFilter` 的 namespace 非空且 cluster 或 vgroup 任一非空条件保持一致。

### 路由与页面

HashRouter 继续作为唯一浏览器路由模式。建议路由：

- `#/login`
- `#/overview`
- `#/transaction-info`
- `#/global-lock-info`
- `#/cluster-manager`
- `#/saga-statemachine-designer`

兼容 redirect/mapping 必须覆盖旧 hash URL：

| 旧 hash URL | 新页面 |
| --- | --- |
| `#/` | redirect 到全局事务页，兼容旧默认 `/transaction/list` |
| `#/transaction/list` | 全局事务页 |
| `#/globallock/list` | 全局锁页 |
| `#/cluster/list` | 集群管理页 |
| `#/sagastatemachinedesigner` | Saga designer iframe 页 |
| `#/login` | 登录页 |

Task 05 必须实现 route redirect/mapping，Task 17 必须用 Playwright 覆盖旧 URL 入口。Task 12 只负责 Saga iframe 页面内容，不能单独改共享 router；若 Saga 路由缺口在实现阶段出现，应回到 Task 05 或以 Task 05 后续补丁处理。

### Ant Design 页面规范

页面骨架使用 `Layout`、`Menu`、`Breadcrumb`、`Space` 与 `Typography`。数据页面使用 `Table`、`Form`、`DatePicker`、`Select`、`Button`、`Drawer`、`Modal`、`Popconfirm`。反馈使用 `message`、`Alert`、`Result`、`Skeleton`、`Empty`。

所有页面必须提供：

- loading：首次加载使用 `Skeleton` 或 Table loading。
- empty：无数据时使用 `Empty`，并保留当前筛选条件可见。
- error：接口失败使用 `Alert` 或 `Result`，说明失败原因与可执行恢复动作。
- success：危险操作成功后使用 `message.success` 并刷新当前列表。
- partial：部分接口失败或部分行操作失败时保留已加载数据，并显示局部错误。

Playwright 友好要求：关键按钮、筛选项、表格、弹窗、抽屉、iframe 入口必须有稳定文本、label、role 或 `data-testid`。禁止依赖 `nth-child` 或视觉样式定位主链路。

### License 与 NOTICE 设计

license 审计分三类输出：

- `allDependencies`：`dependencies`、`devDependencies` 和传递依赖全量审计，作为构建合规门。
- `runtimeDependencies`：会进入浏览器 bundle 或静态产物的生产依赖，用于 `distribution/LICENSE-namingserver` 与 `distribution/licenses`。
- `noticeRequirements`：真实依赖要求携带 NOTICE 的条目，用于 `distribution/NOTICE-namingserver`。

建议用仓库内 Node 脚本读取 `package-lock.json` 与已安装依赖的 package metadata，避免为了 license 审计再引入许可证不符合要求的审计工具。脚本必须解析 SPDX 表达式并归一化 license token：MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license 可通过；GPL、AGPL、LGPL、MPL、EPL、CDDL、SSPL、Commons Clause、Business Source License、copyleft-like、source-available 以及 unknown、missing、custom、未解析 SEE LICENSE IN 必须失败。

可复现分类算法固定为：

1. 从 `package.json` 的 `dependencies`、`devDependencies` 与 `package-lock.json` 生成候选依赖闭包，作为 `allDependencies` 的输入。
2. 从源码 import 图识别运行时代码入口，结合 Vite/Rollup manifest 或 metafile、构建产物中的 chunk/module 引用，反推进入浏览器 bundle 或静态产物的包，作为 `runtimeDependencies` 的输入。
3. 从 `node_modules` package metadata、包内 `LICENSE`/`NOTICE` 文件和 SPDX license 字段解析 license 与 notice 义务；仅 metadata 不足以证明 runtime 分发关系，必须与 import 图和 bundle 产物交叉验证。
4. 输出 `allDependencies`、`runtimeDependencies`、`noticeRequirements` 三个 JSON section，并为每个 runtime dependency 记录 package name、version、license、licenseFile、noticeFile、bundleEvidence。
5. 对 `distribution/LICENSE-namingserver`、`distribution/NOTICE-namingserver`、`distribution/licenses` 做双向校验：runtime 依赖缺分发条目失败，分发条目不在 runtime 依赖中也失败；noticeRequirements 缺 NOTICE 条目失败，NOTICE 中保留不再需要的前端条目也失败。

license 策略来源记录：本轮 license 口径来自用户最新纠正，precedence=user_corrected；实现阶段不得回退到 strict MIT/Apache-2.0，也不得将 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License 等许可证放宽为可接受。

`distribution/LICENSE-namingserver` 只保留真实分发依赖。构建期工具若未进入 static bundle 或发布包，不写入运行分发 license 条目，但仍在 `allDependencies` 审计报告中记录并阻断违规 license。

## 兼容性决策

### 后端 API

前端完整重建不改变 server/namingserver Java API。所有 API 差异通过 TypeScript 适配层消化。若实现阶段发现某个旧接口响应字段未在当前页面使用，仍不得删除后端字段或改变后端语义。

### Header 传播

Console 请求默认从当前命名空间/集群上下文注入 `x-seata-namespace` 与 `x-seata-cluster`。namespace 缺失时必须以 Ant Design 表单错误或 `Alert` 阻止请求；cluster 缺失但 vgroup 有效时不阻断请求，必须保留 query `vgroup` 以兼容 `ConsoleRemotingFilter` 的 vgroup 转发；cluster 与 vgroup 同时缺失时才阻断 Console API。

### 登录态

保留现有登录入口与 localStorage 工具语义。`Authorization` localStorage key 是兼容边界；请求层负责读取、注入和 401/403 清理跳转，页面层只处理登录表单、错误提示和成功后导航。登录页面不得展示 token、Bearer、内部 API 路径、鉴权实现细节或服务端地址。

### 安全敏感路径

- HTML 注入：实现和验证阶段均拒绝 `dangerouslySetInnerHTML`、`.innerHTML`、`.insertAdjacentHTML`，后端错误和危险操作文案使用文本节点、`Typography`、`Descriptions` 或列表渲染。
- 恶意 payload：Playwright 或组件测试必须覆盖 `<img src=x onerror=alert(1)>` 等 payload 作为普通文本展示，不触发脚本执行。
- token 最小暴露：`Authorization` 只允许存在于 localStorage 与请求 header，不得进入 DOM、日志、错误提示、URL、query、hash、Playwright trace 文本或 console 输出。
- network 边界：Console 请求断言 `/api/v1/console/**`、`x-seata-namespace`、`x-seata-cluster` 或 query `vgroup`；Naming 请求断言 `/api/v1/naming/**` 且不匹配 `/api/*/console/*`。

## ADR-1：完整重建 Console 前端

- 日期：2026-04-29
- 决策：采用 Vite + React + TypeScript + Ant Design 完整重建 `console-fe`。
- 背景：旧工程依赖 React 16、Alifd、Alibaba Console Components、webpack 与 Node 19，依赖维护、安全审计、license 同步成本较高，且用户已确认完整重建方向。
- 后果：实现阶段需要迁移页面、请求层、构建链、license/notice 与 Maven 集成；收益是依赖面可控、UI 体系统一、浏览器验证更稳定。

## ADR-2：保持 HashRouter 与后端接口不变

- 日期：2026-04-29
- 决策：继续使用 HashRouter，保持 `/api/v1/console/**`、`/api/v1/naming/**` 与现有 header 契约。
- 背景：Console 静态资源由 `seata-console` jar 与 namingserver 分发包承载，HashRouter 不要求后端增加 HTML fallback；后端接口已被 server/namingserver controller 固定。
- 后果：前端内部可以重构，但 URL、API 与 header 兼容性必须通过适配层和浏览器验证锁住。

## ADR-3：license 分发只记录真实发布依赖

- 日期：2026-04-29
- 决策：devDependencies 全量审计，但 `distribution/LICENSE-namingserver` 与 `NOTICE-namingserver` 只记录随 namingserver 发布包或静态产物分发的前端依赖。
- 背景：构建工具不随运行分发包交付，误加入会污染分发 license；但构建依赖仍会影响供应链安全与合规，需要作为构建门审计。
- 后果：实现阶段需要区分全量审计清单与分发清单，不能简单复制 lockfile 中所有 dev 依赖到分发 license。

## 风险与缓解

- 风险：Ant Design 或 Vite 传递依赖出现 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、unknown、missing、custom 或未解析 SEE LICENSE IN。缓解：license 审计脚本阻断并要求换依赖或回 PM 裁决；MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license 可继续使用。
- 风险：Vite `base` 配置不当导致 namingserver 分发资源 404。缓解：构建后检查 `dist` 资源引用，并用 Maven package 后静态资源路径验证。
- 风险：页面迁移时遗漏旧 hash URL。缓解：`router.tsx` 保留 redirect 映射，Playwright 覆盖旧 URL 入口。
- 风险：危险操作确认文案从旧 HTML 注入迁移时丢失细节。缓解：用 Ant Design Modal 文本和列表渲染，测试覆盖操作对象、确认按钮、后端错误展示。

## Verification Strategy

- 前端依赖安装：`npm ci`
- 类型检查：`npm run typecheck`
- 构建：`npm run build`
- 安全审计：`npm audit --audit-level=low`
- license 审计：`node scripts/check-frontend-licenses.mjs --all`
- 静态安全扫描：反向断言源码中不存在 `dangerouslySetInnerHTML`、`.innerHTML`、`.insertAdjacentHTML`。
- 浏览器验证：`npm run test:e2e`，覆盖登录、旧 hash URL、全局事务、全局锁、集群管理、Saga iframe、success、partial、network failure、console error、空态、错误态与不超过 414px 的窄屏视口。
- Network 验证：Playwright 拦截 Console 与 Naming 请求，断言 path、Authorization header、namespace/cluster/vgroup 传递和 token 不泄漏到 URL。
- Maven 集成：`mvn -pl console -am package`
- 合并前全量：`mvn clean install -T4C`

## Spec 自检

- 占位符扫描：通过，本文档未保留禁用占位表达。
- 内部一致性：通过，技术路线、接口兼容、license/security 与分发链路互相一致。
- Scope decomposition：通过，单一 Spec 覆盖 Console 前端重建与其必需分发合规；后端 API 改造显式排除。
- 歧义消解：通过，默认路线、路由模式、license 策略、devDependencies 分发边界均已固定。