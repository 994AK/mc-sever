# 工作区导航

这个仓库同时是 Leaf 服务端目录和本地插件开发工作区。整理原则是：会影响服务端启动的路径保持不动，近期交付物和文档按用途归类。

## 顶层分类

| 位置 | 放什么 | 注意 |
| --- | --- | --- |
| `src/` | 本地自研插件源码和第三方补丁源码 | Gradle 模块路径，不随便改名或移动 |
| `plugins/` | 当前服务端实际加载的 jar、配置和运行态数据 | 当成运行目录，不当草稿目录 |
| `copy/plugin-packages/` | 单个插件的上线复制包 | 给服主/运维按 README 覆盖到服务器 |
| `copy/server-packages/` | 整服级、第三方插件补丁或服务端复制包 | 可能包含多个插件、配置和文档 |
| `copy/announcements/` | 公告、广播命令、上线通知素材 | 不参与构建，只服务运营发布 |
| `docs/` | 需求、计划、调研、操作、复盘和工具文档 | 细分见 `docs/README.md` |
| `scripts/` | 构建和测试入口脚本 | 脚本应包装 Gradle 任务，不复制业务逻辑 |
| `config/` | Paper/Leaf/Gale 等服务端全局配置 | 属于实际服务端配置 |
| `world*`、`logs/`、`cache/`、`libraries/`、`versions/`、`build/`、`.gradle/`、`.tmp/` | 运行态、依赖缓存或构建产物 | 不作为交付物来源 |

## 近期工作索引

| 功能 | 源码 | 测试脚本 | 构建脚本 | 文档 | 交付包 |
| --- | --- | --- | --- | --- | --- |
| LeafGomoku | `src/LeafGomoku/` | `scripts/test-leaf-gomoku.sh` | `scripts/build-leaf-gomoku.sh` | `docs/operations/gomoku/`、`docs/plans/*gomoku*` | 旧包已归档或删除，当前以 `plugins/LeafGomoku-0.1.0.jar` 为运行包 |
| LeafFriends | `src/LeafFriends/` | `scripts/test-leaf-friends.sh` | `scripts/build-leaf-friends.sh` | `docs/plans/*leaf-friends*` | 旧包已归档或删除，当前以 `plugins/LeafFriends-0.1.0.jar` 为运行包 |
| LeafRecycle | `src/LeafRecycle/` | `scripts/test-leaf-recycle.sh` | `scripts/build-leaf-recycle.sh` | 交付包 README | `copy/plugin-packages/leaf-recycle-20260612-162142/` |
| LeafSoulbind | `src/LeafSoulbind/` | `scripts/test-leaf-soulbind.sh` | `scripts/build-leaf-soulbind.sh` | 交付包 README、README 主文档 | `copy/plugin-packages/leaf-soulbind-20260612-173142/`、`copy/plugin-packages/leaf-soulbind-20260613-113045/` |
| LeafChainHarvest | `src/LeafChainHarvest/` | `scripts/test-leaf-chain-harvest.sh` | `scripts/build-leaf-chain-harvest.sh` | `docs/operations/leaf-chain-harvest-smoke-test.md`、`docs/plans/2026-06-12-001-feat-leaf-chain-harvest-plan.md` | `copy/plugin-packages/leaf-chain-harvest-20260612-181936/` |
| FakePlayer 补丁 | `src/FakePlayer/` | 第三方 Maven 工程内执行 | 第三方 Maven 工程内执行 | `docs/operations/fakeplayer-usage.md` | `copy/server-packages/fakeplayer-leaf-1.21.11-server-copy-20260612/` |

## 新东西放哪里

- 新插件源码：放 `src/<PluginName>/`，并在 `settings.gradle.kts` 注册模块。
- 新插件测试/构建入口：放 `scripts/test-<plugin>.sh` 和 `scripts/build-<plugin>.sh`。
- 新上线复制包：单插件放 `copy/plugin-packages/<name>-<timestamp>/`；整服或第三方补丁放 `copy/server-packages/<name>-<timestamp>/`。
- 新公告文案或后台广播命令：放 `copy/announcements/<name>-<date>/`。
- 新操作验证：放 `docs/operations/`，如果属于某个大功能可再建子目录。
- 新需求/执行计划：放 `docs/brainstorms/` 或 `docs/plans/`。
- 新经验沉淀：放 `docs/solutions/`，不要塞进 README。

## 不要随便移动

- `plugins/`、`world/`、`world_nether/`、`world_the_end/` 是实际服务端路径。
- `src/*/build/`、`src/*/build-test/`、`build/`、`.gradle/` 是构建产物。
- `logs/`、`cache/`、`libraries/`、`versions/` 是运行或依赖缓存。
- `copy/` 只保存给人复制/上线的包，不放临时测试产物。
