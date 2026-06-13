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

## 插件开发环境

插件开发已统一到 Gradle Kotlin DSL / Kotlin JVM。首次开发不需要全局安装 Gradle，直接使用仓库里的 Wrapper：

```bash
./gradlew check
./gradlew buildPlugins
```

源码放在 `src/<PluginName>`，构建产物在对应模块的 `build/libs/`；只有执行 `./gradlew :<PluginName>:installPlugin` 或 `./gradlew installPlugins` 时才会复制到根目录 `plugins/`。服务器运行中不要执行安装任务，先停服再覆盖 jar。

完整方案、版本基线和新 Kotlin 插件模板见 `docs/development/minecraft-kotlin-dev.md`。工作区结构和近期交付物索引见 `WORKSPACE.md`，复制包索引见 `copy/README.md`。

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

- `default`：普通玩家，称号 `[玩家]`，主要入口是 `/menu`、`/menuprofile`、`/menuteleport`、`/menugame`/`/menuprojects`、`/menugomoku`、`/menufriends`、`/menutool`；同时保留 `/spawn`、`/back`、`/rt`、`/homes`、`/sethome`、`/tpa`、`/flyc`、`/flightcharge`、`/recycle`、`/fp`、`/projects`、`/daily`、`/rules`、`/helpop` 等玩家侧基础命令。默认 3 个家、每人最多 1 个假人，保留死亡 `/back` 和消耗型 flight charge 飞行；没有 WorldEdit、CoreProtect 回滚、创造、免费 `/fly`、给别人飞行、管理或强制传送权限。
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

普通玩家没有 WorldEdit、CoreProtect 回滚、创造、免费 `/fly`、给别人飞行、管理、强制传送等破坏性权限；但保留消耗型 flight charge 飞行，可用 `/flyc` 开关。公共地点建议由管理设置成 CMI warp：

```text
/cmi setwarp spawn true
/cmi setwarp nether true
/cmi setwarp end true
```

默认组已预留 `spawn`、`nether`、`end` 三个公共 warp 权限。

## 玩家快捷菜单

已安装 DeluxeMenus 1.14.1，用于普通玩家快捷菜单；同时将 PlaceholderAPI 升级并补齐到 2.12.2，作为 DeluxeMenus 的官方依赖/软依赖。CommandGUI 已从活跃插件和配置中删除。

GUI 插件选型记录见 `docs/research/2026-06-08-gui-plugin-selection.md`。玩家主菜单使用 DeluxeMenus；`/menu` 只保留玩家便捷、传送点、游戏功能三类入口。五子棋的 `/menugomoku` 由 LeafGomoku 注册为动态房间大厅，DeluxeMenus 主菜单只负责跳转。

玩家可用：

```text
/menu
/menutool
/menuprofile
/menuteleport
/menugame
/menuprojects
/menugomoku
/menufriends
```

- `/menu`：打开模块化主菜单。
- `/menutool`：菜单钟丢失时重新领取一个菜单钟；右键菜单钟或输入 `/menu` 都可以打开主菜单。
- `/menuprofile`：打开玩家便捷页。
- `/menuteleport`：打开传送点页。
- `/menugame` / `/menuprojects`：打开游戏功能页。
- `/menugomoku`：打开 LeafGomoku 动态房间大厅。
- `/menufriends`：打开 LeafFriends 好友菜单。

菜单主界面使用 27 格布局，第一行是三类入口，第二行保留最常用直达。子菜单只保留三页，底部提供三类之间的横向跳转和回主菜单，不再让玩家在七八个子菜单里层层找功能。

模块入口：

- `玩家便捷`：家、在线奖励、飞行能量、皮肤、好友、私聊、联系管理、菜单钟、规则和屏蔽/邮件提示。
- `传送点`：出生点、死亡返回、公共地标、随机传送、家、设置/删除家、玩家传送请求。
- `游戏功能`：资源回收站、投放回收站、连锁采集说明、五子棋、领地、假人、本周项目、每日任务和任务入口。

配色约定：

- 绿色：玩家便捷、好友、回收站等对玩家直接有利的入口。
- 蓝色：传送、移动、飞行。
- 金色：家、设置家、删除家。
- 黄色：游戏功能、项目、假人、任务。
- 深绿色：领地、连锁采集。
- 紫色：奖励、外观。
- 白色：规则、五子棋等中性入口。
- 红色：风险动作、删除、联系管理。
- 灰色：功能说明；深灰：插件来源；白色命令：实际执行或提示的命令格式。

需要参数的功能不会在菜单里直接执行失败命令，而是点击后发送示例命令提示，例如 `/sethome 家名`、`/tpa 玩家名`、`/helpop 内容`。可以直接执行的功能才做点击执行，例如 `/spawn`、`/back`、`/homes`、`/prewards`。

菜单内的说明类和状态类按钮默认不关闭菜单。所有 active 菜单项都显式配置左键和右键命令；会打开其它插件 GUI 的入口不再先关闭当前菜单，传送类按钮执行命令后才关闭菜单。

随机传送使用 CMI 原生 `/rt`，同时打开 `/rtp` 别名。当前只开放主世界随机传送，范围是以 0,0 为中心的环形 1500-5000 格，避开水、岩浆、树叶和常见海洋/河流生物群系。

普通玩家不需要 DeluxeMenus 管理权限，也不开放 `/dm open`、`/dm reload`、`deluxemenus.admin`、`deluxemenus.open` 或 bypass 权限。

## 好友系统

已加入 LeafFriends 0.1.0，用于纯净服轻社交：好友申请、好友列表、好友资料页、好友私聊、好友上线提醒、黑名单、隐私开关、点击同意传送和可信好友免确认传送。插件不接入经济、物资奖励、飞行、战力、亲密度属性或公会等级。

推荐玩家入口：

```text
/menufriends
/friend gui
```

`/menufriends` 是主入口：好友申请和好友传送请求可以在菜单里左键同意、右键拒绝；点击好友会打开好友资料页；资料页里可以申请传送、填写私聊、允许/取消对方免确认传送、删除好友或拉黑。好友传送请求也会给接收方发送聊天点击按钮，不需要手动输入同意命令。

备用命令：

```text
/friend add <玩家>
/friend accept <玩家>
/friend deny <玩家>
/friend cancel <玩家>
/friend remove <玩家>
/friend list
/friend msg <玩家> <内容>
/friend tp <玩家>
/friend tpaccept <玩家>
/friend tpdeny <玩家>
/friend toggle <requests|teleports|messages|notifications|status> [on|off]
/friend block <玩家>
/friend unblock <玩家>
```

行为边界：

- 好友关系按 UUID 存储，同时保存最近一次玩家名用于显示。
- 普通好友传送必须由对方在聊天按钮、好友菜单或备用命令里同意，不会直接把人拉走。
- 玩家可以在好友资料页允许某个好友免确认传送到自己身边；这个授权只对该好友生效，删除好友或拉黑会自动清除。
- `/friend msg` 只发给好友；菜单会帮玩家填入私聊前缀，具体内容仍由玩家自己输入；离线留言继续使用 CMI 的 `/mail`。
- 玩家可以关闭好友申请、好友传送、好友私聊、上线提醒或隐藏在线状态。
- 黑名单会阻止好友申请、好友私聊和好友传送请求。
- v1 不自动给 Residence 领地授权，不共享家、箱子、飞行、物资或任何战力能力。

管理入口：

```text
/friend reload
```

默认普通玩家只有 `leaffriends.use`；`leaffriends.reload` 和 `leaffriends.admin` 只给管理组/OP。

## 灵魂绑定 / 物品锁

已加入 LeafSoulbind 0.1.0，用于保护重要物品：锁定或绑定后的物品不能被玩家丢到地上，死亡时不会进入地面掉落，会在复活或下次上线后返还。锁定物品仍然可以放进箱子、木桶、潜影盒等容器；但如果容器里有锁定/绑定物品，默认不能直接打掉容器，避免物品间接掉到地上。

默认即使管理员有 `*` 权限，也不会绕过丢弃和容器保护；管理员要处理异常物品时，用 `/soul admin unlock` 解除手持物品锁定/绑定。

玩家命令：

```text
/soul lock
/soul bind
/soul unlock
/soul info
/soul recover
```

- `/soul lock`：锁定手持物品，防止误丢。
- `/soul bind`：把手持物品灵魂绑定给自己；只有本人或管理员可以解除。
- `/soul unlock`：解除手持物品锁定/绑定。
- `/soul info`：查看手持物品状态。
- `/soul recover`：如果死亡返还时背包满了，用这个命令再次取回暂存物品。

可用别名：

```text
/soulbind
/sb
/slock
/sunlock
```

后台 LuckPerms 指令：

```text
lp group default permission set leafsoulbind.use true
lp group default permission set leafsoulbind.lock true
lp group default permission set leafsoulbind.unlock true
lp group default permission set leafsoulbind.bind true
lp group default permission set leafsoulbind.recover true
lp group admin permission set leafsoulbind.admin true
lp group admin permission set leafsoulbind.reload true
lp group admin permission set leafsoulbind.bypass true
```

## 连锁采集 / 农作物工具

已加入 LeafChainHarvest 0.1.0，用于降低重复采集操作：右键成熟农作物收割并自动补种，手持种子在范围内播种，手持锄头批量耕地，手持骨粉在范围内施肥，斧头连锁砍木头，镐子连锁挖矿物。

默认是全关状态：`收集`、`播种`、`施肥` 和所有材料都需要管理员在 `/leafchain menu` 里点亮后才生效。默认目录是保守的：农作物、原木/木头/菌柄/菌核及其去皮变种、矿石和远古残骸会出现在菜单里；石头、深板岩、凝灰岩、地狱岩、泥土、沙子、砂砾、末地石、树叶、木板等基础或建筑方块不在默认目录中。

玩家侧行为：

- 成熟小麦、胡萝卜、马铃薯、甜菜、地狱疣、可可豆右键收割后会延迟 1 tick 原地补种。
- 手持锄头右键草方块、泥土、砂土、土径，可批量变成耕地。
- 甘蔗、竹子、仙人掌只收集上方可收获部分，底部保留。
- 西瓜、南瓜走玩家破坏路径收集。
- 连锁砍树和连锁挖矿都通过 `Player#breakBlock` 执行，保留 Bukkit/Paper 事件、掉落、经验、工具、耐久和其它插件取消能力。
- 默认蹲下时不触发连锁，方便玩家只处理单个方块。

管理入口：

```text
/leafchain menu
/leafchain preset farm
/leafchain preset chain
/leafchain preset all
/leafchain preset off
/leafchain reload
/lch menu
```

开关是两层：管理员先决定“全服允许哪些功能/材料”，玩家再决定“自己要不要启用”。默认两层都是关闭状态。A 玩家执行个人开启不会影响 B 玩家。

玩家不用 GUI，自己执行：

```text
/leafchain self farm
/leafchain self chain
/leafchain self all
/leafchain self off
```

管理员可以直接用预设命令设置全服允许范围：`/leafchain preset farm` 一键允许农作物收集、播种、耕地、施肥和全部农作物材料；`/leafchain preset chain` 一键允许木头和矿物材料；`/leafchain preset all` 全部允许；`/leafchain preset off` 全部关闭。

管理菜单只改全服允许范围：绿色高亮表示允许，红色表示关闭；可以切换 `收集`、`播种`、`耕地`、`施肥`，也可以分别管理农作物、木头、矿物材料。全服开关和玩家个人开关都写入 `plugins/LeafChainHarvest/settings.yml`。如果要让旧服也回到全关状态，需要停服后覆盖这个文件；如果要保留线上已经点亮的全服/个人状态，就不要覆盖它。

Residence 边界：收割、砍树、挖矿走玩家破坏事件；自动补种、播种、耕地和施肥是额外改方块动作，默认要求 Residence 可用并逐点检查 build 权限。如果没有 Residence，这些额外放置/生长动作默认拒绝；只有明确把 `protection.unsafe-fallback-without-residence: true` 打开才会允许无保护降级。

后台 LuckPerms 指令：

```text
lp group default permission set leafchain.use true
lp group default permission set leafchain.crop.collect true
lp group default permission set leafchain.crop.sow true
lp group default permission set leafchain.crop.till true
lp group default permission set leafchain.crop.fertilize true
lp group default permission set leafchain.tree true
lp group default permission set leafchain.ore true
lp group admin permission set leafchain.admin true
lp group admin permission set leafchain.reload true
lp group admin permission set leafchain.menu true
lp group admin permission set leafchain.bypass-limit true
lp group admin permission set leafchain.bypass-consume true
```

上线后按 `docs/operations/leaf-chain-harvest-smoke-test.md` 做冒烟测试。

## 五子棋房间平台

已加入 LeafGomoku 0.1.0，用于主城实体五子棋装置。当前版本支持从 MVP 单棋盘升级为多房间休闲对局平台：管理员可以从当前位置创建房间，玩家加入黑/白席位，观众进入观战点，胜负写入独立统计和排行榜。插件仍不接入经济、物资、飞行、战斗力奖励或赛事报名系统。

玩家入口：

```text
/gomoku gui
/gomoku join [room]
/gomoku spectate [room]
/gomoku leave
/gomoku status [room]
/gomoku stats [玩家]
/gomoku leaderboard [points|wins|winrate]
```

GUI 入口：

- `/menu` 主菜单里的 `五子棋` 直接执行 `/menugomoku`。
- `/menugomoku` 和 `/gomoku gui` 都打开 LeafGomoku 动态大厅，实时列出 `rooms.yml` 里的全部房间。
- 动态大厅使用分页房间卡片，每页最多 28 个房间；点击房间先进入详情页，详情页再明确选择 `加入对局`、`只观战`、`输出状态`。房间进行中、已满或关闭时，`加入对局` 会灰掉，不影响只观战。
- DeluxeMenus 只负责入口和 `%leafgomoku_room_count%` 展示；加入、观战、离开、统计、排行榜和管理动作都回到 `/gomoku` 命令层执行，不绕开权限。
- 房间初始化、刷新和重置会补齐整块安全地板和玻璃外圈；地板、玻璃外圈、棋盘、大屏和发射器都在保护层内，普通玩家不能拆改。参赛者和观众的摔落伤害也会被取消。

管理入口：

```text
/gomoku admin create <room>
/gomoku admin init <room>
/gomoku admin open <room>
/gomoku admin close <room>
/gomoku admin inspect <room>
/gomoku admin refresh <room>
/gomoku admin reset <room> [force]
/gomoku admin release <room> <black|white> [force]
/gomoku admin forfeit <room> <black|white>
/gomoku admin stop <room>
/gomoku admin delete <room> [force]
/gomoku admin reload
/gomoku var <key> [玩家]
```

玩法边界：

- 第一名加入者为黑方，第二名加入者为白方，黑方先手；每个房间独立记录席位、回合、观众和自动重置。
- 玩家加入后会传送到配置里的黑方 / 白方座位，便于直接开始点击棋盘。
- 观众通过 `/gomoku spectate [room]` 进入观战点，不能落子，也不能占用黑白席位。
- 玩家右键点击 15x15 棋盘空格落子，插件校验回合和空位。
- 黑白双方各有一个固定发射器；合法落子会从对应发射器以弧线动画飞向目标格。
- 棋盘格和大屏预览在每次合法落子后同步更新。
- 横、竖、斜任意方向连续五子即胜；满盘无胜方则平局。
- 对局开始、回合、落子和胜负会广播提示；胜利后播放烟花，保留棋盘几秒后自动重置。
- 正常胜利、平局和管理判负会写入 `stats.yml`；管理中止、刷新、强制重置不写胜负积分。
- 普通玩家不能通过手动放置或破坏棋盘/预览区方块改变棋局；需要临时施工时使用 `leafgomoku.admin.bypass-protection`。
- 插件不消耗玩家背包里的方块，也不发放物资、飞行、权限或战力奖励。

`plugins/LeafGomoku/config.yml` 里的 `arena:` 是旧版 `main` 房间迁移源和默认模板。v2 首次启动会生成 `plugins/LeafGomoku/rooms.yml`，后续房间坐标以 `rooms.yml` 为准。推荐由管理站在目标位置执行 `/gomoku admin create <room>`，插件会使用当前世界、当前 Y、X/Z 和朝向生成棋盘、发射器、座位、观众点和预览墙。上线后按 `docs/operations/gomoku/2026-06-10-runtime-smoke-test.md` 做冒烟测试。

## 公会项目日活体系

已加入一套游戏内可用的公会项目 / 日活入口，目标是让玩家每天上线能看到公共项目、组队参与、完成轻任务，但不走战斗力、不送免费飞行、不靠挂机刷贡献。

玩家入口：

```text
/projects
/daily
/guildhelp
/proposal
/projectrewards
/projectsubmit
```

- `/projects`：查看本周官方项目、公会提案位、参与方式和相关入口。
- `/daily`：查看每日/每周 Quests 任务，并列出可接任务。
- `/guildhelp`：查看当前公会/小队暂停说明和替代报名方式。
- `/proposal`：查看公会项目提案模板。
- `/projectrewards`：查看允许和禁止奖励。
- `/projectsubmit`：项目负责人提交验收时使用的模板。

本周 v1 默认项目：

- `W1-O1`：主城项目墙 + 公会荣誉墙。
- `W1-P1`：公共红石材料池 v1；如果有更好的玩家提案，管理审核后可替换。

插件分工：

- BetterTeams 配置保留，但当前玩家侧公会创建、加入、邀请、聊天和公会名展示暂时下架；项目参与先通过项目负责人或 `/helpop 内容` 登记。
- Quests 只用于每日/每周轻任务入口；任务完成不会自动算公会贡献，不自动发强奖励。
- DeluxeMenus 提供 `/menu`、玩家便捷、传送点和游戏功能三类菜单。
- CMI CustomText + CustomAlias 继续提供 `/projects`、`/daily`、`/guildhelp`、`/proposal`、`/projectrewards`、`/projectsubmit` 等文字说明入口。

贡献确认方式仍然是项目负责人名单 + 可见成果 + 截图/坐标/物资记录 + 管理验收。奖励只给项目记录、署名、展示、称号候选和少量非战斗补给；禁止免费 `/fly`、额外 flight charge 奖励、神装、高效率工具、创造、WorldEdit、CoreProtect、强制传送和管理权限。

上线后用 `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md` 跑普通玩家冒烟测试，确认 `/projects`、`/daily`、`/guildhelp`、`/quests take 每日项目签到`、`/flyc`、`/flightcharge` 可用，并确认 `/fly`、`/tfly`、`/team home`、`/team echest` 等被拒绝或不可用。

## CMI 配置

CMI/CMILib/Vault 已作为基础插件启用，并接入 LuckPerms：

- CMI 经济已通过 Vault hook。
- 普通聊天由 SimpleChat 接管，格式为 `LuckPerms 前缀 + 玩家名 > 消息`；普通玩家白名、建筑组青色名、管理组红色名。
- 已清空新手礼包：`plugins/CMI/Kits/Kits.yml`。
- CMI flight charge 模块已开启；普通玩家可用 `/flyc`、`/flightcharge`、`/flyspeed 1-3`，但没有免费 `/fly`、给别人飞行或管理飞行权限。
- 在线 30 分钟自动获得 10000 点 flight charge；手动 `/prewards` 仍可领取 20 个幻翼膜。
- 已清空 CMI 默认 Rank/Schedule：`plugins/CMI/Settings/Ranks.yml`、`plugins/CMI/Settings/Schedules.yml`。
- AFK 开启但不踢人；普通玩家静止 10 分钟自动进入 AFK。AFK 状态免伤、不能对生物造成伤害、不拾取物品/经验，并停止 CMI playtime 计时。
- 生物头掉落已开启，玩家头掉落关闭。
- CMI 的聊天格式监听、hover/click 聊天、tablist、气泡聊天、默认入服消息、kit、rank、schedule、skin 模块已关闭，避免多个插件同时改聊天或玩家列表；皮肤由 SkinsRestorer 接管。
- CMI `shulkerBackpack` 模块已开启，用于蹲下打开潜影盒；OpenShulk 仍保留为快捷潜影盒插件。

## 聊天格式

已安装 SimpleChat 1.2.0 作为聊天格式替代方案。CMI 继续保留 `/msg`、`/reply`、`/mail`、`/helpop` 等命令，但不再接管普通公屏聊天格式。

当前配置在 `plugins/SimpleChat/config.yml`：

- `default`：`%prefix% + 白色玩家名 + > + 白色消息`
- `builder`：`%prefix% + 青色玩家名 + > + 白色消息`
- `admin`：`%prefix% + 红色玩家名 + > + 白色消息`
- 监听优先级为 `HIGHEST`，支持旧式 `&` 颜色码。

## 玩家列表

已安装 TAB 6.0.3 Vanilla，专门接管游戏内按 Tab 打开的玩家列表；CMI 的 tablist 模块继续保持关闭，避免两个插件同时写 header/footer 或玩家名格式。

当前配置在 `plugins/TAB/`：

- `config.yml`：启用 header/footer、tablist-name-formatting、scoreboard-teams 和 40 槽 Layout；关闭 bossbar、scoreboard、playerlist objective、proxy-support。
- `groups.yml`：按 LuckPerms 分组排序，玩家行只显示 LuckPerms 前缀 + 玩家名 + 延迟，避免 TAB 固定组名和 LuckPerms 前缀重复。
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

- 打开 `/menu`，点 `领地保护`，再点聊天页里的 `领取木锄`。
- 用木锄左键点低处角，右键点高处对角。
- 输入 `/res create 名字` 创建领地。
- Y 轴按你选的两个点计算，圈多少就是多少。
- 创建后用 `/res list` 检查。

也可以站在建筑中心点打开 `领地保护` 子菜单，点 `快速选 48x16x48`，再输入 `/res create 名字`。

当前安全限制：

- 普通玩家最多 10 个领地，单个领地 X/Z 最大 500 格，最小 1x1。
- Residence 会按配置检查大小、数量、重叠和权限。

旧的 `docs/tools/residence-planner.html` 保留为离线草稿工具；实际给玩家使用时以游戏内 Residence 为准。

当前默认配置：

- 中文语言：`plugins/Residence/config.yml` 的 `Global.Language: Chinese`
- 免费圈地：关闭 Residence 经济、租赁、租金系统，默认组 `BuyCost/SellCost/RenewCost` 全部为 `0.0`
- 普通玩家最多 10 个领地
- 单个领地 X/Z 最大 500 格，最小 1x1，Y 轴按玩家选区计算

新手推荐流程：

```text
打开 /menu
点击 领地保护
点击 领取木锄
左键点低处角
右键点高处对角
输入 /res create 名字
输入 /res list 检查
```

圈地规范：

- 只圈自己的建筑、机器、仓库和明确要施工的边缘。
- 新手第一块地可用 `领地保护` 子菜单里的 `快速选 48x16x48`。
- 边界离建筑外 3-8 格通常足够，不要为了“以后可能用到”圈大片空地。
- 不要圈公共道路、公共地狱门、村庄、刷怪塔、公共农场、别人家或别人机器。
- 不要用长条形领地截断道路、河道、矿道或公共通行路线。
- 圈错了用 `/res remove 名字` 再 `/res confirm` 删除，重新按实际范围创建。

普通玩家常用命令：

```text
/res select
/res select 48 16 48
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

默认使用木锄或 `/res select` 选择范围。当前玩家菜单的“游戏功能”页提供领取木锄、快速 48x16x48、领地列表、限制查看、授权提示和删除流程入口。

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
- CMI 9.8.7.7：基础命令、经济、AFK、生物头
- SimpleChat 1.2.0：普通公屏聊天格式
- DeluxeMenus 1.14.1：玩家快捷菜单和模块子菜单入口
- LeafMenuTool 0.1.0：右键菜单钟打开 `/menu`
- PlaceholderAPI 2.12.2：DeluxeMenus 占位符和消息处理依赖
- SkinsRestorer 15.12.0：离线服皮肤恢复和切换
- Residence 6.0.1.8：玩家领地保护和防熊
- TAB 6.0.3 Vanilla：两列式玩家列表、玩家称号和延迟显示
- LeafGomoku 0.1.0：主城五子棋房间平台插件
- LeafFriends 0.1.0：好友申请、好友列表、好友私聊、好友传送请求和隐私开关
- LeafRecycle 0.1.0：公共资源回收站和玩家投放入口
- LeafChainHarvest 0.1.0：农作物右键收割补种、播种施肥、连锁砍木头和连锁挖矿物
- BetterTeams 5.1.2（jar 已禁用）：轻公会/小队配置保留，玩家侧功能暂停
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
- 19 个插件被识别并加载。`leafmc-2026.06.07-r3` 曾确认 CommandGUI、SkinsRestorer、GriefPrevention、OpenShulk 和 JEI Recipe Bridge 能启动加载；当前菜单插件已替换为 DeluxeMenus，需按最新菜单冒烟测试重新确认。
- NobleWhitelist 已关闭，`nwl status` 显示 `Whitelist state: off`。
- LuckPerms 使用 YAML 存储，`default`、`builder`、`admin` 三组已写入；default 组已补齐普通玩家常用命令。
- CMI 经济成功 hook Vault，CMI 权限识别 LuckPerms。
- AuthMe 成功 hook LuckPerms 和 CMI。
- CoreProtect CE 启用，并初始化 WorldEdit logging。
- MiniMOTD 启用。
- 服务端出现 `Done`。
- 发送 `stop` 后世界正常保存并退出。
- 2026-06-07 20:23 曾完成一次 CommandGUI 短启动验证；该菜单栈已在 2026-06-10 被 DeluxeMenus 替换。
- 2026-06-09 Residence 替换 GriefPrevention 后仅做静态配置检查，本次没有启动服务端验证；上线前需确认 Residence 和 CMILib 正常加载。
- 2026-06-10 已完成 DeluxeMenus 迁移和五子棋 GUI 接入验证：CommandGUI jar/config 已删除；DeluxeMenus jar、PlaceholderAPI jar、`config.yml` 和 8 个已注册菜单已加载；`/menu` 里的五子棋入口会执行 LeafGomoku 的 `/menugomoku` 动态大厅。
- 2026-06-10 已完成 LeafGomoku 房间平台构建、纯 Java 规则/统计/布局测试和本地重启验证：`scripts/test-leaf-gomoku.sh` 通过，`plugins/LeafGomoku-0.1.0.jar` 已生成；本地服已确认 LeafGomoku、`leafgomoku` PlaceholderAPI expansion、`/menugomoku` 命令、`/gomoku status main` 正常，并已 refresh `main`/`test1` 生成玻璃外圈。
- 2026-06-11 已完成 LeafFriends 点击式好友体验迭代：`scripts/test-leaf-friends.sh` 和 `scripts/build-leaf-friends.sh` 通过，`plugins/LeafFriends-0.1.0.jar` 已生成；本地 Leaf 服已启动到 `Done`，日志确认 `LeafFriends v0.1.0` 启用成功。游戏内仍建议用两个测试账号确认 `/menufriends`、好友资料页、申请/同意/拒绝、聊天点击同意传送、免确认传送、好友私聊、黑名单和隐私开关。
- 2026-06-12 已完成 LeafSoulbind 物品锁/灵魂绑定插件构建：`./gradlew :LeafSoulbind:check :LeafSoulbind:shadowJar` 通过，copy 包在 `copy/plugin-packages/leaf-soulbind-20260612-173142/`；本次没有启动服务端，首次上线后需按 copy 包 README 做游戏内丢弃、容器、死亡返还测试。
- 2026-06-09 LeafResidenceWeb 和 BlueMap 已改为 `.disabled`，当前玩家圈地回到 Residence 原生流程；本次没有启动服务端验证。
- 2026-06-09 TAB 玩家列表已完成 jar 哈希校验和 YAML 静态检查；本次没有启动服务端验证，首次上线后建议用 `/tab reload` 或重启后按 Tab 检查占位符是否全部解析。
- 2026-06-09 公会项目日活体系已完成静态配置：BetterTeams 5.1.2 和 Quests 5.3.1 jar 元数据已检查；默认组权限已收紧；flight charge 后续已恢复为消耗型飞行。本次没有重新启动服务端验证，首次上线后需按 `docs/operations/guild-projects/2026-06-09-runtime-smoke-test.md` 重启确认 BetterTeams、Quests、DeluxeMenus、CMI alias/custom text 和默认玩家权限正常。

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
- `plugins/DeluxeMenus-1.14.1-Release.jar`：玩家快捷菜单插件
- `plugins/PlaceholderAPI-2.12.2.jar`：DeluxeMenus 占位符和消息处理依赖
- `plugins/DeluxeMenus/config.yml`：DeluxeMenus 菜单加载配置
- `plugins/DeluxeMenus/gui_menus/`：模块化玩家主菜单和子菜单配置
- `plugins/LeafMenuTool-0.1.0.jar`：右键菜单钟打开 `/menu`
- `plugins/LeafGomoku-0.1.0.jar`：五子棋房间平台插件
- `plugins/LeafGomoku/config.yml`：五子棋 legacy main 房间迁移源和默认模板
- `plugins/LeafGomoku/rooms.yml`：五子棋房间坐标、座位、观众点和生命周期配置
- `plugins/LeafGomoku/stats.yml`：五子棋玩家统计、排行榜和已记录比赛 ID
- `plugins/LeafFriends-0.1.0.jar`：好友系统插件
- `plugins/LeafFriends/config.yml`：好友申请、好友传送、冷却和 GUI 配置
- `plugins/LeafFriends/friends.yml`：运行时生成的好友关系、隐私设置、黑名单和可信好友免确认传送数据
- `plugins/LeafRecycle-0.1.0.jar`：资源回收站插件
- `plugins/LeafRecycle/config.yml`：回收站清理、投放和提示配置
- `plugins/LeafSoulbind-0.1.0.jar`：物品锁和灵魂绑定插件
- `plugins/LeafSoulbind/config.yml`：物品锁、死亡返还、容器保护和中文提示配置
- `plugins/LeafSoulbind/pending-returns.yml`：运行时生成的死亡后待返还物品暂存数据
- `plugins/SkinsRestorer-15.12.0.jar`：皮肤插件
- `plugins/Residence6.0.1.8.jar`：领地保护插件
- `plugins/Residence/`：Residence 中文、免费圈地和默认组限制配置
- `plugins/LeafResidenceWeb-1.0.0.jar.disabled`：暂时禁用的网页领地插件
- `plugins/bluemap-5.16-paper.jar.disabled`：暂时禁用的 BlueMap 插件
- `plugins/TAB-6.0.3-Vanilla.jar`：玩家列表插件
- `plugins/TAB/`：两列式玩家列表配置
- `plugins/SimpleChat-1.2.0.jar`：普通聊天格式插件
- `plugins/SimpleChat/config.yml`：普通玩家、建筑组、管理组聊天格式
- `plugins/BetterTeams-5.1.2.jar.disabled`：已禁用的轻公会/小队插件
- `plugins/BetterTeams/`：轻公会配置，关闭队伍传送、队伍银行、队伍箱子、击杀加分和 scoreboard team
- `plugins/Quests-5.3.1.jar`：任务插件
- `plugins/Quests/`：每日/每周轻任务配置
- `plugins/CMI/CustomAlias/CustomAlias.yml`：`/menu*`、`/projects`、`/daily`、`/guildhelp` 等入口
- `plugins/CMI/CustomText/`：菜单子页、项目、提案、奖励边界和验收模板说明
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
