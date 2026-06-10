# YuHua服务器 1.21.11 离线技术生存服

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

- `default`：普通玩家，称号 `[玩家]`，有 `/menu`、`/menutool`、`/projects`、`/daily`、`/guild`、`/proposal`、`/projectrewards`、`/projectsubmit`、`/spawn`、`/back`、`/home`、`/sethome`、`/delhome`、`/tpa`、`/tpahere`、`/tpaccept`、`/tpdeny`、`/tpacancel`、`/msg`、`/reply`、`/mail`、`/pay`、`/balance`、`/baltop`、`/afk`、`/warps`、`/warp`、`/rules`、`/ping`、`/list`、`/seen`、`/ignore`、`/ignorelist`、`/helpop`、`/suicide` 等基础 CMI 命令；默认 3 个家，保留死亡 `/back`，同时有 SkinsRestorer 皮肤命令、Residence 领地命令、BetterTeams 基础公会命令和 Quests 基础任务命令。
- `builder`：建筑/创造组，继承 `default`，称号 `[建筑]`，有 `worldedit.*`、飞行、创造模式、上帝、修复、治疗等命令。
- `admin`：管理组，继承 `builder`，称号 `[管理]`，有 `*`、CoreProtect 和 Residence 管理权限。

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

普通玩家没有 WorldEdit、CoreProtect 回滚、创造、免费 `/fly`、飞行充能、给别人飞行、管理、强制传送等破坏性权限。公共地点建议由管理设置成 CMI warp：

```text
/cmi setwarp spawn true
/cmi setwarp nether true
/cmi setwarp end true
```

默认组已预留 `spawn`、`nether`、`end` 三个公共 warp 权限。

## 玩家快捷菜单

已安装 CommandGUI 3.3.0，用于普通玩家快捷菜单。

GUI 插件选型记录见 `docs/research/2026-06-08-gui-plugin-selection.md`。当前结论是：
第一版玩家快捷菜单继续使用 CommandGUI；如果以后要做商店、任务、多页复杂菜单或
Bedrock/Floodgate 对话框，再考虑升级到 CommandPanels。

玩家可用：

```text
/menu
/menutool
/commandgui
/cg
```

- `/menu`：打开快捷菜单。
- `/menutool`：菜单钟丢失时重新领取菜单钟。
- 右键菜单钟：打开和 `/menu` 相同的快捷菜单。

菜单内目前包含：出生点、死亡返回、家、公共地标、传送请求提示、私聊提示、在线奖励、本周项目、每日任务、公会小队、皮肤菜单、服务器规则、联系管理，以及一整行领地新手模块。

领地模块占用菜单第三行：

- `领地怎么圈`：提示木锄两点选区、`/res select vert` 和 `/res create 名字`。
- `领取木锄`：由菜单发放 1 把 Residence 选区木锄。
- `快速选 48x48`：站在建筑中心执行 `/res select 48 3 48`。
- `我的领地`：执行 `/res list`。
- `查看限制`：执行 `/res limits`。
- `给朋友权限`：提示 `build/container/removeall` 的授权命令。
- `删除或改小`：提示 `/res remove 名字` 和 `/res confirm`。
- `圈地命令`：提示木锄选两点或 `/res select 48 3 48` + `/res select vert`。
- `圈地规范`：提示只圈实际使用区域，避免圈公共道路、村庄、地狱门、别人家或大片空地；不会处理时用 `/helpop 内容`。

普通玩家只能使用 `commandgui.use` 和 `commandgui.tool`，没有 `commandgui.reload`、`commandgui.give` 或 bypass 权限。

## 公会项目日活体系

已加入一套游戏内可用的公会项目 / 日活入口，目标是让玩家每天上线能看到公共项目、组队参与、完成轻任务，但不走战斗力、不送飞行、不靠挂机刷贡献。

玩家入口：

```text
/projects
/daily
/guild
/proposal
/projectrewards
/projectsubmit
```

- `/projects`：查看本周官方项目、公会提案位、参与方式和相关入口。
- `/daily`：查看每日/每周 Quests 任务，并列出可接任务。
- `/guild`：查看 BetterTeams 公会/小队用法。
- `/proposal`：查看公会项目提案模板。
- `/projectrewards`：查看允许和禁止奖励。
- `/projectsubmit`：项目负责人提交验收时使用的模板。

本周 v1 默认项目：

- `W1-O1`：主城项目墙 + 公会荣誉墙。
- `W1-G1`：公共红石材料池 v1；如果有更好的公会提案，管理审核后可替换。

插件分工：

- BetterTeams 只用于轻公会身份、成员组织和队聊；队伍传送、队伍银行、队伍箱子、击杀加分、PvP 切换、rankup 和 scoreboard team 接管已关闭或权限禁止。
- Quests 只用于每日/每周轻任务入口；任务完成不会自动算公会贡献，不自动发强奖励。
- CMI CustomText + CustomAlias 提供 `/projects`、`/daily`、`/guild` 等游戏内说明入口。
- CommandGUI 菜单中间一行提供 `本周项目`、`每日任务`、`公会小队` 三个入口。

贡献确认方式仍然是项目负责人名单 + 可见成果 + 截图/坐标/物资记录 + 管理验收。奖励只给项目记录、署名、展示、称号候选和少量非战斗补给；禁止免费飞行、flight charge、神装、高效率工具、创造、WorldEdit、CoreProtect、强制传送和管理权限。

上线后用 `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md` 跑普通玩家冒烟测试，确认 `/projects`、`/daily`、`/guild`、`/quests take 每日项目签到`、`/team create` 可用，并确认 `/flyc`、`/flightcharge`、`/team home`、`/team echest` 等被拒绝。

## CMI 配置

CMI/CMILib/Vault 已作为基础插件启用，并接入 LuckPerms：

- CMI 经济已通过 Vault hook。
- CMI 聊天格式已改成高对比简洁样式：`[玩家/建筑/管理] 玩家名 > 消息`。
- 已清空新手礼包：`plugins/CMI/Kits/Kits.yml`。
- CMI flight charge 模块已关闭，普通玩家不再通过在线时长获得飞行充能，也没有 `/flyc`、`/flightcharge` 或 `/flyspeed` 权限。
- 在线时长奖励保留手动 `/prewards` 领取 20 个幻翼膜；该奖励不计入公会项目贡献。
- 已清空 CMI 默认 Rank/Schedule：`plugins/CMI/Settings/Ranks.yml`、`plugins/CMI/Settings/Schedules.yml`。
- AFK 开启但不踢人、不免伤、不反挂机机器；AFK 时停止 CMI playtime 计时。
- 生物头掉落已开启，玩家头掉落关闭。
- CMI tablist、气泡聊天、默认入服消息、kit、rank、schedule、skin 模块已关闭，避免默认内容干扰；皮肤由 SkinsRestorer 接管。

## 玩家列表

已安装 TAB 6.0.3 Vanilla，专门接管游戏内按 Tab 打开的玩家列表；CMI 的 tablist 模块继续保持关闭，避免两个插件同时写 header/footer 或玩家名格式。

当前配置在 `plugins/TAB/`：

- `config.yml`：启用 header/footer、tablist-name-formatting、scoreboard-teams 和 40 槽 Layout；关闭 bossbar、scoreboard、playerlist objective、proxy-support。
- `groups.yml`：按 LuckPerms 分组显示称号，玩家行格式为 `[生存/建筑/管理] + LuckPerms 称号 + 玩家名 + 延迟`。
- Layout 使用两列槽位：`2-20` 和 `22-40` 显示玩家，`1` 和 `21` 是列标题；超过 38 名在线玩家会显示剩余人数提示。

## 皮肤

已安装 SkinsRestorer 15.12.0，用于离线服恢复和切换玩家皮肤。普通玩家可用：

```text
/skin set 正版玩家名
/skin clear
/skin update
/skins
```

CMI 自带的 `/skin` 已关闭，避免两个插件抢同一个命令。

## 领地保护

已安装 Residence 6.0.1.8，用于普通玩家自助圈地和防熊。Residence 官网下载页提供免费 jar；当前使用 `plugins/Residence6.0.1.8.jar`。

## 普通圈地流程

当前暂时不开放地图网站。`plugins/LeafResidenceWeb-1.0.0.jar.disabled` 和 `plugins/bluemap-5.16-paper.jar.disabled` 保留在目录里，但不会随服务端启动加载。

玩家使用 Residence 原生流程圈地：

- 打开 `/menu`，点 `领取木锄`。
- 用木锄左键点第一个对角，右键点第二个对角。
- 输入 `/res select vert`，让选区覆盖上下高度。
- 输入 `/res create 名字` 创建领地。
- 创建后用 `/res list` 检查。

也可以站在建筑中心点菜单里的 `快速选 48x48`，再输入 `/res select vert` 和 `/res create 名字`。

当前安全限制：

- 普通玩家最多 10 个领地，单个领地 X/Z 最大 128 格。
- Residence 会按配置检查大小、数量、重叠和权限。

旧的 `docs/tools/residence-planner.html` 保留为离线草稿工具；实际给玩家使用时以游戏内 Residence 为准。

当前默认配置：

- 中文语言：`plugins/Residence/config.yml` 的 `Global.Language: Chinese`
- 免费圈地：关闭 Residence 经济、租赁、租金系统，默认组 `BuyCost/SellCost/RenewCost` 全部为 `0.0`
- 普通玩家最多 10 个领地
- 单个领地 X/Z 最大 128 格，Y 轴覆盖 -64 到 320

新手推荐流程：

```text
打开 /menu
点击 领取木锄
左键点第一个对角
右键点第二个对角
输入 /res select vert
输入 /res create 名字
输入 /res list 检查
```

圈地规范：

- 只圈自己的建筑、机器、仓库和明确要施工的边缘。
- 新手第一块地优先用菜单里的 `快速选 48x48`。
- 边界离建筑外 3-8 格通常足够，不要为了“以后可能用到”圈大片空地。
- 不要圈公共道路、公共地狱门、村庄、刷怪塔、公共农场、别人家或别人机器。
- 不要用长条形领地截断道路、河道、矿道或公共通行路线。
- 圈错了用 `/res remove 名字` 再 `/res confirm` 删除，重新按实际范围创建。

普通玩家常用命令：

```text
/res select
/res select 48 3 48
/res select vert
/res create 名字
/res auto 名字
/res list
/res info
/res limits
/res tp 名字
/res set
/res pset 领地名 玩家名 权限 true/false/remove
/res remove 名字
/res confirm
/res unstuck
```

默认使用木锄或 `/res select` 选择范围。快捷菜单的领地模块会提供圈地步骤、木锄、快速 48x48、领地列表、限制查看、权限提示、删除流程和圈地规范。

管理常用命令：

```text
/resadmin
/resreload
/resload
```

旧 GriefPrevention 数据目录 `plugins/GriefPreventionData/` 保留，旧 jar 已改名为 `plugins/GriefPrevention-16.18.7.jar.disabled`，正常启动时不会加载。

## 服务器列表头像和文案

左侧头像不需要插件也能改：根目录的 `server-icon.png` 必须是 `64x64` PNG。本目录已经使用你的 logo 压缩生成了合规图标。

服务器列表文案由 MiniMOTD 接管：

- 配置文件：`plugins/MiniMOTD/main.conf`
- 图标文件：`plugins/MiniMOTD/icons/leaf.png`
- 当前第一行：`YuHua服务器 1.21.11 纯净生存`
- 当前第二行：`纯净服 | 生电友好 | 离线登录 | 跨版本`

`server.properties` 的 `motd=YuHua服务器 1.21.11 Pure Survival` 只是 MiniMOTD 未加载时的兜底。

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
- CommandGUI 3.3.0：玩家快捷菜单和菜单钟入口
- SkinsRestorer 15.12.0：离线服皮肤恢复和切换
- Residence 6.0.1.8：玩家领地保护和防熊
- TAB 6.0.3 Vanilla：两列式玩家列表、玩家称号和延迟显示
- BetterTeams 5.1.2：轻公会/小队身份和队聊
- Quests 5.3.1：每日/每周轻任务入口
- OpenShulk 1.21.x：快捷潜影盒
- JEI Recipe Bridge 1.0.0：JEI 配方同步桥接
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
- 19 个插件被识别并加载。`leafmc-2026.06.07-r3` 已确认 CommandGUI、SkinsRestorer、GriefPrevention、OpenShulk 和 JEI Recipe Bridge 能启动加载。
- NobleWhitelist 已关闭，`nwl status` 显示 `Whitelist state: off`。
- LuckPerms 使用 YAML 存储，`default`、`builder`、`admin` 三组已写入；default 组已补齐普通玩家常用命令。
- CMI 经济成功 hook Vault，CMI 权限识别 LuckPerms。
- AuthMe 成功 hook LuckPerms 和 CMI。
- CoreProtect CE 启用，并初始化 WorldEdit logging。
- MiniMOTD 启用。
- 服务端出现 `Done`。
- 发送 `stop` 后世界正常保存并退出。
- 2026-06-07 20:23 已完成一次短启动验证：CommandGUI 3.3.0 启用成功，`zh_cn.yml` 加载成功，15 个菜单项加载成功，CMI 加载 2 个 custom alias。玩家实际右键 GUI 仍建议上线后用普通测试号确认。
- 2026-06-09 Residence 替换 GriefPrevention 后仅做静态配置检查，本次没有启动服务端验证；上线前需确认 Residence 和 CMILib 正常加载。
- 2026-06-09 领地模块加入菜单后已做 CommandGUI 静态检查：24 个菜单项，最大 slot 26，动态菜单大小 27 格；本次没有启动服务端验证。
- 2026-06-09 LeafResidenceWeb 和 BlueMap 已改为 `.disabled`，当前玩家圈地回到 Residence 原生流程；本次没有启动服务端验证。
- 2026-06-09 TAB 玩家列表已完成 jar 哈希校验和 YAML 静态检查；本次没有启动服务端验证，首次上线后建议用 `/tab reload` 或重启后按 Tab 检查占位符是否全部解析。
- 2026-06-09 公会项目日活体系已完成静态配置：BetterTeams 5.1.2 和 Quests 5.3.1 jar 元数据已检查；默认组权限已收紧；CommandGUI 最大 slot 仍为 26；CMI flight charge 已关闭。本次没有启动服务端验证，首次上线后需按 `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md` 重启确认 BetterTeams、Quests、CMI alias/custom text 和默认玩家权限正常。

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
- `plugins/CommandGUI-3.3.0.jar`：玩家快捷菜单插件
- `plugins/CommandGUI/config.yml`：玩家快捷菜单和菜单钟配置
- `plugins/SkinsRestorer-15.12.0.jar`：皮肤插件
- `plugins/Residence6.0.1.8.jar`：领地保护插件
- `plugins/Residence/`：Residence 中文、免费圈地和默认组限制配置
- `plugins/LeafResidenceWeb-1.0.0.jar.disabled`：暂时禁用的网页领地插件
- `plugins/bluemap-5.16-paper.jar.disabled`：暂时禁用的 BlueMap 插件
- `plugins/TAB-6.0.3-Vanilla.jar`：玩家列表插件
- `plugins/TAB/`：两列式玩家列表配置
- `plugins/BetterTeams-5.1.2.jar`：轻公会/小队插件
- `plugins/BetterTeams/`：轻公会配置，关闭队伍传送、队伍银行、队伍箱子、击杀加分和 scoreboard team
- `plugins/Quests-5.3.1.jar`：任务插件
- `plugins/Quests/`：每日/每周轻任务配置
- `plugins/CMI/CustomAlias/CustomAlias.yml`：`/projects`、`/daily`、`/guild` 等入口
- `plugins/CMI/CustomText/`：项目、提案、奖励边界和验收模板说明
- `plugins/OpenShulk-1.21.x.jar`：快捷潜影盒插件
- `plugins/JEI-Recipe-Bridge-1.0.0.jar`：JEI 配方同步桥接插件
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
cd D:\YuHua服务器\current\leafmc-server
.\scripts\git-pull-update.ps1
.\start-windows.bat
```

如果你已经手动停服并备份，裸跑也可以：

```powershell
git pull --ff-only
```

不要在开服状态下 pull。世界和数据库不进 Git，必须靠备份回滚。
