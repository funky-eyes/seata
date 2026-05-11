# Console 前端 Ant Design 完整重建需求

## 目标

本 Spec Bundle 定义 `console/src/main/resources/static/console-fe` 的完整重建要求。新前端必须采用 Vite、React、TypeScript 与 Ant Design 体系，继续作为 Apache Seata Console 的后台工具界面交付到 `seata-console` jar，并通过 namingserver 分发包获得静态资源。

## 背景与基线

- 当前 Console 前端位于 `console/src/main/resources/static/console-fe`，基线依赖包含 `@alicloud/console-components`、`@alifd/next`、React 16、react-router 5、redux、styled-components 4、jquery、moment、lodash、axios、yamljs。
- 当前构建链使用 webpack 脚本，Maven 通过 `frontend-maven-plugin` 在 `console/src/main/resources/static/console-fe` 工作目录执行 Node/npm 安装、`npm install` 与 `npm run build`。
- 当前 Maven 配置使用 Node v19.5.0，本地内置 npm 为 9.3.1。目标实现禁止继续以 Node 19 作为目标版本，统一选定 Node.js v24.11.1 与其捆绑 npm 11.6.2。
- `console/pom.xml` 将 `console-fe/dist` 复制到 `target/classes/static`，并从 `src/main/resources` 排除 `static/console-fe` 与 `node_modules`。
- namingserver 依赖 `org.apache.seata:seata-console`，因此 namingserver 发布包通过 `seata-console` jar 获得 Console 静态资源。
- namingserver 对匹配 `^/api/.*/console/.*` 的请求启用 Console 转发，并依据 `x-seata-namespace`、`x-seata-cluster` 或 `vgroup` 选择控制端点转发到 server；转发前置条件是 namespace 非空且 cluster 或 vgroup 至少一个有效。
- 旧登录链路调用 `POST /api/v1/auth/login`，请求体为 `username` 与 `password`；成功响应中的 `data` 写入 `localStorage` 的 `Authorization`，后续请求从该 key 读取并注入 `Authorization` header，401/403 跳转 `#/login`。
- server console API 当前路径包含 `/api/v1/console/globalSession/**`、`/api/v1/console/branchSession/**`、`/api/v1/console/globalLock/**`。
- naming API 对外路径包含 `/api/v1/naming`，当前前端调用命名空间、集群、分组相关相对路径并通过请求工具拼接。
- `distribution/LICENSE-namingserver` 已包含旧前端依赖条目，`distribution/NOTICE-namingserver` 对旧前端关键词未命中。

## 用户需求

### R1：完整重建前端工程

必须替换旧 webpack/Alifd/React 16 体系，重建为 Vite + React + TypeScript + Ant Design 工程。实现应保留现有页面能力：登录、概览、全局事务、全局锁、集群管理、Saga state machine designer iframe 入口、语言切换、命名空间与集群上下文。

### R2：Node/npm 与依赖升级

目标 Node/npm 必须固定为 Node.js v24.11.1 与 npm 11.6.2，不得继续使用 Node v19.5.0/npm 9.3.1 作为目标。`console/pom.xml` 的 `frontend-maven-plugin` nodeVersion、`package.json engines`、本地验证命令和 Task 01/16 验收记录必须保持同一精确版本；Maven 前端依赖安装必须使用 `npm ci`。

### R3：后端接口兼容

不得修改 Java 后端 API 路径、HTTP 方法、参数语义或响应语义。前端请求适配层必须继续兼容：

- `POST /api/v1/auth/login`，请求体 `username`、`password`，成功响应 `data` 作为 `Authorization` localStorage 值
- `/api/v1/console/globalSession/query`
- `/api/v1/console/globalSession/deleteGlobalSession`
- `/api/v1/console/globalSession/forceDeleteGlobalSession`
- `/api/v1/console/globalSession/stopGlobalSession`
- `/api/v1/console/globalSession/startGlobalSession`
- `/api/v1/console/globalSession/sendCommitOrRollback`
- `/api/v1/console/globalSession/changeGlobalStatus`
- `/api/v1/console/branchSession/deleteBranchSession`
- `/api/v1/console/branchSession/forceDeleteBranchSession`
- `/api/v1/console/branchSession/stopBranchSession`
- `/api/v1/console/branchSession/startBranchSession`
- `/api/v1/console/globalLock/query`
- `/api/v1/console/globalLock/delete`
- `/api/v1/console/globalLock/check`
- `/api/v1/naming/namespace`
- `/api/v1/naming/clusterData`
- `/api/v1/naming/addGroup`
- `/api/v1/naming/changeGroup`

### R4：请求头与转发链路兼容

Console API 请求必须继续携带 `x-seata-namespace` 与 `x-seata-cluster`。若页面仍支持按 vgroup 选择控制端点，适配层必须保持与 `ConsoleRemotingFilter` 一致的 header/query 传递行为。namingserver 静态资源、Console API 转发、server controller 三段链路必须在设计中保持边界清晰。

namespace 为必填上下文；cluster 与 vgroup 至少一个有效。cluster 模式下发送 `x-seata-cluster`，vgroup 模式下即使 cluster 为空也必须保留 query 参数 `vgroup` 并允许 Console API 请求继续发出，不得因 cluster 为空在前端阻断。缺失 namespace 或 cluster/vgroup 同时缺失时，页面必须在请求前给出字段级错误或页面 `Alert`。

### R5：路由兼容

必须保留 HashRouter 与现有 URL 路由入口，避免破坏已收藏的 Console URL。新路由可以重构内部组件层级，但不得要求后端新增 HTML fallback 或改静态资源 servlet 行为。

旧 hash URL 兼容映射必须显式实现并纳入验收：`#/` redirect 到 `#/transaction/list` 对应的新全局事务页；`#/transaction/list`、`#/globallock/list`、`#/cluster/list`、`#/sagastatemachinedesigner`、`#/login` 均必须映射或 redirect 到新路由。实现不得仅保留新路由而让旧 URL 404 或空白。

### R6：Ant Design 控制台体验

页面必须是后台工具，不做营销页。默认使用 Ant Design 的 `Layout`、`Menu`、`Table`、`Form`、`Modal`、`Drawer`、`message`、`Result`、`Skeleton`、`Empty`、`Descriptions`、`Tabs`、`Popconfirm` 等官方组件。所有主页面必须具备 loading、empty、error、success 与 partial 状态；关键交互必须可通过语义化文本、role、label 或稳定 `data-testid` 被 Playwright 定位。

### R7：静态资源与 Saga iframe

必须保留 Saga state machine designer iframe 入口，继续让现有静态资源可从 Console 页面访问。Vite 构建后必须保持 `console-fe/dist` 作为 Maven 复制输入，不能把构建产物改到 Maven 未复制的目录。

Saga designer 的真实源为 `saga/seata-saga-statemachine-designer/dist`。构建流程必须在 `npm run build` 内可复现地复制该目录到 Console 前端构建可分发路径，并将旧 `designer.html` 入口重命名或映射为 `dist/saga-statemachine-designer` 下 iframe 可加载的入口。旧 `console-fe/.gitignore` 忽略 `/public/saga-statemachine-designer/`，实现不得依赖提交该忽略目录中的人工复制产物。

### R8：License 验收

前端直接依赖、传递依赖、构建依赖与发布包中真实分发的前端依赖必须完成 license 审计。本轮 license 口径来自用户最新纠正，precedence=user_corrected：不得选择 GPL、AGPL、LGPL、MPL、EPL、CDDL、SSPL、Commons Clause、Business Source License 等强 copyleft、弱 copyleft、source-available 或 copyleft-like 许可证组件；优先 permissive license。允许 MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license。unknown、missing、custom、SEE LICENSE IN 未解析结果必须阻断实现并更换依赖或回 PM 裁决。

devDependencies 必须参与 license 审计并记录结果；但是否写入 `distribution/LICENSE-namingserver` 与 `distribution/NOTICE-namingserver` 以实际是否随 namingserver 发布包或静态产物分发为准。未随产物分发的构建期工具不得误加入运行分发 license 条目。

### R9：安全验收

必须基于 lockfile 执行安全审计。`npm ci` 后执行 `npm audit --audit-level=low`，不得保留前端相关已知漏洞。若 audit 报告来自 dev-only 构建链，也必须修复、替换依赖或回 PM 裁决。

实现必须补充安全敏感验收：全仓静态扫描拒绝 `dangerouslySetInnerHTML`、`.innerHTML`、`.insertAdjacentHTML`；恶意 HTML payload 必须以文本渲染；token 不得出现在 DOM、日志、错误提示、URL 或 Playwright trace；Console 与 Naming 请求必须断言 header、query 与 path 没有串路；Playwright 必须覆盖 success、partial、network failure、console error 与窄屏视口。

### R10：分发 license/notice 同步

必须准确更新 `distribution/LICENSE-namingserver` 与 `distribution/NOTICE-namingserver` 中前端相关第三方 license/notice 条目，并删除不再使用的旧前端依赖条目。需要同步维护 `distribution/licenses` 下随 LICENSE 引用的前端许可证文本；未使用的旧前端 license 文本必须删除或从分发引用中移除。

## 非目标

- 不修改 server 或 namingserver 的 Java API 路径、HTTP 方法、参数语义、权限语义或响应结构。
- 不引入新的 Console 后端能力，不新增数据库 schema，不修改 Kafka 或事务协议。
- 不把 Console 做成营销页、门户页或独立产品站。
- 不把未随 namingserver 发布包分发的构建期工具加入运行分发 license 条目。
- 不保留旧 Alifd、Aliyun Console Components、jQuery、moment、styled-components、webpack 构建链，只为兼容旧代码而继续引入。

## 接口兼容边界

### Console API

Console API 仍由 server controller 提供，namingserver 只对 `/api/*/console/*` 进行转发。前端可以重写 API 适配层，但必须保持现有请求路径、方法与 header 契约。

### Naming API

Naming API 仍由 namingserver 提供，前端集群管理页面必须继续使用 `/api/v1/naming` 路径族。实现不得将 naming API 请求误走 ConsoleRemotingFilter。

### 静态资源链路

前端构建产物必须进入 `console-fe/dist`，再由 `console/pom.xml` 复制到 `target/classes/static`。namingserver 发布包通过 `seata-console` 依赖获得这些资源。Saga designer 资源必须继续可被 iframe 加载。

## License 与 Security 验收细则

- `package-lock.json` 必须随 `package.json` 同步更新，不能只改 manifest。
- 必须提供可复现 license 审计脚本，例如 `node scripts/check-frontend-licenses.mjs`，结合 `package.json` dependencies/devDependencies、源码 import 图、Vite/Rollup bundle manifest 或 metafile、构建产物引用分析，以及安装后的 package metadata，生成生产依赖、构建依赖、分发依赖三类清单。仅依赖 lockfile 或 `node_modules` metadata 不足以判定 runtime 分发依赖。
- license 审计脚本必须解析 SPDX 表达式，不得只做字符串包含判断；表达式中任一 token 命中 GPL、AGPL、LGPL、MPL、EPL、CDDL、SSPL、Commons Clause、Business Source License、copyleft-like 或 source-available 许可证时必须阻断。
- license 审计脚本必须允许 MIT、Apache-2.0、BSD-2-Clause、BSD-3-Clause、ISC、0BSD 等 permissive license，且拒绝 missing、unknown、custom、SEE LICENSE IN 未解析结果。
- 必须生成 `allDependencies`、`runtimeDependencies`、`noticeRequirements` 三个稳定输出：`allDependencies` 覆盖 dependencies、devDependencies 与传递依赖；`runtimeDependencies` 只包含实际进入浏览器 bundle 或静态分发产物的依赖；`noticeRequirements` 只包含真实需要随分发携带 NOTICE 的依赖。`distribution/LICENSE-namingserver`、`distribution/NOTICE-namingserver` 与 `distribution/licenses` 必须与 `runtimeDependencies`、`noticeRequirements` 做双向映射校验，任何一侧多余或缺失都必须失败。
- 必须删除 `distribution/LICENSE-namingserver` 中不再使用的旧前端条目，例如旧 Alifd、旧 Alibaba Console Components、旧 React 16、jQuery、moment、styled-components、webpack/babel 旧链路中不再被分发的条目。
- `distribution/NOTICE-namingserver` 只加入真实依赖要求携带的 notice；若新前端依赖无 notice 要求，应保持无多余前端 notice 条目。
- 必须执行 `npm audit --audit-level=low`，输出为 0 个漏洞才可进入 Maven 集成验证。

## 验收标准

- Spec Bundle 三件套存在于 `spec/console-fe-ant-design-rebuild-2026-04-29/`，且不包含占位符。
- 前端工程使用 Vite + React + TypeScript + Ant Design，目标 Node/npm 不再是 Node 19/npm 9。
- HashRouter 与现有 URL 路由保留。
- 旧 hash URL 映射完整：`#/`、`#/transaction/list`、`#/globallock/list`、`#/cluster/list`、`#/sagastatemachinedesigner`、`#/login` 均进入对应新页面或兼容 redirect。
- 登录兼容完整：`POST /api/v1/auth/login` 使用 `username/password`，成功响应 `data` 写入 `localStorage.Authorization`，后续请求注入 `Authorization` header，401/403 跳转 `#/login`，logout 清理 localStorage 与默认 Authorization 注入状态；登录 UI 不暴露 token、Bearer、内部 host 或 API 实现细节。
- `/api/v1/console/**`、`/api/v1/naming/**`、`x-seata-namespace`、`x-seata-cluster` 契约保持兼容。
- vgroup/cluster 转发契约保持兼容：namespace 必填，cluster 或 vgroup 至少一个有效；vgroup 模式保留 query `vgroup`，不因 cluster 为空阻断 Console API。
- Saga designer iframe 入口与静态资源复制语义保留。
- 生产依赖、构建依赖和发布包依赖 license 审计均通过，不包含 GPL/AGPL/LGPL/MPL/EPL/CDDL/SSPL/Commons Clause/Business Source License、source-available、copyleft-like、unknown、missing、custom 或未解析 SEE LICENSE IN 结果；license 算法可复现地产出 `allDependencies`、`runtimeDependencies`、`noticeRequirements` 与分发 LICENSE/NOTICE 双向一致性结果。
- `npm audit --audit-level=low` 通过，无前端相关已知安全漏洞。
- XSS、token 泄漏、Console/Naming network header/path、Playwright success/partial/network/console error/窄屏链路均有自动化验收。
- `distribution/LICENSE-namingserver`、`distribution/NOTICE-namingserver` 与 `distribution/licenses` 准确反映新前端真实分发依赖，旧未使用前端条目已删除。
- 浏览器主链路通过 Playwright 验证，至少覆盖登录页、全局事务查询、全局锁查询、集群管理、Saga iframe 入口、错误态与空态。
- Maven 最小集成验证通过，推荐命令为 `mvn -pl console -am package`；最终合并前按仓库事实执行 `mvn clean install -T4C`。

## Traceability

| 用户需求 | Spec 条目 | 实现任务 | 验证 |
| --- | --- | --- | --- |
| 完整重建 Console 前端 | R1、R2、R6 | Task 01-12 | `npm ci`、`npm run typecheck`、`npm run build`、Playwright 主链路 |
| 保持后端接口兼容 | R3、R4、R5 | Task 04、Task 05、Task 08-12、Task 17 | API contract mock、浏览器请求检查、controller 路径对照、旧 hash URL 检查 |
| 保留静态资源和 Saga iframe | R7 | Task 03、Task 12、Task 16 | `npm run build`、静态产物检查、Playwright iframe 检查 |
| license 避开 copyleft/source-available 且允许 permissive | R8、R10 | Task 14、Task 15 | `node scripts/check-frontend-licenses.mjs --all`、runtimeDependencies/noticeRequirements 与分发 license 双向 diff review |
| npm audit 与安全敏感路径无漏洞 | R9 | Task 09、Task 14、Task 17 | `npm audit --audit-level=low`、HTML 注入静态扫描、恶意 payload 文本渲染、token 泄漏扫描、Playwright network/console/窄屏检查 |
| Maven 集成与 namingserver 分发链路 | R7、R10 | Task 16、Task 17 | `mvn -pl console -am package`、发布包静态资源检查 |

## Spec 自检

- 占位符扫描：通过，本文档未保留禁用占位表达。
- 内部一致性：通过，Node/npm、接口兼容、静态资源、license/security 与分发要求未互相冲突。
- Scope decomposition：通过，本轮只覆盖 Console 前端完整重建及其分发 license/notice 同步；Java API、数据库、事务协议不纳入实现。
- 歧义消解：通过，路线固定为完整重建；默认技术栈固定为 Vite + React + TypeScript + Ant Design；license 默认口径按用户最新纠正确认为 precedence=user_corrected，允许 permissive license，阻断 copyleft、source-available、copyleft-like 与 unknown/missing/custom/未解析 SEE LICENSE IN。