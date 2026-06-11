# Menu DeluxeMenus Update 20260610-1042

用途：把玩家菜单从 CommandGUI 迁移到 DeluxeMenus，并应用当前简洁版主菜单。

## 复制方式

1. 停服。
2. 把本目录里的 `plugins/` 整个复制到服务器根目录，覆盖同名文件。
3. 按 `DELETE_FILES.txt` 删除目标服旧文件。
4. 启动服务器。

## 包含内容

- `plugins/DeluxeMenus-1.14.1-Release.jar`
- `plugins/PlaceholderAPI-2.12.2.jar`
- `plugins/DeluxeMenus/config.yml`
- `plugins/DeluxeMenus/gui_menus/*.yml`
- `plugins/PlaceholderAPI/config.yml`
- `plugins/CMI/CustomAlias/CustomAlias.yml`
- `plugins/CMI/Settings/EventCommands.yml`
- `plugins/CMI/CustomText/rules.txt`
- `plugins/LuckPerms/yaml-storage/groups/default.yml`

## 菜单状态

- `/menu` 第一屏只保留 6 个模块：生存传送、家园地标、领地保护、个人功能、玩家互动、规则帮助。
- 项目任务模块已从 DeluxeMenus 加载列表下架，`/menuprojects` 不再注册。
- CommandGUI 不再作为活跃插件使用。

## 启动后检查

- 控制台应出现 `DeluxeMenus successfully hooked into PlaceholderAPI`。
- 控制台应出现 `8 GUI menus loaded!`。
- 控制台不应再加载 `CommandGUI`。
- 游戏内执行 `/menu`，确认主菜单只有简洁模块入口。
