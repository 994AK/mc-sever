# Changelog

## Unreleased

- 安装 BetterTeams 5.1.2 作为轻公会/小队身份底座；仅开放创建、加入、邀请、队聊、成员管理等基础能力。
- 安装 Quests 5.3.1 作为每日/每周轻任务入口；覆盖默认 RPG 示例任务，不发钻石剑、金钱、经验或战力奖励。
- 新增 `/projects`、`/daily`、`/guildhelp`、`/proposal`、`/projectrewards`、`/projectsubmit` 游戏内入口，配合 CMI CustomText 说明本周项目、提案、奖励边界和验收提交；`/guildhelp` 当前说明公会功能暂停和替代报名方式。
- 快捷菜单重构为模块化主菜单：生存传送、家园与地标、领地保护、项目日活、奖励与外观、玩家社交、规则与帮助，每个独立子菜单都有上一页 / 下一页 / 回主菜单导航和统一配色。
- 生存传送模块新增随机传送入口，开放普通玩家 `/rt` 并启用 `/rtp` 别名；随机传送仅开放主世界范围，避免下界和末地误传送。
- 恢复 CMI flight charge 模块和普通玩家 `/flyc`、`/flightcharge`、`/flyspeed 1-3` 权限；在线 30 分钟自动获得 10000 点 flight charge，但仍不开放免费 `/fly` 或管理飞行。
- 恢复 CMI `shulkerBackpack` 模块，用于蹲下打开潜影盒；OpenShulk 插件继续保留。
- 收紧 BetterTeams 默认权限：禁止队伍传送、队伍银行、队伍箱子、队伍 rankup、击杀加分、PvP 切换、队伍 warp、队伍 home、队伍 anchor 和管理命令。
- 收紧 Quests 默认权限：普通玩家不能使用 Quests 管理、编辑器、条件/动作编辑、排行榜和 compass 入口。
- 安装 TAB 6.0.3 Vanilla，新增两列式玩家列表：顶部 YuHua服务器 标题、底部 TPS/MSPT/内存信息，玩家行显示 LuckPerms 称号和延迟。
- 修正 TAB 玩家列表称号重复问题：玩家行只显示 LuckPerms 前缀，不再额外叠加 TAB 固定组名。
- 安装 SimpleChat 1.2.0 接管普通公屏聊天格式，CMI 保留私聊/邮件/求助等命令但不再接管公屏格式。
- 安装 Residence 6.0.1.8 免费版并停用 GriefPrevention jar，让 Residence 接管玩家领地保护。
- Residence 使用中文语言，关闭经济/租赁费用；普通玩家最多 10 个领地，单个领地 X/Z 上限放大到 128 格。
- 暂时禁用 LeafResidenceWeb 和 BlueMap，圈地回到 Residence 原生木锄/命令流程，不再开放地图网站。
- 保留 `docs/tools/residence-planner.html` 作为离线草稿工具；正式玩家流程使用 Residence 原生命令。
- 领地能力移动到 `/menuland` 独立子菜单，包含圈地步骤、领取木锄、快速 48x48 选区、列表、限制、授权、删除和规范入口。
- 限制每玩家区块生成、加载和发送突发，并开启 Leaf 实体移动包降载，缓解多人探索或传送时的网络发送队列堆积。
- 收紧命令补全发送，减少客户端聊天框输入时的补全负担。
- 关闭 ViaVersion/CoreProtect/Vault/SkinsRestorer 的自动更新外联，并停用 SkinsRestorer Paper 入服即时皮肤监听，减少 TPS 正常时的入服/客户端同步卡顿变量。

## leafmc-2026.06.07-r3

- 安装 CommandGUI 3.3.0，新增玩家快捷菜单。
- 新增 `/menu` 打开菜单，`/menutool` 取回菜单钟；首次进服会发放菜单钟。
- 菜单整合出生点、死亡返回、家、公共地标、传送提示、私聊提示、在线奖励、飞行充能、皮肤、规则和联系管理。
- 给普通玩家开放 `commandgui.use`、`commandgui.tool`、`/menu`、`/menutool` 权限，不开放 CommandGUI reload、give-other 或 bypass 权限。
- `/rules` 改为中文简洁规则，并补充菜单、飞行、奖励和联系管理入口。
- 本版暂不加入领地菜单入口。

## leafmc-2026.06.06-r2

- 安装 SkinsRestorer 15.12.0，解决离线服玩家没有皮肤的问题。
- 关闭 CMI 自带 skin 模块和 `/cmi skin` alias，让 SkinsRestorer 接管 `/skin`。
- 优化 CMI 聊天格式：普通玩家、建筑组、管理组使用更清楚的标签和高对比正文。
- 安装 GriefPrevention 16.18.7，提供玩家自助领地保护。
- 给普通玩家补充皮肤和领地基础权限，禁止普通玩家领地买卖和攻城；给管理组补充 GriefPrevention 管理权限。

## leafmc-2026.06.05-r1

- 初始化 Leaf 1.21.11 build 158 服务端包。
- 配置 Windows Server 2022 / 8G 内存启动脚本，默认 `-Xms4G -Xmx6G`。
- 安装并配置 AuthMe、LuckPerms、Vault、CMI、CMILib、ViaVersion 套组、CoreProtect、MiniMOTD、Chunky、WorldEdit、NobleWhitelist。
- 关闭 NobleWhitelist 白名单，保留插件以便后续重新启用。
- 配置普通玩家基础命令权限，内置 `default`、`builder`、`admin` 组。
- 优化 CMI 基础配置：清空新手礼包、挂机奖励、默认 Rank/Schedule，开启生物头掉落，保留 AFK 但不踢人。
- 使用服务器 logo 生成合规 `64x64` `server-icon.png` 和 MiniMOTD icon。
- 增加 Git 运维文件：`.gitignore`、`.gitattributes`、`VERSION_MANIFEST.md`、`RUNBOOK-GIT.md`、`scripts/`。
