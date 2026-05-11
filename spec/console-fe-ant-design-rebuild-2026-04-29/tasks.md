# 实现计划：Console 前端 Ant Design 完整重建

## 概览

- 总任务数：17
- sprint_phase：Build
- 并行机会：Task 06、07、08、10、11 可在 Task 01-05 完成后按页面文件无交叉并行；Task 09 必须依赖 Task 08；Task 12 必须依赖 Task 03 与 Task 05，且不得单独修改共享 router。Task 14 可在 Task 01 完成后与页面迁移并行；Task 15 必须在 Task 14 产出分发清单后执行。
- 默认上下文：`context_mode=full` 用于构建链、请求层、license/notice 与 Maven 集成；页面迁移可使用 `context_mode=lean`，但必须消费 `requirements.md`、`design.md` 和对应旧页面文件。
- 页面任务粒度：Task 06-12 是子计划级任务，进入 implementation 前必须再拆成 2-5 分钟切片；页面任务只消费 Task 04 落完整的 API 适配层，不补共享 API 接口、共享请求类型或共享路由。

## 快速参考

- requirements.md：完整重建 Console 前端，保持后端接口、HashRouter、静态资源与 license/security 合规。
- design.md：Vite + React + TypeScript + Ant Design，Node.js v24.11.1/npm 11.6.2，Console API 与 Naming API 分层，分发 license 只记录真实发布依赖。
- 主要禁止项：不改 Java API；不引入 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、unknown、missing、custom 或未解析 SEE LICENSE IN 前端依赖；不保留旧无用前端 license 条目；不使用危险 HTML 注入。

## Task 01: 重建前端工程元数据与依赖边界

**目标**：将旧前端 manifest 切换为 Vite + React + TypeScript + Ant Design 工程，并锁定 Node.js v24.11.1、npm 11.6.2 与依赖许可边界。

**files_involved**：
- `console/src/main/resources/static/console-fe/package.json`（替换）
- `console/src/main/resources/static/console-fe/package-lock.json`（替换）
- `console/src/main/resources/static/console-fe/.npmrc`（新建或修改）

**依赖**：无依赖，可立即开始。

**parallel_ready**: false

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 影响 console Maven 构建输入，不影响 Java API。

**实现步骤**：
1. 删除旧 `@alicloud/*`、`@alifd/*`、React 16、redux、styled-components、jquery、moment、webpack、babel 旧构建链依赖。
2. 添加 Vite、React、TypeScript、Ant Design、axios、dayjs、Playwright 测试依赖；每个新增依赖必须先确认 license 符合用户最新纠正的 policy，允许 MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license，阻断 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、unknown、missing、custom 或未解析 SEE LICENSE IN。
3. 设置 `engines.node` 为 `24.11.1`，设置 `engines.npm` 为 `11.6.2`，并在 README 或脚本输出中只引用这一组精确版本。
4. 定义脚本：`ci:install`、`dev`、`typecheck`、`build`、`audit:security`、`audit:licenses`、`test:e2e`。
5. 使用 `npm install --package-lock-only` 生成与 manifest 一致的 lockfile；若出现非允许 license 依赖，换依赖后重新生成 lockfile。

**验证命令**：
- `npm ci`
- `npm run typecheck`

**验收标准**：
- `package-lock.json` 与 `package.json` 同步。
- `npm ci` 退出码为 0。
- `node --version` 输出 `v24.11.1`，`npm --version` 输出 `11.6.2`。
- lockfile 中不存在旧 Alifd、Alibaba Console Components、jQuery、moment、styled-components、webpack 运行链路。

## Task 02: 建立 Vite 与 TypeScript 构建配置

**目标**：用 Vite 配置替换旧 webpack 构建，并保持 `dist` 输出语义。

**files_involved**：
- `console/src/main/resources/static/console-fe/vite.config.ts`（新建）
- `console/src/main/resources/static/console-fe/tsconfig.json`（替换）
- `console/src/main/resources/static/console-fe/tsconfig.node.json`（新建）
- `console/src/main/resources/static/console-fe/index.html`（新建）
- `console/src/main/resources/static/console-fe/public/favicon.ico`（保留或替换）
- `console/src/main/resources/static/console-fe/build/webpack.base.conf.js`（删除）
- `console/src/main/resources/static/console-fe/build/webpack.prod.conf.js`（删除）
- `console/src/main/resources/static/console-fe/build/webpack.dev.conf.js`（删除）
- `console/src/main/resources/static/console-fe/build/version-plugin.js`（删除或替换到 Vite plugin）
- `console/src/main/resources/static/console-fe/build/copy-dist.js`（删除）

**依赖**：Task 01。

**parallel_ready**: false

**context_mode**: full

**model_hint**: standard

**blast_radius_note**: 影响前端构建产物路径，需与 Maven 复制语义一致。

**实现步骤**：
1. 配置 Vite React plugin、`outDir: "dist"`、相对资源 base 与 hash 路由兼容。
2. 将旧 `public/index.html` 迁移为根 `index.html`，保留 Seata 标题与必要 meta。
3. 开启 TypeScript strict、JSX react-jsx、路径别名 `@` 指向 `src`。
4. 删除旧 webpack 构建脚本，避免双构建链共存。
5. 若旧 version plugin 仍需要注入版本信息，改为 Vite plugin 或构建时常量，且不改变后端接口。

**验证命令**：
- `npm run typecheck`
- `npm run build`

**验收标准**：
- `dist/index.html` 和静态资源生成成功。
- 旧 webpack 入口不再被 package scripts 或 Maven 调用。

## Task 03: 保留静态资源复制与 Saga designer 入口

**目标**：确保 Vite 构建后 Saga state machine designer iframe 与既有静态资产仍可访问。

**files_involved**：
- `console/src/main/resources/static/console-fe/vite.config.ts`（修改）
- `console/src/main/resources/static/console-fe/scripts/copy-saga-designer.mjs`（新建）
- `console/src/main/resources/static/console-fe/build/copyDesigner.js`（删除或替换）
- `console/src/main/resources/static/console-fe/build/copyFile.js`（删除或替换）
- `saga/seata-saga-statemachine-designer/dist`（读取源，不改源语义）
- `console/src/main/resources/static/css/`（读取源，不改源语义）
- `console/src/main/resources/static/js/`（读取源，不改源语义）

**依赖**：Task 02。

**parallel_ready**: false

**context_mode**: full

**model_hint**: standard

**blast_radius_note**: 影响静态资源打包，不影响 Java API。

**实现步骤**：
1. 确认 Saga designer 真实源路径为 `saga/seata-saga-statemachine-designer/dist`，并记录旧 `console-fe/.gitignore` 忽略 `/public/saga-statemachine-designer/`。
2. 将需要随 Console 前端分发的静态资源纳入 `npm run build` 内的可复现 copy 步骤，输出到 `dist/saga-statemachine-designer`。
3. 保持 iframe 可通过 hash 路由页面加载，不要求后端新增路由 fallback。
4. 删除不再使用的旧 copy 脚本，或者将其替换为 Vite 构建内可执行脚本。
5. 在 copy 步骤中处理旧 `designer.html` 入口重命名或映射，保证 iframe 入口在构建产物中稳定存在。

**验证命令**：
- `npm run build`
- `test -f dist/index.html`

**验收标准**：
- `dist/saga-statemachine-designer` 或等价路径存在。
- Saga designer 产物来自 `saga/seata-saga-statemachine-designer/dist`，不是手工提交的 `public/saga-statemachine-designer`。
- Console 页面引用静态资源使用相对路径，不依赖开发服务器绝对路径。

## Task 04: 实现 API 适配层与类型契约

**目标**：建立 Console API 与 Naming API 分层适配，锁住后端兼容契约。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/api/request.ts`（新建）
- `console/src/main/resources/static/console-fe/src/api/auth.ts`（新建）
- `console/src/main/resources/static/console-fe/src/api/console.ts`（新建）
- `console/src/main/resources/static/console-fe/src/api/naming.ts`（新建）
- `console/src/main/resources/static/console-fe/src/api/types.ts`（新建）
- `console/src/main/resources/static/console-fe/src/utils/cookie.ts`（迁移或替换）
- `console/src/main/resources/static/console-fe/src/utils/localstorage.ts`（迁移或替换）
- `console/src/main/resources/static/console-fe/src/service/transactionInfo.ts`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/service/globalLockInfo.ts`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/service/clusterManager.ts`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/utils/request.ts`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/utils/requestV2.ts`（迁移后删除）

**依赖**：Task 01。

**parallel_ready**: false

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 锁定 `/api/v1/console/**` 与 `/api/v1/naming/**` 客户端契约，不修改服务端。

**实现步骤**：
1. 在 `request.ts` 中统一超时、错误解析、登录态处理与 Ant Design message 注入接口。
2. 在 `auth.ts` 中封装 `POST /api/v1/auth/login`，请求体为 `username`、`password`；成功响应 `data` 写入 `localStorage.Authorization`；logout 清理该 key 与请求层 Authorization 注入状态。
3. 在 `request.ts` 中从 `localStorage.Authorization` 注入 `Authorization` header，401/403 清理登录态并跳转 `#/login`。
4. 在 Console API 请求中默认注入 `x-seata-namespace`；cluster 模式注入 `x-seata-cluster`，vgroup 模式保留 query `vgroup` 并允许 cluster 为空。
5. 在 Naming API 请求中使用 `/api/v1/naming` base path，避免误匹配 Console 转发规则。
6. 一次性补齐登录、全局事务、分支事务、全局锁、命名空间、集群、分组、Saga 页面所需 API 类型与方法；后续页面任务只消费这些 API，不新增共享接口。
7. 删除旧 service 与 request 文件前，确认所有页面已迁移引用。

**验证命令**：
- `npm run typecheck`

**验收标准**：
- TypeScript 能发现 API 字段误用。
- 登录、Authorization 注入、401/403 跳转、namespace/cluster/vgroup 上下文均有 API contract mock 覆盖。
- 前端无旧 `requestV2`、旧 service 文件引用。

## Task 05: 搭建 HashRouter、布局与主题骨架

**目标**：用 Ant Design 搭建后台控制台骨架，保留 hash URL 兼容。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/main.tsx`（新建或替换）
- `console/src/main/resources/static/console-fe/src/app.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/router.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/layout/ConsoleLayout.tsx`（新建）
- `console/src/main/resources/static/console-fe/src/layout/navigation.ts`（新建）
- `console/src/main/resources/static/console-fe/src/components/NamespaceClusterSelector.tsx`（新建）
- `console/src/main/resources/static/console-fe/src/styles/tokens.css`（新建）
- `console/src/main/resources/static/console-fe/src/styles/global.css`（新建）
- `console/src/main/resources/static/console-fe/src/layout/index.tsx`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/layout/index.scss`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/components/Header/Header.tsx`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/components/Header/index.scss`（迁移后删除）

**依赖**：Task 02、Task 04。

**parallel_ready**: false

**context_mode**: full

**model_hint**: standard

**blast_radius_note**: 影响所有页面入口和视觉骨架。

**实现步骤**：
1. 使用 `HashRouter` 注册登录、概览、全局事务、全局锁、集群管理、Saga designer 路由。
2. 对旧 hash URL 增加兼容 redirect/mapping：`#/`、`#/transaction/list`、`#/globallock/list`、`#/cluster/list`、`#/sagastatemachinedesigner`、`#/login`。
3. 使用 Ant Design `Layout`、`Menu`、`ConfigProvider`、`App` 组件建立页面骨架。
4. 使用主题 token 承接 Seata 主色、间距、文字层级，不写营销式 hero。
5. 为导航、语言切换、namespace/cluster selector 添加稳定 label 或 `data-testid`。

**验证命令**：
- `npm run typecheck`
- `npm run build`

**验收标准**：
- 旧 hash URL 可以进入对应新页面。
- Task 17 可直接复用这些旧 URL 做 Playwright 验收，不需要再改 router。
- 主布局无绝对定位拼接和魔法偏移。

## Task 06: 迁移登录页

**目标**：用 Ant Design Form 重建登录体验，保持登录 API 与登录态存储兼容。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/Login/Login.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/pages/Login/index.ts`（保留或替换）
- `console/src/main/resources/static/console-fe/src/pages/Login/index.scss`（删除）

**依赖**：Task 04、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: standard

**blast_radius_note**: 不改变鉴权后端语义。

**实现步骤**：
1. 使用 `Form`、`Input.Password`、`Button`、`Alert` 实现登录表单。
2. 表单校验必须聚焦首个错误字段。
3. 登录失败显示业务错误，不暴露 token、内部 host 或鉴权实现细节。
4. 登录成功后按旧路由语义跳转到默认 Console 页面。
5. 只消费 Task 04 的 `auth.ts` 与请求层登录态工具；若接口缺失，回 Task 04 补齐，不在本任务修改共享 API。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep login`

**验收标准**：
- 空用户名/密码有字段级错误。
- 登录失败和成功路径均可被 Playwright 通过 label/button 定位。
- 登录页面 DOM、错误提示、URL 与 console 输出不包含 token、Bearer、内部 host 或完整 API 实现细节。

## Task 07: 迁移概览页

**目标**：用 Ant Design 数据展示组件迁移概览页，保留原有 overview 数据接口。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/Overview/Overview.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/pages/Overview/index.ts`（保留或替换）
- `console/src/main/resources/static/console-fe/src/pages/Overview/index.scss`（删除）

**依赖**：Task 04、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: standard

**blast_radius_note**: 只影响前端展示。

**实现步骤**：
1. 使用 `Card`、`Descriptions`、`Skeleton`、`Empty`、`Alert` 展示 overview 数据。
2. 保留 namespace/cluster 上下文变化后的刷新语义。
3. 为空数据、接口失败和部分字段缺失提供明确状态。
4. 只消费 Task 04 的 API 适配层；若 overview API 缺失，回 Task 04 补齐，不在本任务修改共享 API。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep overview`

**验收标准**：
- loading、empty、error 状态可见且可定位。
- 页面不依赖旧 overview reducer。

## Task 08: 迁移全局事务查询与表格

**目标**：重建全局事务列表查询、分页、筛选和详情入口。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/TransactionInfo/TransactionInfo.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/pages/TransactionInfo/index.ts`（保留或替换）
- `console/src/main/resources/static/console-fe/src/pages/TransactionInfo/index.scss`（删除）

**依赖**：Task 04、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: capable

**blast_radius_note**: 只调用现有 `/api/v1/console/globalSession/**`。

**实现步骤**：
1. 使用 `Form` 构建 xid、status、日期范围、分页条件。
2. 使用 `Table` 展示事务列表，保留分页参数与后端字段映射。
3. 使用 `Drawer` 或 `Descriptions` 展示行详情和分支事务摘要。
4. 提供 loading、empty、error、partial 状态。
5. 只消费 Task 04 的 globalSession API 与已有通用状态组件；若缺 API 或共享组件，分别回 Task 04 或 Task 13 补齐，不在本任务修改共享文件。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep transaction-query`

**验收标准**：
- 查询请求路径为 `/api/v1/console/globalSession/query`。
- 请求包含 namespace/cluster headers。
- vgroup 模式下请求保留 query `vgroup`，且 cluster 为空不阻断查询。
- 表格分页与筛选可被 Playwright 定位。

## Task 09: 迁移全局事务危险操作

**目标**：迁移删除、强删、停止、开始、提交/回滚、状态变更等操作确认流程。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/TransactionInfo/TransactionInfo.tsx`（修改）
- `console/src/main/resources/static/console-fe/src/components/ConfirmActionModal.tsx`（新建）

**依赖**：Task 08。

**parallel_ready**: false

**context_mode**: lean

**model_hint**: capable

**blast_radius_note**: 危险操作仍调用现有 server controller，不改变后端语义。

**实现步骤**：
1. 用 `Modal`、`Popconfirm`、`Alert` 明确展示操作对象 xid、目标状态和不可逆影响。
2. 删除旧 HTML 注入式确认内容，改为文本、列表或 `Descriptions` 渲染。
3. 操作成功后刷新当前查询结果并显示 `message.success`。
4. 操作失败时保留当前数据并显示后端错误。
5. 只消费 Task 04 的 action API；若缺接口，回 Task 04 补齐，不在本任务修改共享 API。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep transaction-actions`

**验收标准**：
- 不存在 `dangerouslySetInnerHTML`。
- 不存在 `.innerHTML` 与 `.insertAdjacentHTML`。
- 恶意 HTML payload 在确认弹窗和错误提示中按文本展示。
- 每个危险操作有明确确认入口和失败展示。

## Task 10: 迁移全局锁页面

**目标**：用 Ant Design 表单与表格迁移全局锁查询、删除与检查能力。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/GlobalLockInfo/GlobalLockInfo.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/pages/GlobalLockInfo/index.ts`（保留或替换）
- `console/src/main/resources/static/console-fe/src/pages/GlobalLockInfo/index.scss`（删除）

**依赖**：Task 04、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: standard

**blast_radius_note**: 只调用现有 `/api/v1/console/globalLock/**`。

**实现步骤**：
1. 使用 `Form` 迁移全局锁查询条件。
2. 使用 `Table` 展示锁记录与行操作。
3. 删除与检查操作使用 `Popconfirm` 或 `Modal`，失败时保留列表状态。
4. 保留 namespace/cluster headers。
5. 只消费 Task 04 的 globalLock API；若缺接口，回 Task 04 补齐，不在本任务修改共享 API。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep global-lock`

**验收标准**：
- 查询请求路径为 `/api/v1/console/globalLock/query`。
- vgroup 模式下请求保留 query `vgroup`，且 cluster 为空不阻断查询。
- 删除与检查请求路径保持原 controller 契约。

## Task 11: 迁移集群管理与 naming API 页面

**目标**：迁移命名空间、集群、分组相关管理能力，并确保走 `/api/v1/naming/**`。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/ClusterManager/ClusterManager.tsx`（替换）
- `console/src/main/resources/static/console-fe/src/pages/ClusterManager/index.ts`（保留或替换）
- `console/src/main/resources/static/console-fe/src/pages/ClusterManager/index.scss`（删除）

**依赖**：Task 04、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: capable

**blast_radius_note**: naming API 不走 ConsoleRemotingFilter，不修改 namingserver controller。

**实现步骤**：
1. 使用 `Tabs`、`Table`、`Form`、`Modal` 迁移命名空间、集群、分组操作。
2. 所有请求使用 `/api/v1/naming` base path。
3. 变更 namespace/cluster 后同步更新全局上下文和页面数据。
4. 提供局部失败状态，避免一个分组操作失败清空整个页面。
5. 只消费 Task 04 的 naming API 与 Task 05 的 namespace/cluster selector；若缺接口或共享组件，回对应前置任务补齐，不在本任务修改共享文件。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep cluster-manager`

**验收标准**：
- naming 请求不匹配 `/api/*/console/*` 转发规则。
- naming 请求不携带错误的 Console vgroup query，Console 请求不误走 `/api/v1/naming`。
- namespace 与 cluster 控件有稳定 label。

## Task 12: 迁移 Saga designer iframe 页面

**目标**：保留 Saga state machine designer iframe 入口与加载状态。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/pages/SagaDesigner/SagaDesigner.tsx`（新建）
- `console/src/main/resources/static/console-fe/src/components/Iframe/Iframe.tsx`（迁移后删除）
- `console/src/main/resources/static/console-fe/src/components/Iframe/index.scss`（删除）

**依赖**：Task 03、Task 05。

**parallel_ready**: true

**context_mode**: lean

**model_hint**: standard

**blast_radius_note**: 只影响前端 iframe 展示。

**实现步骤**：
1. 使用 Ant Design `Result` 或 `Spin` 展示 iframe 加载和错误状态。
2. iframe `src` 指向构建后可访问的 Saga designer 相对路径。
3. 给 iframe 添加稳定 title 与 `data-testid`。
4. 只消费 Task 05 已注册的路由与 Task 03 已复制的静态资源；若路由缺失，回 Task 05 补齐，不在本任务修改共享 router。

**验证命令**：
- `npm run typecheck`
- `npm run test:e2e -- --grep saga-designer`

**验收标准**：
- iframe 可加载 Saga designer 静态资源。
- iframe 入口在 hash 路由下可直接访问。

## Task 13: 迁移国际化、通用状态与死代码清理

**目标**：迁移中英文文案、通用状态组件和工具函数，清除旧 reducer 与无用样式。

**files_involved**：
- `console/src/main/resources/static/console-fe/src/locales/en-us.ts`（替换）
- `console/src/main/resources/static/console-fe/src/locales/zh-cn.ts`（替换）
- `console/src/main/resources/static/console-fe/src/locales/index.ts`（替换）
- `console/src/main/resources/static/console-fe/src/components/PageState.tsx`（补齐）
- `console/src/main/resources/static/console-fe/src/utils/common.ts`（迁移或删除）
- `console/src/main/resources/static/console-fe/src/reducers/index.ts`（删除）
- `console/src/main/resources/static/console-fe/src/reducers/base.ts`（删除）
- `console/src/main/resources/static/console-fe/src/reducers/login.ts`（删除）
- `console/src/main/resources/static/console-fe/src/reducers/locale.ts`（删除）
- `console/src/main/resources/static/console-fe/src/reducers/overview.ts`（删除）
- `console/src/main/resources/static/console-fe/src/index.scss`（迁移后删除）

**依赖**：Task 05-12。

**parallel_ready**: false

**context_mode**: full

**model_hint**: standard

**blast_radius_note**: 清理旧状态层，影响所有页面 import。

**实现步骤**：
1. 将旧 locale key 合并为新页面使用的稳定文案 key。
2. 抽取 `PageState` 覆盖 loading、empty、error、partial。
3. 删除 Redux reducer 与未引用样式文件。
4. 扫描 import，删除无用依赖和死代码。

**验证命令**：
- `npm run typecheck`
- `npm run build`

**验收标准**：
- 无 Redux、Alifd、styled-components、jQuery、moment import。
- 中英文切换后关键页面文案完整。

## Task 14: 实现前端 license 与安全审计脚本

**目标**：提供可复现合规门，确保前端依赖符合用户最新纠正的 license policy 且无已知漏洞。

**files_involved**：
- `console/src/main/resources/static/console-fe/scripts/check-frontend-licenses.mjs`（新建）
- `console/src/main/resources/static/console-fe/package.json`（修改 scripts）
- `console/src/main/resources/static/console-fe/package-lock.json`（输入）

**依赖**：Task 01。

**parallel_ready**: true

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 影响构建门，不影响运行时代码。

**实现步骤**：
1. 编写 Node 脚本读取 `package.json` dependencies/devDependencies、lockfile、源码 import 图、Vite/Rollup manifest 或 metafile、构建产物引用与 `node_modules` package metadata。
2. 将依赖分类为 `allDependencies`、`runtimeDependencies`、`noticeRequirements`；仅 lockfile 或 `node_modules` metadata 不足以判定 runtime 分发依赖。
3. 解析 SPDX 表达式并归一化 license token；MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license 可通过，GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、缺失 license、未知 license、custom license、未解析 SEE LICENSE IN 均使脚本退出码非 0。
4. 输出 JSON 报告到 `target/frontend-license-report.json` 或控制台，避免将生成物提交到源码目录。
5. 为每个 runtime dependency 记录 package name、version、license、licenseFile、noticeFile 与 bundleEvidence。
6. 在 `package.json` 中加入 `audit:licenses` 与 `audit:security` 脚本。

**验证命令**：
- `npm ci`
- `npm run audit:licenses`
- `npm audit --audit-level=low`

**验收标准**：
- license 脚本对 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、missing、unknown、custom、未解析 SEE LICENSE IN 返回非 0；对 MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license 返回 0。
- `allDependencies`、`runtimeDependencies`、`noticeRequirements` 可稳定复现，且 runtime 依赖均有 import 或 bundle 证据。
- `npm audit --audit-level=low` 返回 0 个漏洞。

## Task 15: 更新 namingserver 分发 LICENSE/NOTICE

**目标**：根据真实前端分发依赖更新 namingserver license 与 notice 条目，删除旧未使用依赖条目。

**files_involved**：
- `distribution/LICENSE-namingserver`（修改）
- `distribution/NOTICE-namingserver`（修改）
- `distribution/licenses/`（新增、修改、删除前端相关 license 文本）
- `console/src/main/resources/static/console-fe/scripts/check-frontend-licenses.mjs`（读取报告格式）

**依赖**：Task 14；Task 01-13 产出最终依赖与 bundle 后执行。

**parallel_ready**: false

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 影响 Apache 分发合规，不影响 Java API。

**实现步骤**：
1. 使用 license 审计报告的 runtimeDependencies 作为分发 license 输入。
2. 删除 `LICENSE-namingserver` 中不再使用的旧前端条目，包括 Alifd、Alibaba Console Components、旧 React 16、jQuery、moment、styled-components 以及旧 webpack/babel 分发条目。
3. 为真实分发依赖补充符合 policy 的 permissive license 行，并引用 `distribution/licenses` 中对应文本。
4. 按 noticeRequirements 更新 `NOTICE-namingserver`；无 notice 要求时不新增多余前端 notice。
5. 清理 `distribution/licenses` 中不再被任何分发 license 引用的旧前端 license 文本。
6. 执行双向一致性检查：`runtimeDependencies` 中每个依赖必须在 LICENSE 或 `distribution/licenses` 有对应条目；LICENSE/NOTICE 中每个前端条目必须能回到 `runtimeDependencies` 或 `noticeRequirements`。

**验证命令**：
- `npm run audit:licenses`
- `! grep -E "@alifd|@alicloud|jquery|moment|styled-components" distribution/LICENSE-namingserver`
- `node scripts/check-frontend-licenses.mjs --verify-distribution`

**验收标准**：
- 反向 grep 旧前端关键词无未使用条目残留，命令退出码为 0。
- LICENSE/NOTICE 与 runtimeDependencies、noticeRequirements 一致。
- 不存在 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、自定义、未知、缺失或未解析 SEE LICENSE IN 前端条目；BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license 条目可保留。

## Task 16: 更新 console Maven 前端构建集成

**目标**：让 Maven 使用新 Node/npm 与 Vite 构建链，并继续复制 `dist` 静态资源。

**files_involved**：
- `console/pom.xml`（修改）
- `console/src/main/resources/static/console-fe/package.json`（确认 scripts）
- `console/src/main/resources/static/console-fe/package-lock.json`（确认 lockfile）
- `console/src/main/resources/static/console-fe/dist/`（构建产物，不提交源码工程内生成物，除非仓库既有规则要求提交）

**依赖**：Task 01-15。

**parallel_ready**: false

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 影响 `seata-console` jar 静态资源，不影响后端 controller。

**实现步骤**：
1. 将 `frontend-maven-plugin` Node 版本更新为 Task 01 固定的 Node.js v24.11.1。
2. 将 Maven 前端安装命令切换为 lockfile 友好的 `npm ci`。
3. 保持 `npm run build` 作为 Maven build 命令。
4. 确认 `maven-resources-plugin` 仍从 `console-fe/dist` 复制到 `target/classes/static`。
5. 确认 resources excludes 仍排除 `static/console-fe` 与 `node_modules`。

**验证命令**：
- `mvn -pl console -am package`

**验收标准**：
- `seata-console` jar 中包含新 Console 静态资源。
- Maven 不再安装 Node v19.5.0，并通过 Maven 下载的 Node 输出 `v24.11.1`、npm 输出 `11.6.2`。

## Task 17: 执行最终验证与回归记录

**目标**：用最小充分命令证明前端重建、合规、浏览器主链路与 Maven 集成可交付。

**files_involved**：
- `console/src/main/resources/static/console-fe/package.json`（读取 scripts）
- `console/src/main/resources/static/console-fe/package-lock.json`（读取 lockfile）
- `console/src/main/resources/static/console-fe/playwright.config.ts`（新建或修改）
- `console/src/main/resources/static/console-fe/tests/e2e/console.spec.ts`（新建）
- `distribution/LICENSE-namingserver`（读取 diff）
- `distribution/NOTICE-namingserver`（读取 diff）
- `console/pom.xml`（读取 diff）

**依赖**：Task 01-16。

**parallel_ready**: false

**context_mode**: full

**model_hint**: capable

**blast_radius_note**: 验证阶段，不扩大实现范围。

**实现步骤**：
1. 运行 `npm ci`，确保 lockfile 可复现安装。
2. 运行 `npm run typecheck` 与 `npm run build`。
3. 运行 `npm audit --audit-level=low` 与 `npm run audit:licenses`。
4. 运行静态安全扫描，反向断言不存在 `dangerouslySetInnerHTML`、`.innerHTML`、`.insertAdjacentHTML`。
5. 运行 Playwright 主链路，覆盖登录、旧 hash URL、全局事务查询、全局锁查询、集群管理、Saga iframe、success、partial、network failure、console error、空态、错误态与不超过 414px 的窄屏视口。
6. 用 Playwright network 断言 Console/Naming path、Authorization header、namespace/cluster/vgroup 传递，并确认 token 不出现在 DOM、日志、错误、URL 或 trace。
7. 运行 `mvn -pl console -am package` 验证 Maven 集成。
8. 合并前运行 `mvn clean install -T4C` 或由 PM 明确记录验证预算调整。

**验证命令**：
- `npm ci`
- `npm run typecheck`
- `npm run build`
- `npm audit --audit-level=low`
- `npm run audit:licenses`
- `node scripts/check-frontend-licenses.mjs --verify-distribution`
- `npm run scan:html-injection`
- `npm run test:e2e`
- `mvn -pl console -am package`
- `mvn clean install -T4C`

**验收标准**：
- 所有命令退出码为 0。
- license 审计采用用户最新纠正的 policy：允许 MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license，阻断 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、unknown、missing、custom 与未解析 SEE LICENSE IN；分发 LICENSE/NOTICE 只覆盖真实 runtimeDependencies/noticeRequirements。
- Playwright 截图或 trace 能证明关键页面非空、无明显布局重叠、关键交互可定位，并覆盖 success、partial、network failure、console error 与窄屏。
- token 不出现在 DOM、日志、错误、URL 或 trace；Console/Naming 请求 path 与 header/query 断言通过。
- license/security/Maven 证据可作为最终收口记录输入。

## 验证计划

- 最小前端验证：`npm ci && npm run typecheck && npm run build`
- 安全与合规验证：`npm audit --audit-level=low && npm run audit:licenses && node scripts/check-frontend-licenses.mjs --verify-distribution && npm run scan:html-injection`
- 浏览器验证：`npm run test:e2e`
- Maven 最小集成：`mvn -pl console -am package`
- 仓库合并前验证：`mvn clean install -T4C`

## 非目标

- 不修改 server/namingserver Java API。
- 不新增数据库、Kafka、事务协议或鉴权协议。
- 不提交 `node_modules`。
- 不把未随 namingserver 发布包分发的构建期工具写入运行分发 license 条目。

## Plan 自检

- Spec 覆盖：requirements 中的完整重建、接口兼容、静态资源、license/security、distribution 更新、浏览器验证和 Maven 集成均映射到 Task 01-17。
- 占位符扫描：通过，本计划未保留禁用占位表达。
- 类型一致性：通过，API 适配层、页面、license 脚本、Maven 集成任务的路径和脚本名一致。
- 任务独立性：通过，每个任务包含目标、文件、依赖、并行标记、上下文模式、验证命令和验收标准。
- 并行性复核：通过，Task 06、07、08、10、11 页面任务文件集合互不交叉；Task 09 串行依赖 Task 08；Task 12 只消费 Task 03/05 产物；共享 `api` 文件只由 Task 04 一次性落完整适配层。

## Traceability Matrix

| Requirement | Tasks | Verification |
| --- | --- | --- |
| R1 完整重建 | Task 01-13 | `npm run typecheck`、`npm run build`、Playwright |
| R2 Node/npm 升级 | Task 01、Task 16、Task 17 | `node --version`、`npm --version`、`npm ci`、`mvn -pl console -am package` |
| R3/R4 后端接口与 headers | Task 04、Task 08-11、Task 17 | API mock、Playwright network check、vgroup query check |
| R5 HashRouter | Task 05、Task 12、Task 17 | Playwright old hash URL check |
| R6 Ant Design 控制台体验 | Task 05-13 | Playwright role/text/label selectors，视觉状态检查 |
| R7 静态资源与 Saga iframe | Task 03、Task 12、Task 16 | `npm run build`、iframe Playwright check、Maven package |
| R8/R10 license/notice | Task 14、Task 15、Task 17 | `npm run audit:licenses`、`node scripts/check-frontend-licenses.mjs --verify-distribution`、distribution diff review；policy provenance=用户最新纠正，precedence=user_corrected |
| R9 security audit | Task 09、Task 14、Task 17 | `npm audit --audit-level=low`、`npm run scan:html-injection`、Playwright token/network/console/narrow-screen checks |