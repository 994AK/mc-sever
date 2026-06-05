# Changelog

## leafmc-2026.06.05-r1

- 初始化 Leaf 1.21.11 build 158 服务端包。
- 配置 Windows Server 2022 / 8G 内存启动脚本，默认 `-Xms4G -Xmx6G`。
- 安装并配置 AuthMe、LuckPerms、Vault、CMI、CMILib、ViaVersion 套组、CoreProtect、MiniMOTD、Chunky、WorldEdit、NobleWhitelist。
- 关闭 NobleWhitelist 白名单，保留插件以便后续重新启用。
- 配置普通玩家基础命令权限，内置 `default`、`builder`、`admin` 组。
- 优化 CMI 基础配置：清空新手礼包、挂机奖励、默认 Rank/Schedule，开启生物头掉落，保留 AFK 但不踢人。
- 使用服务器 logo 生成合规 `64x64` `server-icon.png` 和 MiniMOTD icon。
- 增加 Git 运维文件：`.gitignore`、`.gitattributes`、`VERSION_MANIFEST.md`、`RUNBOOK-GIT.md`、`scripts/`。
