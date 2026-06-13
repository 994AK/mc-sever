# 玩家菜单整理服务器复制包

复制方式：把本目录里的 `plugins/` 文件夹合并到服务器根目录。建议停服、备份后覆盖配置，再启动服务器。

包含文件：

```text
plugins/DeluxeMenus/config.yml
plugins/DeluxeMenus/gui_menus/main.yml
plugins/DeluxeMenus/gui_menus/profile.yml
plugins/DeluxeMenus/gui_menus/teleport.yml
plugins/DeluxeMenus/gui_menus/projects.yml
plugins/LeafMenuTool-0.1.0.jar
plugins/CMI/CustomAlias/CustomAlias.yml
plugins/CMI/CustomText/rules.txt
plugins/CMI/Settings/EventCommands.yml
plugins/CMI/Settings/RandomTeleportations.yml
LUCKPERMS-COMMANDS.txt
PACKAGE_CHECKSUMS.txt
```

## 本次更新

- `/menu` 收敛为三类：玩家便捷、传送点、游戏功能。
- 主菜单保留常用直达：出生点、随机传送、我的家、资源回收站、好友菜单、规则求助。
- 补上资源回收站、连锁采集、假人常用入口。
- 菜单 lore 使用 `&` 颜色规范：灰色说明、深灰插件来源、白色命令格式、红色风险提醒。
- 所有 active 菜单按钮都显式配置左键和右键命令，避免右键无动作。
- 好友菜单、回收站、五子棋、皮肤、家列表等外部 GUI 入口不再先执行 `[close]`，避免菜单被关闭后目标 GUI 没打开。
- 传送类按钮会先执行命令再关闭菜单。
- 新增 `LeafMenuTool-0.1.0.jar`：玩家右键名为 `菜单钟` 的时钟时执行 `/menu`。
- 首次进服和 `/menutool` 重新领取的菜单钟文案改为“右键打开 /menu”。
- `/rules` 规则页更新为当前三分类菜单入口，不再指向旧的 `/menunav`、`/menuland`。
- CMI 随机传送主世界范围改为环形 `1500-5000` 格。
- 新增 `/menugame` CMI alias，指向 `/menuprojects`。

## 安装

1. 停服。
2. 备份以下目录或文件：
   - `plugins/DeluxeMenus/config.yml`
   - `plugins/DeluxeMenus/gui_menus/`
   - `plugins/CMI/CustomAlias/CustomAlias.yml`
   - `plugins/CMI/CustomText/rules.txt`
   - `plugins/CMI/Settings/EventCommands.yml`
   - `plugins/CMI/Settings/RandomTeleportations.yml`
3. 把本包的 `plugins/` 合并覆盖到服务器根目录。
4. 启动服务器。
5. 在后台执行 `LUCKPERMS-COMMANDS.txt` 里的命令。

如果目标服的 `plugins/CMI/CustomAlias/CustomAlias.yml` 有线上临时改动，不要直接覆盖；手动合并 `menugame` alias 块即可。随机传送配置同理，如目标服已调整过其它世界，只合并 `world` 的 `Range.Max: 5000`、`Range.Min: 1500` 和 `Circle: true`。

## 最短测试

1. 普通玩家执行 `/menu`，应看到 `玩家便捷`、`传送点`、`游戏功能` 三类。
2. 普通玩家执行 `/menutool`，拿到名为 `菜单钟` 的时钟；右键菜单钟应打开 `/menu`。
3. 左键和右键点击 `规则和求助`，都应打开规则页。
4. 左键和右键点击 `好友菜单`，都应打开 LeafFriends 好友 GUI。
5. 点击 `传送点`，再点 `随机传送`，应执行 CMI `/rt`，主世界范围为 `1500-5000`。
6. 点击 `游戏功能`，确认能看到资源回收站、连锁采集、五子棋、领地、假人、项目和任务入口。
7. 点击 `连锁采集`、`设置家`、`请求传送`、`移除假人` 这类说明/风险按钮，应只发送命令提示，不应直接关闭菜单或执行危险操作。
8. 普通玩家执行 `/menugame`，应打开游戏功能页。
9. 普通玩家确认 `/recycle`、`/menufriends`、`/menugomoku`、`/fp list`、`/homes` 等入口权限正常。

## 不包含

- 不覆盖 LuckPerms `groups/default.yml`，权限请用 `LUCKPERMS-COMMANDS.txt` 补齐，避免覆盖线上已有权限调整。
