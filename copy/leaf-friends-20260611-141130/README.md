# LeafFriends 好友系统复制包

这个目录按服务端原路径整理，复制到服务器根目录即可。复制前请先停服。

## 包含文件

- `plugins/LeafFriends-0.1.0.jar`：好友系统插件。
- `plugins/LeafFriends/config.yml`：好友申请、传送冷却、允许传送世界和 GUI 标题配置。
- `plugins/DeluxeMenus/gui_menus/social.yml`：在 `/menusocial` 里加入“好友系统”按钮。
- `plugins/LuckPerms/yaml-storage/groups/default.yml`：给默认玩家 `leaffriends.use`，并明确不给 `leaffriends.reload` / `leaffriends.admin`。

## 复制步骤

```text
stop
复制 plugins/LeafFriends-0.1.0.jar
复制 plugins/LeafFriends/config.yml
复制 plugins/DeluxeMenus/gui_menus/social.yml
复制 plugins/LuckPerms/yaml-storage/groups/default.yml
start
```

启动后先看控制台是否出现 `LeafFriends` 启用成功。

## 冒烟测试

用两个普通测试账号验证：

```text
/menufriends
/friend add PlayerB
/friend accept PlayerA
/friend list
/friend msg PlayerB hello
/friend tp PlayerB
/friend tpaccept PlayerA
/friend block PlayerB
/friend unblock PlayerB
/friend toggle requests off
/friend toggle requests on
```

确认普通玩家不能执行：

```text
/friend reload
```

## 注意边界

- 好友传送必须对方同意，不会直接拉人。
- 默认只允许 `world` 里发起和完成好友传送；要放开其他世界，改 `plugins/LeafFriends/config.yml` 的 `allowed-teleport-worlds`。
- 本包不会覆盖运行时生成的 `plugins/LeafFriends/friends.yml`。
- LeafFriends 不给战力、飞行、物品、经济、公会等级、领地自动授权、家传送共享、箱子权限或任何直接传送能力。
