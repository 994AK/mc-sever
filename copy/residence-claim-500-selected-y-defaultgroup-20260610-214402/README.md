# Residence 圈地修正复制包

把本目录里的 `plugins/` 文件夹复制到服务器根目录，覆盖同路径文件。

包含文件：

- `plugins/Residence/config.yml`
- `plugins/Residence/groups.yml`
- `plugins/LuckPerms/yaml-storage/groups/default.yml`
- `plugins/DeluxeMenus/gui_menus/land.yml`
- `plugins/CMI/CustomText/menuland.txt`
- `LUCKPERMS-COMMANDS.txt`

生效内容：

- 修正 Residence 默认组名为 `default`，匹配 LuckPerms 的默认组。
- Residence 将 `vip`、`builder`、`admin` 映射到同一套 500 限制，避免这些主组掉回低限制。
- 给 LuckPerms 默认组补 `residence.group.default`，避免普通玩家掉回 Residence 内置低限制。
- 普通玩家最多 10 个领地。
- 单个领地 X/Z 最大 500，最小 1x1。
- Y 轴按玩家选区计算，圈多少就是多少。
- 快速选区改为 48x16x48。

建议停服后覆盖，再启动服务器。

如果你不想覆盖 `plugins/LuckPerms/yaml-storage/groups/default.yml`，可以不复制这个文件，改为按 `LUCKPERMS-COMMANDS.txt` 在控制台执行命令。

如果不停服，覆盖后至少执行：

```text
lp reloadconfig
/resreload
/dm reload
/cmi reload
```

但本次涉及 LuckPerms 和 Residence 组限制，重启最稳。
