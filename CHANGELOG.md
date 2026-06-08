# Changelog

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
