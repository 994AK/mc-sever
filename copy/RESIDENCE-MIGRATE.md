# Residence 圈地迁移说明

把本目录下的 `plugins/` 复制到服务器根目录，圈地相关文件包括：

```text
plugins/Residence6.0.1.8.jar
plugins/Residence/config.yml
plugins/Residence/flags.yml
plugins/Residence/groups.yml
plugins/Residence/ShopVotes.yml
plugins/Residence/Language/Chinese.yml
plugins/LuckPerms/yaml-storage/groups/default.yml
plugins/LuckPerms/yaml-storage/groups/builder.yml
plugins/LuckPerms/yaml-storage/groups/admin.yml
plugins/CommandGUI/config.yml
plugins/CommandGUI/lang/zh_cn.yml
```

迁移前先停服。迁移后启动服务端，确认 Residence 加载成功，普通玩家应能使用 `/res select`、`/res create`、`/res list`、`/res pset` 等命令。

注意：

- `LuckPerms` 分组文件会覆盖服务器上的同名分组；如果线上已经手动改过权限，优先合并 `residence.*` 权限节点，不要直接覆盖。
- `CommandGUI/config.yml` 会覆盖玩家菜单；如果线上菜单已有改动，优先合并领地相关 slot。
- 没有包含 `plugins/Residence/Save/` 和 `plugins/Residence/Backup/`，避免覆盖服务器已有领地数据。
- 如果服务器还在加载 GriefPrevention jar，需要先停用或改成 `.disabled`，避免两个领地插件同时工作。

Residence jar SHA-256：

```text
2fae1ae5cfb809d58a5536bce4b0f94c598d5143473e823c5ea0a5abe0739f07  plugins/Residence6.0.1.8.jar
```
