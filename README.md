# LeafMC 1.21.11 离线技术生存服

这是已经配置好的 Leaf 1.21.11 build 158 服务端目录。目标是：离线模式、登录注册、纯净生存、保守性能优化、不破坏生存和生电玩法，并开启常见技术服需要的破坏性 bug 特性。

Leaf 官网下载页在 2026-06-05 显示 `Leaf 1.21.11 · Build #158` 为 `Latest stable`，下载 SHA-256 为 `4b5fecbf9074e7f0d1b5c9942875777b6351912594e0241492652979d119d2bc`，已与本地核心 jar 校验一致。

## 启动

Windows Server 2022，整机只有 8G 内存时，推荐用 Windows 专用脚本：

```bat
cd /d D:\path\to\leafmc-server
start-windows.bat
```

`start-windows.bat` 默认是：

```text
-Xms4G
-Xmx6G
```

原因是 Windows Server、Java native memory、插件、网络和文件缓存都需要额外内存。不要写 `-Xmx8G`，否则系统内存会被挤满，反而更容易卡顿或崩服。

如果这台机器只跑 Minecraft，没有面板、数据库、网页服务，可以尝试更激进的 6.5G：

```bat
set MIN_MEMORY=6500M
set MEMORY=6500M
start-windows.bat
```

macOS/Linux 启动：

```bash
cd /Users/yangguanghua/Documents/Codex/2026-06-05/1-5/outputs/leafmc-server
./start.sh
```

`start.sh` 当前默认是 `6500M` 起步、`6500M` 上限。需要手动覆盖时可以这样启动：

```bash
MEMORY=6G MIN_MEMORY=4G ./start.sh
```

停止服务端请在控制台输入：

```text
stop
```

## 登录

服务端已关闭正版认证：

- `server.properties`：`online-mode=false`
- `server.properties`：`enforce-secure-profile=false`
- AuthMe：`enablePremium=false`

玩家第一次进入后注册：

```text
/register 密码 密码
```

后续进入登录：

```text
/login 密码
```

白名单当前已下架，NobleWhitelist 的 `whitelist.enabled=false`，普通玩家不需要白名单也能进服。NobleWhitelist 插件仍保留在目录里，后续如果要重新启用，可以改回 `true` 后执行 `nwl reload`。

如果以后重新启用白名单，按名字添加玩家：

```text
nwl add name 玩家名
nwl remove name 玩家名
nwl list
nwl reload
```

## 权限和称号

LuckPerms 已切到 `yaml` 存储，分组文件在 `plugins/LuckPerms/yaml-storage/groups/`。

已内置三组：

- `default`：普通玩家，称号 `[玩家]`，有 `/spawn`、`/back`、`/home`、`/sethome`、`/delhome`、`/tpa`、`/tpahere`、`/tpaccept`、`/tpdeny`、`/tpacancel`、`/msg`、`/reply`、`/mail`、`/pay`、`/balance`、`/baltop`、`/afk`、`/warps`、`/warp`、`/rules`、`/ping`、`/list`、`/seen`、`/ignore`、`/ignorelist`、`/helpop`、`/suicide` 等基础 CMI 命令。
- `builder`：建筑/创造组，继承 `default`，称号 `[建筑]`，有 `worldedit.*`、飞行、创造模式、上帝、修复、治疗等命令。
- `admin`：管理组，继承 `builder`，称号 `[管理]`，有 `*` 和 CoreProtect 管理权限。

给玩家分组：

```text
lp user 玩家名 parent set admin
lp user 玩家名 parent set builder
lp user 玩家名 parent set default
```

单独给玩家称号前缀：

```text
lp user 玩家名 meta setprefix 80 "&6[服主]&r "
```

## CMI 配置

CMI/CMILib/Vault 已作为基础插件启用，并接入 LuckPerms：

- CMI 经济已通过 Vault hook。
- CMI 聊天格式由 LuckPerms 前缀显示称号：`[称号] 玩家名: 消息`。
- 已清空新手礼包：`plugins/CMI/Kits/Kits.yml`。
- 已清空挂机奖励：`plugins/CMI/Settings/PlayTimeRewards.yml`。
- 已清空 CMI 默认 Rank/Schedule：`plugins/CMI/Settings/Ranks.yml`、`plugins/CMI/Settings/Schedules.yml`。
- AFK 开启但不踢人、不免伤、不反挂机机器；AFK 时停止 CMI playtime 计时。
- 生物头掉落已开启，玩家头掉落关闭。
- CMI tablist、气泡聊天、默认入服消息、kit、rank、schedule 模块已关闭，避免默认内容干扰。

## 服务器列表头像和文案

左侧头像不需要插件也能改：根目录的 `server-icon.png` 必须是 `64x64` PNG。本目录已经使用你的 logo 压缩生成了合规图标。

服务器列表文案由 MiniMOTD 接管：

- 配置文件：`plugins/MiniMOTD/main.conf`
- 图标文件：`plugins/MiniMOTD/icons/leaf.png`
- 当前第一行：`LeafMC 1.21.11 纯净生存`
- 当前第二行：`纯净服 | 生电友好 | 离线登录 | 跨版本`

`server.properties` 的 `motd=LeafMC 1.21.11 Pure Survival` 只是 MiniMOTD 未加载时的兜底。

## 跨版本

已安装 ViaVersion 套组：

- ViaVersion 5.9.1
- ViaBackwards 5.9.1
- ViaRewind 4.1.1

用途是让不同 Minecraft 客户端版本尽量能进入 1.21.11 服务端。跨太多版本时仍可能有物品、方块、UI 或协议差异，这是 Via 系列的正常边界。

## 回滚

已安装 CoreProtect Community Edition 23.2，使用 SQLite 本地存储，并已接上 WorldEdit 日志。

常用命令：

```text
co inspect
co lookup u:玩家名 t:1h r:20
co rollback u:玩家名 t:1h r:20
co restore u:玩家名 t:1h r:20
```

回滚前建议先用 `co inspect` 或 `co lookup` 查清楚范围和时间，不要直接大范围 rollback。

## WorldEdit

已安装 WorldEdit 7.4.2。7.4.3 在当前 Java 21 环境下因 class file 版本过高无法加载，所以这里使用已验证可在 Leaf 1.21.11 + Java 21 启动的 7.4.2。

建筑组已经拥有：

```text
worldedit.*
```

## 区块预生成

已安装 Chunky，用于提前生成区块，减少玩家探索时的卡顿。

```text
chunky world world
chunky radius 5000
chunky start
chunky progress
chunky pause
```

下界可用 `chunky world world_nether`，末地可用 `chunky world world_the_end`。

## 技术服特性

已打开 Paper/Leaf 中常见的破坏性技术特性：

- 无头活塞：`allow-headless-pistons=true`
- 永久方块破坏漏洞：`allow-permanent-block-break-exploits=true`
- 活塞复制：`allow-piston-duplication=true`
- 不安全末地传送门传送：`allow-unsafe-end-portal-teleportation=true`
- 绊线钩校验跳过：`skip-tripwire-hook-placement-validation=true`
- Leaf 绊线复制：`allow-tripwire-dupe=true`

没有启用 DAB、刷怪节流、漏斗机制改写、红石非原版实现、随机刻优化等会明显影响生存或生电装置的配置。

## 已安装插件

- AuthMeReloaded 6.0.0 Paper：离线服登录/注册
- PacketEvents 2.12.2：AuthMe 登录前保护能力依赖
- NobleWhitelist 1.2.23：插件白名单，当前关闭
- Chunky 1.4.40：区块预生成
- LuckPerms 5.5.53：权限和称号
- Vault 1.7.4 jar：经济/权限桥接，运行时显示为 Vault 1.7.3-CMI
- CMILib 1.5.9.6：CMI 依赖库
- CMI 9.8.7.7：基础命令、经济、聊天、AFK、生物头
- WorldEdit 7.4.2：创造/建筑工具
- ViaVersion 5.9.1：跨版本协议
- ViaBackwards 5.9.1：旧版客户端兼容
- ViaRewind 4.1.1：更旧客户端兼容
- CoreProtect CE 23.2：日志和回滚
- MiniMOTD 2.2.3：服务器列表文案和图标
- spark：Leaf 随核心提供的性能分析插件

## 可忽略的 warning

因为当前是离线服，启动时会出现 Paper 的 `SERVER IS RUNNING IN OFFLINE/INSECURE MODE`。这是 `online-mode=false` 的预期提示。

NobleWhitelist、PacketEvents、MiniMOTD 可能会因为 GitHub/API/SSL 访问限制输出更新检查 warning。它们不影响插件启用和玩家进服。

CMILib 可能会提示无法下载部分 `Translations/Items/items_*.yml`，这是外部 GitHub 访问失败，不影响 CMI/CMILib 启动。

## 已验证

2026-06-05 18:27 已完成启动验证：

- Java 21 可运行。
- Leaf `1.21.11-158-ver/1.21.11@dfd6281` 启动成功。
- 14 个插件被识别并加载。
- NobleWhitelist 已关闭，`nwl status` 显示 `Whitelist state: off`。
- LuckPerms 使用 YAML 存储，`default`、`builder`、`admin` 三组已写入；default 组已补齐普通玩家常用命令。
- CMI 经济成功 hook Vault，CMI 权限识别 LuckPerms。
- AuthMe 成功 hook LuckPerms 和 CMI。
- CoreProtect CE 启用，并初始化 WorldEdit logging。
- MiniMOTD 启用。
- 服务端出现 `Done`。
- 发送 `stop` 后世界正常保存并退出。

## 关键文件

- `start-windows.bat`：Windows Server 2022 启动脚本
- `start.sh`：macOS/Linux 启动脚本
- `server.properties`：基础服务端配置
- `server-icon.png`：服务器列表头像兜底
- `config/paper-global.yml`：Paper 全局配置
- `config/leaf-global.yml`：Leaf 全局配置
- `plugins/AuthMe/config.yml`：登录插件配置
- `plugins/NobleWhitelist/config.yml`：白名单插件配置
- `plugins/CMI/`：CMI 配置目录
- `plugins/LuckPerms/yaml-storage/groups/`：权限组配置
- `plugins/MiniMOTD/main.conf`：服务器列表文案
- `plugins/CoreProtect/config.yml`：回滚插件配置
- `CHECKSUMS.txt`：核心、插件 jar 和图标的 SHA-256

## Git 运维

这个目录已经补了一套 Git 运维文件：

- `.gitignore`：忽略世界、日志、缓存、数据库、备份包等运行期数据。
- `.gitattributes`：固定 Windows / Linux 脚本换行，并把 jar、png、db、zip 当二进制文件。
- `VERSION_MANIFEST.md`：当前服务端版本、核心、插件和运行策略。
- `CHANGELOG.md`：服务端包变更记录。
- `RUNBOOK-GIT.md`：建仓库、pull、备份、回滚说明。
- `scripts/init-git-repo.ps1`：初始化 Git 仓库。
- `scripts/git-pull-update.ps1`：服务器安全 pull，先备份再 `git pull --ff-only`。
- `scripts/backup-before-pull.ps1`：pull 前备份。
- `scripts/make-checksums.ps1`：重新生成 `CHECKSUMS.txt`。
- `scripts/verify-checksums.ps1`：校验 `CHECKSUMS.txt`。

服务器上推荐这样更新：

```powershell
cd D:\LeafMC\current\leafmc-server
.\scripts\git-pull-update.ps1
.\start-windows.bat
```

如果你已经手动停服并备份，裸跑也可以：

```powershell
git pull --ff-only
```

不要在开服状态下 pull。世界和数据库不进 Git，必须靠备份回滚。
