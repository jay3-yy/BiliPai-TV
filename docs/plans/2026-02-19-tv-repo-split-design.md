# TV 端代码拆仓设计（当前仓库仅维护移动端/平板端）

- 日期：2026-02-19
- 状态：已评审通过
- 适用仓库：`/Users/yiyang/Desktop/BiliPai`

## 1. 背景与目标

当前仓库同时维护移动端、平板端、TV 端能力，TV 逻辑已横向渗透到 Manifest、导航、UI 焦点、遥控输入、测试与脚本。为降低当前仓库维护复杂度，决定将 TV 端 UI/遥控/Leanback 能力拆出到新仓库独立维护。

本次目标：

1. 当前仓库仅保留移动端与平板端产品能力。
2. TV 端 UI/遥控/Leanback 相关代码从当前仓库移除。
3. 保留“TV token 高画质链路”（登录/签名/刷新/token 播放请求）在当前仓库继续可用。

## 2. 关键约束

1. 不影响现有移动端/平板端核心功能与发布节奏。
2. 不移除高画质 token 能力相关代码路径。
3. 先在新 TV 仓库落地可用基线，再删除当前仓库 TV UI 代码（先迁后删）。

## 3. 方案对比

### 方案 A：一次性切干净（采用）

- 做法：新 TV 仓库先接收当前 TV 基线；当前仓库随后移除 TV UI/遥控/Leanback 与相关测试脚本。
- 优点：边界最清晰，后续维护成本最低。
- 风险：短期改动面大，需要严格回归和分步提交回滚点。

### 方案 B：两阶段迁移

- 做法：先模块化和弃用标记，1-2 个版本后再完全删除。
- 优点：短期风险低。
- 缺点：双轨成本高，过渡期持续带来认知负担。

### 方案 C：最小变更停用

- 做法：先关闭 Leanback 入口，仅禁用不删除 TV 代码。
- 优点：改动最小。
- 缺点：技术债保留，无法达到“当前仓库只维护移动/平板”的目标。

## 4. 范围定义

### 4.1 当前仓库保留

1. TV token 高画质链路相关能力：
- TV 扫码登录生成/轮询 token
- 签名工具与 TV appkey 策略
- token 刷新
- 使用 access_token 的高画质播放请求

2. 代表文件（示例，不限于）：
- `app/src/main/java/com/android/purebilibili/feature/login/LoginViewModel.kt`
- `app/src/main/java/com/android/purebilibili/core/network/AppSignUtils.kt`
- `app/src/main/java/com/android/purebilibili/core/network/TokenRefreshHelper.kt`
- `app/src/main/java/com/android/purebilibili/data/repository/VideoRepository.kt`
- `app/src/main/java/com/android/purebilibili/core/network/ApiClient.kt`

### 4.2 当前仓库移除

1. Manifest TV 入口与 Leanback 适配：
- `uses-feature android.software.leanback`
- `LEANBACK_LAUNCHER`
- TV banner / TV 启动相关声明

2. TV UI/遥控/焦点导航：
- TV 专属焦点策略、DPAD 键控策略、TV 动效修饰器
- UI 中 `isTv/rememberIsTvDevice` 驱动的 TV 分支

3. TV 专项测试与脚本：
- `app/src/androidTest/.../feature/tv/*`
- TV 相关 unit test
- `scripts/tv_*`

### 4.3 新 TV 仓库承接

- 当前仓库被移除的 TV UI/遥控/Leanback 及其测试脚本。
- 后续 TV 端需求与节奏在新仓库独立演进。

## 5. 执行顺序与回滚点

1. 在新 TV 仓库落地基线（tag：`tv-base-from-6.0.3`）。
2. 当前仓库移除 Manifest TV 入口（回滚点：`commit A`）。
3. 当前仓库移除 TV UI/遥控/焦点策略（回滚点：`commit B`）。
4. 当前仓库移除 TV 测试与脚本（回滚点：`commit C`）。
5. 当前仓库收口移动/平板布局策略，清除 dead code（回滚点：`commit D`）。
6. 更新 README/CHANGELOG，明确仓库边界（回滚点：`commit E`）。

## 6. 验收标准

1. 当前仓库 `debug/release` 构建可通过。
2. Home/Search/Settings/Video/Player 在移动端/平板端无 TV 依赖残留。
3. TV token 高画质链路回归通过（登录、签名、刷新、playurl app 请求）。
4. Manifest 不再暴露 Leanback 入口。
5. TV 相关测试脚本已从当前仓库清理。
6. 文档明确“当前仓库仅维护移动端/平板端，TV 在新仓库维护”。

## 7. 风险与缓解

1. 风险：误删高画质 token 代码导致能力回退。
- 缓解：先补充/固定链路保护测试，再删 TV UI。

2. 风险：删 TV 分支时误伤平板大屏策略。
- 缓解：按页面批次收敛并逐批回归。

3. 风险：导航/返回行为回归。
- 缓解：每个删除批次后执行导航回归测试并保留独立回滚提交。

## 8. 非目标

1. 本次不重构业务架构为多应用多模块工程。
2. 本次不改变高画质 token 能力的协议实现。
3. 本次不在当前仓库继续维护 TV 端体验优化。

