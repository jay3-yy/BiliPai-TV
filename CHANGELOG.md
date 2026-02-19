# Changelog

## [6.0.3] - 2026-02-18

### ✨ New Features (新增功能)

- **平板端视频详情重设计（Stage + Side Curtain）**:
  - 点击视频后改为“舞台式播放器 + 侧幕式内容”布局，强化平板交互层次与沉浸感。
  - 侧幕支持 `PEEK / OPEN / HIDDEN` 状态切换，并提供窄态快捷入口（评论/相关推荐）。
  - 引入按屏宽自适应策略，覆盖主流平板尺寸区间，避免固定尺寸硬编码。

### 🛠 Improvements & Fixes (优化与修复)

- **播放器卡死恢复链路增强**:
  - 新增错误恢复策略与生命周期播放策略，按错误类型执行差异化重试。
  - 解码异常场景支持自动回退编码重载，降低“起播失败后卡死”概率。
  - 补充播放错误日志与生命周期判定细节，便于后续排障。
- **平板详情页信息区结构优化**:
  - 播放器下方区域收敛为“互动按钮 + 视频简介”，移除冗余推荐速览。
  - 该区域背景改为纯白，提升信息对比与视觉一致性。
- **互动按钮可用性修复**:
  - 点赞图标改为更常见样式，修复“大拇指过细”观感问题。
  - 点赞交互改为稳定单按钮实现，修复点赞后偶发闪动。
  - 投币按钮文案改为“投币/已投币”，修正文字与语义。
  - 收藏图标切换为常用五角星样式。
- **侧幕交互策略调整**:
  - 取消“展开后自动收起”，改为仅由用户手动控制，避免操作被打断。

### ✅ Tests (测试)

- 新增/更新测试:
  - `TabletCinemaLayoutPolicyTest`
  - `PlayerErrorRecoveryPolicyTest`
  - `PlayerLifecyclePlaybackPolicyTest`

### 📦 Release

- **Version Bump**: Updated app version to `6.0.3` (`versionCode` `74`).

## [5.3.4] - 2026-02-15

### ✨ New Features (新增功能)

- **今日推荐单卡片交互升级**:
  - 新增收起/展开能力，收起时仅保留轻量头部，不再自动跟随首页推荐流同步重算。
  - 新增“刷新”入口，支持只刷新推荐单（不触发首页推荐流下拉刷新）。
  - 手动刷新会优先消耗当前预览队列，快速“换一批”。
- **播放结束行为可选**:
  - 播放设置新增“选择播放顺序”：`播完暂停` / `顺序播放` / `单个循环` / `列表循环` / `自动连播`。
  - 针对“稍后再看”场景优化：从列表进入播放时默认按顺序队列运行，看完一条可无缝接下一条。
  - 横屏/竖屏播放器内新增“播放顺序”快捷入口，可直接弹出同款五选项面板切换。

### 🛠 Improvements & Fixes (优化与修复)

- **竖屏链路稳定性修复**:
  - 修复竖屏滑到新视频后，进入 UP 主页（或搜索）返回时内容回退到首视频的问题。
  - 竖屏“简介”面板内点击 UP 头像现在可直接进入 UP 空间，返回后保持竖屏视频上下文。
  - 竖屏会话内播放结束改为由竖屏分页接管自动续播，自动下滑到下一条后起播，避免主链路抢占导致的“仅音频无画面/无法继续滑动”问题。
  - 竖屏恢复时新增初始页定位策略，优先回到上次浏览的 bvid（命中推荐队列时）。
- **播放顺序面板与动作栏视觉修正**:
  - 下调播放器内“选择播放顺序”面板选项字号，避免弹窗文字过大挤占视线。
  - 统一详情页操作栏五列按钮槽位与基线，修复“点赞（大拇指）一列”与其余列视觉不齐的问题。
- **播放器顶栏与弹幕可读性修复**:
  - 非全屏播放器左上角快捷入口由“画质”改为“弹幕开/关”，避免与底部画质入口重复。
  - 指令型弹幕新增可读性过滤：仅展示可读文本提示，自动忽略 `upower_state` 等结构化 payload，修复黄色乱码弹幕问题。
- **图片预览开关动画重构（iOS 风格）**:
  - 重做图片预览打开/关闭动画阻尼与速度曲线，改为更贴近 iOS 的顺滑回收手感。
  - 过渡期间圆角改为恒定策略，移除“中途圆角渐变”过程，避免视觉抖动与形变感。
  - 关闭阶段移除过冲回弹，改为单段收拢到源位置，降低违和感。
  - 图片预览改为非 Dialog 全局 Overlay Host，避免深层列表项内弹窗导致的返回手势失效。
  - 新增预测性返回手势联动：支持边滑边预览退出进度，手势取消时平滑回弹，退出过程可中断。
  - 根据反馈取消图片预览“可打断跟手”返回动画，统一为固定退场动画，避免中途停留和手势状态不一致。
  - 修复预测返回取消时偶发停留在中间态（卡住不回位）的问题，新增常规 Back 兜底与回弹收敛保障，避免手势穿透回到首页后预览层残留。

### 📦 Release

- **Version Bump**: Updated app version to `5.3.4` (`versionCode` `70`).

## [5.3.3] - 2026-02-14

### 🛠 Improvements & Fixes (优化与修复)

- **Bangumi 时间线稳定性修复**:
  - 在 `BangumiTimelineScreen` 中将列表 key 调整为 `seasonId + episodeId` 复合 key，避免 `episodeId` 重复时导致的列表崩溃问题。
- **文案一致性修正（Watch Later）**:
  - 全局修正“稀后再看”为“稍后再看”，统一了侧边栏、底部导航与相关注释文案。
- **竖屏播放器交互完善**:
  - 顶栏去除重复的三点菜单入口，避免与底部“简介”功能重叠。
  - 横竖屏切换入口下移至右下角操作区，单手触达更顺手。
  - 竖屏支持长按倍速播放（松手恢复原速），并增加实时倍速反馈。
  - 竖屏分享文案补齐视频标题（格式：`【标题】+ 链接`）。
- **首页顶栏视觉比例微调**:
  - 顶栏选中背景（胶囊）整体缩小，缓解“色块偏大”观感。
  - 顶栏文字字号小幅上调（仍低于旧版体量），提升可读性与对比平衡。
  - 关键尺寸改为集中 token 策略，后续可快速回滚/AB 调整。
- **首页顶栏自定义（显示/隐藏/排序）**:
  - 新增顶部标签顺序与可见项持久化配置（DataStore）。
  - 设置页支持顶部标签显隐与排序（`推荐`固定显示），并支持一键重置。
  - 首页顶部标签映射改为按“当前配置列表”驱动，移除对固定索引的依赖。
  - 直播入口路由改为按标签语义判断，避免自定义排序后点击错位。
  - 修复顶栏配置热更新时 `HorizontalPager` 可能发生的越界崩溃（索引保护 + 页码钳制）。
- **今日推荐单可用性优化**:
  - 点击推荐单视频后会立即从当前队列移除，降低“看完即废”的体感。
  - 已点击条目在后续重建推荐时会被过滤，避免短时间重复回流。
  - 卡片内补充了简短使用说明（点击即移除、下拉可换一批）。
- **竖屏交互继续对齐（P2）**:
  - 修复切换竖屏后“优先显示封面、未直接起播”的问题，改为播放器就绪后直出播放画面。
  - 进一步修正为“首帧渲染后才隐藏封面”，避免竖屏切换瞬间出现黑屏+播放键。
  - 竖屏第一页进度条支持使用进入时进度做初始种子，并在暂停/等待首帧阶段保持进度显示。
  - 竖屏入口改为“进入瞬间进度快照”驱动，避免重组时读取实时位置导致进度条回退。
  - 暂停图标显示条件改为“真实暂停态”判定，缓冲/待播期间不再误显示大播放键。
  - 修复共享播放器模式下的进度回写冲突：竖屏播放期间不再对同一 ExoPlayer 执行周期性回写 seek，避免“首帧重复/画面抽动”。
  - 修复横屏点击“竖屏”路由错误：不再回落到普通详情页，改为直接进入竖屏沉浸播放流（必要时先退出横屏全屏）。
  - 竖屏右下角旋转入口图标由“窗口样式”替换为“屏幕旋转”图标，语义更贴近“切换横屏”。
  - 启动“单 ExoPlayer 复用”重构第一阶段：竖屏页复用主播放器实例，仅切换承载容器，减少进竖屏重拉流与状态丢失。
  - 竖屏底部右下角去重，保留单一横竖屏切换入口，避免双按钮语义重叠。
  - 标题支持直接点击打开“简介 + 推荐”面板，推荐项可直接跳转到竖屏流内对应视频。
  - 竖屏 UP 信息区支持点击进入 UP 主页。
- **首页/稍后再看删除动效加速（参考 Telegram 风格）**:
  - 不感兴趣与稍后再看删除统一切换为“快版”消散预设，减少拖沓感。
  - 缩短粒子动画总时长与波前扩散时间，删除反馈更干脆。
  - 稍后再看页面新增“批量删除”模式（选择/全选/确认删除）。
- **播放加载链路性能优化（针对“加载慢、卡顿”反馈）**:
  - 自动最高画质策略分级：非大会员优先稳定起播档，降低高画质协商失败概率。
  - 新增 APP API 风控冷却（命中 `-351` 后短期跳过 APP API），避免无效重试。
  - DASH 高画质失败后快速回落到 80，减少重试等待。
  - 首帧优先策略：非关键请求延后触发，并限制推荐预加载仅在 Wi-Fi 下执行。
- **发布渠道安全提示增强**:
  - 设置页“关于与支持”新增常驻发布渠道声明卡片（固定展示官方渠道）。
  - 设置页新增“发布渠道声明”入口弹窗，统一展示免责声明文案。
  - 新用户首次打开应用时弹出发布渠道声明（仅首次确认）。
- **UP 主页连续播放链路补强**:
  - 空间页“播放全部”和视频列表点击改为构建外部播放列表（`setExternalPlaylist`），从所点视频开始顺序串联播放。
- **测试补充**:
  - 扩展 `HomeTopCategoryPolicyTest`，覆盖自定义顺序/可见性与兜底逻辑。
  - 扩展 `TopTabLayoutPolicyTest`，覆盖直播路由语义判断逻辑。
  - 新增 `TodayWatchQueuePolicyTest`，覆盖点击消费后的移除与补货判定逻辑。
  - 扩展 `TodayWatchPolicyTest`，覆盖已消费条目过滤逻辑。
  - 新增 `PortraitSharePolicyTest`，覆盖竖屏分享文案拼装规则。
  - 扩展 `PortraitFullscreenOverlayPolicyTest`，覆盖顶栏重复菜单隐藏策略。
  - 扩展 `PortraitPagerSwitchPolicyTest`，覆盖竖屏封面显示策略。
  - 新增 `SpacePlaybackPolicyTest`，覆盖 UP 空间外部播放列表构建与起播索引逻辑。

### 📦 Release

- **Version Bump**: Updated app version to `5.3.3` (`versionCode` `69`).

## [5.3.2] - 2026-02-13

### 🛠 Improvements & Fixes (优化与修复)

- **Top/Bottom Label Alignment Rework**:
  - reduced top-tab selected-state scaling in `图标+文字` mode to remove visual drop/misalignment
  - normalized top-tab icon/text metrics (icon size, line-height, spacing) for consistent optical center
  - adjusted bottom-bar icon+text metrics and baseline to improve icon/title alignment consistency
- **Version Bump**: Updated app version to `5.3.2` (`versionCode` `68`).

## [5.3.1] - 2026-02-13

### ✨ New Features (新增功能)

- **Bangumi Policy Layer**: Added dedicated policy modules to split view logic from screens/viewmodels:
  - `BangumiFollowStatusPolicy`
  - `BangumiModePolicy`
  - `BangumiPlaybackUrlPolicy`
  - `BangumiSeasonActionPolicy`
  - `BangumiUiPolicy`
  - `MyFollowPolicy`
  - `MyFollowStats`
  - `MyFollowStatsDetailPolicy`
  - `MyFollowWatchInsightPolicy`
- **Home Top Category Policy**: Added `HomeTopCategoryPolicy` to centralize top-tab category order/mapping.
- **Mine Drawer Visual Policy**: Added `MineSideDrawerVisualPolicy` for blur/opacity/scrim tuning in one place.
- **Danmaku Settings Policy**: Added `DanmakuSettingsPolicy` for opacity normalization and boundary handling.
- **Watch Later External Playlist Policy**: Added `WatchLaterPlaybackPolicy` to build external queue and start index from clicked item.
- **Top Tab Label Mode Setting**: Added configurable top-tab label mode (`图标+文字 / 仅图标 / 仅文字`) in settings and wired to home header.
- **Playlist Persistence**: Added `PlaylistManager` state persistence/restore (playlist, index, play mode, external-queue flag) and app-start initialization.

### 🛠 Improvements & Fixes (优化与修复)

- **Top Tab Refraction Behavior**: Top indicator now disables refraction when fully stationary; refraction only applies during drag/settle motion.
- **Top Tab Text Clarity**: Removed double-text crossfade rendering that caused ghosting/blur on selected tabs; switched to single-layer color interpolation.
- **Top Tab Icon+Text Layout Polish**: Improved visual placement by changing icon+text style to a cleaner horizontal arrangement.
- **Watch Later Playback Order**:
  - card click now also sets external playlist (not only top-right "play all")
  - queue starts from the clicked item index
  - playback flow no longer falls back to recommended queue unexpectedly in this scenario
- **Search Repository Reliability**: Improved video search fallback path (`all/v2`) and page-info handling/logging.
- **Danmaku Repository Robustness**:
  - refined segment-count fallback policy
  - strengthened thumb-up state resolution and error message mapping
  - improved segment cache safety behavior
- **Eye Protection Logic Cleanup**: Consolidated eye-care decision/tuning/reminder logic into policy helpers for predictable behavior.
- **Bangumi Experience Refinement**:
  - clearer follow-state preload flow
  - safer season-id resolution for action routes
  - cleaner playback-url collection and UI sizing rules
  - improved MyFollow type/stat/watch-insight derivation

### ✅ Tests (测试)

- Added and/or updated unit tests:
  - `BangumiFilterAndSearchTypePolicyTest`
  - `BangumiFollowStatusPolicyTest`
  - `BangumiModePolicyTest`
  - `BangumiPlaybackUrlPolicyTest`
  - `BangumiSeasonActionPolicyTest`
  - `BangumiUiPolicyTest`
  - `MyFollowPolicyTest`
  - `MyFollowStatsPolicyTest`
  - `MyFollowStatsDetailPolicyTest`
  - `MyFollowWatchInsightPolicyTest`
  - `HomeTopCategoryPolicyTest`
  - `MineSideDrawerVisualPolicyTest`
  - `TopTabLayoutPolicyTest`
  - `TopTabLabelModePolicyTest`
  - `TopTabRefractionPolicyTest`
  - `DanmakuRepositoryPolicyTest`
  - `DanmakuSettingsPolicyTest`
  - `EyeProtectionPolicyTest`
  - `LiquidLensProfileTest`
  - `VideoPlaybackUseCaseQualitySwitchTest`
  - `WatchLaterPlaybackPolicyTest`

### 📦 Release

- **Version Bump**: Updated app version to `5.3.1` (`versionCode` `67`).

## [5.3.0] - 2026-02-12

### ✨ New Features (新增功能)

- **Today Watch Plugin (今日推荐单插件)**: Added a new built-in plugin that locally analyzes watch history and builds a daily recommendation queue with two modes:
  - `今晚轻松看`
  - `深度学习看`
- **Today Watch Card UI**: Added a dedicated recommendation card in Home/Recommend with:
  - mode switch chips
  - UP 主榜
  - recommended video queue (with UP avatar/name)
  - per-item explanation tags
- **Local Personalization Stores**:
  - added creator-profile persistence store (`TodayWatchProfileStore`)
  - added negative-feedback persistence store (`TodayWatchFeedbackStore`)
- **Eye Protection 2.0**: Rebuilt eye-care plugin with:
  - three presets (`轻柔 / 平衡 / 专注`) plus DIY tuning
  - real-time settings preview
  - reminder cadence + snooze options
  - richer humanized reminder copy

### 🛠 Improvements & Fixes (优化与修复)

- **Cold Start Discoverability**: Fixed issue where Today Watch card was loaded but often out of viewport on cold start; now applies one-shot startup reveal strategy during startup window.
- **Refresh Toast Lifecycle Fix**: Fixed issue where “新增 X 条内容” hint could remain on screen and not auto-dismiss reliably.
- **Recommendation Signal Upgrade**:
  - fused history completion + recency + creator affinity
  - linked eye-care night signal (shorter, lower-stimulation preference at night)
  - integrated dislike penalties (video / creator / keyword)
  - diversified queue ordering to avoid consecutive same-creator streaks
- **Playback Quality Switching Reliability**:
  - quality options now prioritize actual DASH switchable tracks
  - cache switching now requires exact quality match; falls back to API fetch when missing
  - improved quality-switch toast wording for clearer fallback explanation
- **History Model Enrichment**: Added `author_mid` mapping in history response conversion so creator affinity can be computed accurately.
- **Plugin Registry Update**: Built-in plugin count updated from 4 to 5 by registering Today Watch plugin in app startup.
- **App Icon Switching Fix**: Resolved icon switching errors caused by mismatched Telegram activity-alias names during app startup icon-state sync (`icon_telegram_pink`, `icon_telegram_purple`, `icon_telegram_dark`).

### ✅ Tests (测试)

- Added and verified unit tests:
  - `TodayWatchPolicyTest`
  - `TodayWatchMotionPolicyTest`
  - `TodayWatchStartupRevealPolicyTest`
  - `EyeProtectionPolicyTest`
  - `VideoPlaybackUseCaseQualitySwitchTest`

### 📦 Release

- **Version Bump**: Updated app version to `5.3.0`.

## [5.2.2] - 2026-02-11

### ✨ New Features (新增功能)

- **Danmaku Interaction Callback**: Wired danmaku click callback end-to-end for context menu and interaction extension scenarios.

### 🛠 Improvements & Fixes (优化与修复)

- **Portrait Video Mode Upgrade**: Improved portrait-mode player flow, including playback continuity when swiping between videos, progress synchronization across portrait/landscape transitions, and overlay control consistency.
- **Dynamic Feed UX**: Added dynamic-tab bottom reselect double-tap to top behavior and improved smoothness of Home/Dynamic return-to-top animations.
- **Forwarded Dynamic Images**: Fixed an issue where images inside forwarded dynamics could not be opened for preview.
- **Image Preview Animation Polish**: Unified open/close motion for image preview dialog across entry points, with smoother rounded-corner transitions and spring-like close rebound.
- **Inbox User Info Stability**: Improved reliability of avatar/username resolution in private-message list after repeated entry.
- **Version Bump**: Updated app version to `5.2.2`.

## [5.2.1] - 2026-02-11

### 🛠 Improvements & Fixes (优化与修复)

- **Space Dynamic Navigation**: Fixed an issue where image/text dynamics in personal space could not be opened; now dynamic cards route correctly:
  - video dynamics -> native video detail
  - non-video dynamics -> dynamic detail page (`t.bilibili.com/{id_str}`)
- **Home Double-Tap Stability**: Fixed blank area appearing when double-tapping Home from non-top position with "header auto-collapse" enabled; Home double-tap now restores top header/tabs before scroll/refresh.
- **Liquid Glass Indicator Tuning**: Improved bottom bar indicator geometry in icon+text mode so labels participate in refraction more reliably.
- **Version Bump**: Updated app version to `5.2.1`.

## [5.2.0] - 2026-02-10

### ✨ New Features (新增功能)

- **Top Tabs Style Sync**: Top category tab bar now follows bottom bar style linkage, supporting floating/non-floating, blur, and liquid glass modes with unified visual language.
- **Refraction Upgrade**: Added stronger liquid lens profile for tab/bottom indicators during horizontal slide, with a clearer spherical feel and edge-space warp.
- **Incremental Timeline Refresh**: Added optional incremental refresh for Recommend/Following/Dynamic feeds, preserving old content and prepending only new items.
- **Refresh Delta Feedback**: Added "new items count" prompt after manual refresh and an old-content divider cue in Recommend.

### 🛠 Improvements & Fixes (优化与修复)

- **Top Indicator Geometry**: Refined top indicator size/shape/centering and boundary clamping to prevent clipping and offset drift when sliding.
- **Bottom Indicator Refraction Source**: Fixed cases where icon/text were not clearly refracted by switching to icon-layer backdrop capture.
- **Default Visual Bootstrapping**: Added one-time startup migration to ensure default Home visual settings are enabled on first launch after update:
  - floating bottom bar
  - liquid glass enabled
  - top blur enabled
- **Version Bump**: Updated app version to `5.2.0`.

## [5.1.4] - 2026-02-08

### 🛠 Improvements & Fixes (优化与修复)

- **Playback Fix**: Resolved playback issues in certain scenarios.

## [5.1.3] - 2026-02-08

### ✨ New Features (新增功能)

- **Search Upgrade**: Extended search types and interaction flow (视频/UP/番剧/直播), improved suggestion/discover results, and optimized pagination/loading behavior.
- **Comment Preference**: Added configurable default comment sort preference and synchronized it across comment entry points.
- **Danmaku Plugin 2.0**: Added user ID/hash blocking for danmaku plugins, plus in-play hot refresh when danmaku plugin configs/rules change.
- **Fullscreen Clock**: Added a top time display in landscape/fullscreen overlays.
- **Settings Tips Expansion**: Added more hidden usage tips in the tips page.
- **Version Easter Egg**: Enhanced version-click visual/easter-egg effects and toggles.

### 🛠 Improvements & Fixes (优化与修复)

- **Bottom Bar UX**: Reworked bottom bar visibility rules for top-level destinations and fixed alignment/position issues when tab count changes.
- **Playback Completion UX**: When "Auto-play next" is disabled, playback completion no longer forces intrusive action popups.
- **Background Playback Fix**: Fixed an issue where switching recommended videos inside detail page could dirty lifecycle flags and cause unexpected pause on Home/background.
- **Gesture Anti-MisTouch**: Brightness/volume vertical gestures are now limited to left/right one-third zones; center zone no longer triggers accidental adjustments.
- **Like Icon Unification**: Replaced heart-like visuals with thumb-up style across key interaction surfaces.
- **Comment Logic Reliability**: Fixed missing UP/置顶 comments under some sort modes and improved mixed-source comment loading behavior.
- **Firebase Telemetry Hardening**: Strengthened Firebase Analytics + Crashlytics integration (user/session context, custom keys, screen/event tracing, and error domain reporting).
- **General Stability**: Multiple UI/state synchronization fixes across home, video detail, plugin center, and settings.

## [5.1.1] - 2026-02-07

### ✨ New Features (新增功能)

- **Experimental Features**: Added some experimental features for better user experience.

### 🛠 Improvements & Fixes (优化与修复)

- **System Stability**: Fixed some known issues and optimized layout performance.

## [5.1.0] - 2026-02-06

### 🛠 Improvements (优化)

- **Scrolling Performance**: Optimized list scrolling performance and reduced recomposition overhead.
- **UI Interaction**: Enhanced card press feedback and physics.

## [5.0.5] - 2026-02-05

### ✨ New Features (新增功能)

- **Video Player Optimization**: Narrowed brightness/volume trigger zones in portrait mode to prevent accidental triggers when swiping for fullscreen.
- **AI Summary**: Added support for AI-generated video summaries.
- **Music Identification**: Added support for identifying and searching for BGM in videos.
- **Version Bump**: Updated app version to 5.0.5.

### 🛠 Improvements (优化)

- **Engineering**: Removed mandatory dependency on `google-services.json` for cleaner builds.
- **Tablet Support**: Improved drawer and bottom bar interaction on tablets.
- **Messaging**: Enhanced private message loading and added video link previews.

## [5.0.1] - 2026-02-01

### ✨ New Features (新增功能)

- **Deep Link Support**: Added comprehensive support for Bilibili links (Video, Live, Space, Dynamic). Supports `bilibili.com`, `m.bilibili.com`, `live.bilibili.com`, `space.bilibili.com`, `t.bilibili.com`.
- **Playback Controls**:
  - Added "Loop Single" (单曲循环) mode.
  - Added "Shuffle" (随机播放) mode.
  - Added "Sequential" (顺序播放) mode.
  - Added "Pause on Completion" (播完暂停) logic when auto-play is disabled.
- **Settings**:
  - Fixed "Auto-Play Next" setting synchronization.

### 🐛 Bug Fixes (修复)

- **UI**: Fixed "Share" button in video detail screen not responding.
- **UI**: Renamed "IP属地" to "IP归属地" for consistency.
- **Compilation**: Resolved build errors related to `PlaylistManager` and `PlayMode`.
