---
title: "Minecraft 插件分层测试工作流"
date: 2026-06-12
category: workflow-issues
module: leafmc-server
problem_type: workflow_issue
component: development_workflow
severity: medium
related_components:
  - "testing_framework"
  - "tooling"
applies_when:
  - "修改 LeafGomoku、LeafFriends 或其它 Bukkit/Paper 插件代码"
  - "需要判断是否必须为每次插件改动进游戏手测"
  - "代码涉及 Bukkit API、事件监听、命令注册或插件生命周期"
  - "变更可能依赖真实 Leaf/Paper 服务端、客户端交互或世界状态"
  - "发布 jar 到本地或远程服务器前需要分层验证"
tags:
  - "minecraft-plugin"
  - "paper-api"
  - "leaf"
  - "java-21"
  - "mockbukkit"
  - "logic-tests"
  - "local-server"
  - "in-game-smoke"
---

# Minecraft 插件分层测试工作流

## Context

这个仓库的自写 Paper/Leaf 插件不需要每次改动都先进入游戏验证。`LeafGomoku` 和 `LeafFriends` 已经走了“脚本测试优先”的路线：先用 `javac --release 21`、本地 Paper API 缓存和依赖编译主代码与测试代码，再运行 `src/<plugin>/src/test/java` 下的 main-method 测试类。

当前可用入口是：

```bash
./scripts/test-leaf-gomoku.sh
./scripts/test-leaf-friends.sh
./scripts/build-leaf-gomoku.sh
./scripts/build-leaf-friends.sh
```

本次验证中，`./scripts/test-leaf-gomoku.sh` 和 `./scripts/test-leaf-friends.sh` 都已通过。它们不是完整 Bukkit 运行时测试，而是第一层快速反馈：规则、状态机、布局、存储、请求对象、命令文本格式这类逻辑应尽量在进服前失败。

Session history 里也验证过同一个边界：`LeafGomoku` 的规则、棋盘坐标、布局、统计、SQLite、外观、悔棋、邀请、自定义棋盘尺寸，以及 `LeafFriends` 的好友申请、过期、冷却、黑名单、隐私、传送和 YAML round-trip，都适合先放在脚本测试里跑通。(session history)

## Guidance

推荐把插件验证拆成四层，不把“进游戏点一遍”当第一层测试。

| 层级 | 验证什么 | 何时使用 | 不能证明 |
| --- | --- | --- | --- |
| 纯 Java 脚本测试 | 规则、状态机、服务、数据存储、格式化、几何布局 | 默认先跑 | Bukkit 生命周期、真实插件加载、真实玩家交互 |
| MockBukkit 测试 | mock server/player/plugin、命令、权限、监听器、事件取消、配置加载、GUI 点击的轻量行为 | 代码依赖 Bukkit/Paper API，但不需要真实客户端时 | Paper/Leaf 真实启动、跨插件链路、视觉/手感 |
| 本地 Paper/Leaf 日志验证 | jar 加载、依赖注册、命令注册、配置读取、启动日志无异常 | 构建 jar 后需要确认插件能启用时 | 玩家真实体验和双人流程 |
| 进游戏 smoke | 真实玩家路径、右键落子、GUI、粒子/声音、双客户端、跨插件联动 | 发布前、用户明确要求实测、或改动只能由客户端/世界状态证明时 | 全量逻辑正确性 |

优先把核心逻辑从 Bukkit 层剥离。规则、请求、状态机、数据读写和布局生成能不依赖 `Player`、`Plugin`、`World`、`Event` 就不要依赖，这样可以继续沿用现有脚本测试。

本仓库现有脚本已经覆盖大量第一层测试：

- `LeafGomoku`：`MatchController`、`GomokuRules`、`UndoRequest`、`InviteRequest`、`BoardGeometry`、`RoomLayoutFactory`、`AppearanceCatalog`、`EnvironmentCatalog`、`RoomEnvironmentLayout`、`SqliteDataStore`、`AppearanceUnlockService`、`StatsService`
- `LeafFriends`：`FriendService`、`TeleportRequestService`、`YamlFriendStore`、`FriendCommandFormat`

MockBukkit 是可以引入的中间层，但当前仓库还没有实际落地；文档和计划里不要把它写成既有事实。(session history) 它适合补纯 Java 测试和真实开服之间的空白，例如：

```java
@BeforeEach
void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.load(MyPlugin.class);
}

@AfterEach
void tearDown() {
    MockBukkit.unmock();
}
```

可优先用 MockBukkit 覆盖：

- 插件 `onEnable` 后命令是否注册
- 权限分支是否拒绝或允许命令
- listener 是否取消指定事件
- `InventoryClickEvent` 这类 GUI 点击行为的低成本断言
- 配置文件加载后的默认值和非法配置处理
- `Player` 相关消息、传送请求、在线/离线状态的 mock 行为

不要用 MockBukkit 替代真实服务器验证。Paper 专有事件链、真实聊天插件链路、WorldEdit/PlaceholderAPI 兼容、显示实体动画、粒子/声音、客户端 GUI 手感、两名真实玩家交互，都仍然需要本地服或进游戏 smoke。

如果未来把插件迁到 Gradle，可以再考虑 `xyz.jpenilla.run-paper` 这类 run task 自动拉起测试服；当前仓库已有 shell 脚本，不需要为了测试分层强行迁移构建系统。

## Why This Matters

一改插件就启动服务端、登录客户端、手动点流程，反馈慢且定位成本高。失败时你很难快速判断是规则错、配置错、命令注册错、插件依赖错，还是客户端/世界交互问题。

分层后，失败位置更清楚：

- 纯 Java 测试失败，优先修核心逻辑。
- MockBukkit 测试失败，优先修 Bukkit API 接线、命令、事件或权限。
- 本地服日志失败，优先修 jar、依赖、`plugin.yml`、Java 版本或启动配置。
- 进游戏 smoke 失败，才重点看玩家路径、视觉交互、跨插件联动或真实世界状态。

Session history 里有两个容易踩的坑也要保留：(session history)

- 控制台不能可靠伪装成真实玩家。`join`、右键落子、GUI 点击、双人交互这类 Player-only 行为必须用真实客户端或更接近 Bukkit 的测试层。
- Java class 改动不能靠 `/gomoku reload` 生效。class 变更要重建 jar 并重启服务端；reload 只适合插件自己支持的配置、房间和数据热加载。

## When to Apply

- 新增或修改 `LeafGomoku`、`LeafFriends` 或其它自写 Bukkit/Paper 插件。
- 修改规则、邀请、悔棋、好友、传送、数据存储、命令格式、房间布局。
- 改动涉及 `Player`、`CommandSender`、listener、inventory GUI、权限、`plugin.yml` 或 `onEnable`。
- 构建新的 `plugins/*.jar` 准备放到本地或远程服务器。
- 需要决定“这次是否必须进游戏测”时。

本地 Paper/Leaf 日志验证不要默认启动。只有用户明确要求本地服验证、需要复现运行时问题、或交付要求运行证据时再启动。启动时沿用本仓库稳定方式：`screen` + 低内存参数 + `logs/latest.log`，正常停服，不直接 kill。(session history)

```bash
screen -dmS leafmc-local bash -lc 'MIN_MEMORY=512M MEMORY=2048M ./start.sh'
tail -n 200 logs/latest.log
screen -S leafmc-local -p 0 -X stuff $'stop\n'
```

## Examples

新增一个不依赖 Bukkit API 的规则类时，先加 main-method 测试，再跑：

```bash
./scripts/test-leaf-gomoku.sh
```

修改好友关系、冷却、黑名单或传送规则时，不需要先进游戏；先覆盖 service 层：

```bash
./scripts/test-leaf-friends.sh
```

修改插件入口、命令注册、权限或事件监听时，推荐顺序是：

```bash
./scripts/test-leaf-friends.sh
./scripts/build-leaf-friends.sh
# 如果已经引入 MockBukkit，再跑对应 JUnit/MockBukkit 测试。
# 只有明确需要运行时验证时，才启动本地 Paper/Leaf 看 logs/latest.log。
```

发布前的进游戏 smoke 只挑关键路径：

- `/gomoku` 或 `/menugomoku` 能打开入口。
- 双玩家加入、落子、胜负、悔棋或重置能走通。
- `/friend` 或 `/menufriends` 能完成申请、同意、拒绝、传送、黑名单和隐私开关。
- 控制台和 `logs/latest.log` 没有异常栈。

新增插件测试脚本时，也要同步处理生成目录。当前 `.gitignore` 已覆盖 `src/LeafGomoku/build/` 和 `src/LeafGomoku/build-test/`；新插件应补对应 `build/`、`build-test/` 忽略规则，或在验证后清理，避免测试产物污染提交。(session history)

## Related

- [PaperMC Project setup](https://docs.papermc.io/paper/dev/project-setup/) — Paper 官方插件开发项目设置基线。
- [MockBukkit](https://mockbukkit.org/) 和 [MockBukkit Docs](https://docs.mockbukkit.org/) — Bukkit/Paper 插件 mock 测试框架。
- [jpenilla run-task](https://github.com/jpenilla/run-task) — Gradle 项目里常用的 Paper 测试服运行任务。
- [LeafGomoku runtime smoke test](../../operations/gomoku/2026-06-10-runtime-smoke-test.md) — 本仓库 LeafGomoku 运行时/进游戏验证清单。
- [LeafGomoku MVP plugin plan](../../plans/2026-06-10-001-feat-gomoku-mvp-plugin-plan.md) — 记录纯 Java core 与 runtime smoke 的原始边界。
- [LeafFriends system plan](../../plans/2026-06-11-003-feat-leaf-friends-system-plan.md) — 记录“pipeline tests 不启动 Minecraft server，部署后 runtime smoke”的边界。
- [README](../../../README.md) — 当前插件清单和最近验证记录。
