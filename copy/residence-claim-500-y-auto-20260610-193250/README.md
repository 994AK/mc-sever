# Residence 圈地修正复制包

把本目录里的 `plugins/` 文件夹复制到服务器根目录，覆盖同路径文件。

包含文件：

- `plugins/Residence/config.yml`
- `plugins/Residence/groups.yml`
- `plugins/DeluxeMenus/gui_menus/land.yml`
- `plugins/CMI/CustomText/menuland.txt`

生效内容：

- 普通玩家可圈地。
- 单个领地 X/Z 最大 500。
- 最小圈地 1x1。
- Y 轴按玩家选区计算，圈多少就是多少。
- 菜单和 `/menuland` 文案同步去掉手动高度扩展步骤。

复制后重启服务端；如果只想热重载，至少执行 `/resreload`，并重载 DeluxeMenus/CMI 文案。
