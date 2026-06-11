# LeafGomoku GUI Platform 迁移包

把本目录里的内容复制到服务器根目录，保持目录结构覆盖。

推荐服务器目录：

```powershell
D:\YuHua服务器\current\leafmc-server
```

## 包含内容

- `plugins/LeafGomoku-0.1.0.jar`
- `plugins/LeafGomoku/config.yml`
- `plugins/LeafGomoku/rooms.yml`
- `plugins/DeluxeMenus-1.14.1-Release.jar`
- `plugins/PlaceholderAPI-2.12.2.jar`
- `plugins/DeluxeMenus/config.yml`
- `plugins/DeluxeMenus/gui_menus/*.yml`
- `CHECKSUMS.txt`
- `docs/operations/gomoku/2026-06-10-runtime-smoke-test.md`

没有包含 `plugins/LeafGomoku/stats.yml`，避免覆盖线上玩家积分和排行榜。

## 迁移步骤

1. 停服。
2. 备份服务器上的 `plugins/LeafGomoku/`、`plugins/DeluxeMenus/`、`plugins/CommandGUI/`、`plugins/PlaceholderAPI/` 和 `CHECKSUMS.txt`。
3. 把本目录里的文件复制到服务器根目录，覆盖同路径文件。
4. 如果服务器还有旧菜单栈，停用或删除：
   - `plugins/CommandGUI-3.3.0.jar`
   - `plugins/CommandGUI/`
   - `plugins/PlaceholderAPI-2.11.6.jar`
   - `plugins/.paper-remapped/CommandGUI-3.3.0.jar`
   - `plugins/.paper-remapped/PlaceholderAPI-2.11.6.jar`
5. 启动服务器。
6. 进服测试 `/menu` 和 `/menugomoku`。

## 房间坐标注意

`plugins/LeafGomoku/rooms.yml` 里的 `main` 和 `test1` 使用当前本地测试世界坐标。只有服务器世界和本地测试世界一致时才直接覆盖。

如果服务器世界坐标不同：

1. 先不要覆盖线上 `plugins/LeafGomoku/rooms.yml`。
2. 启动后管理站在目标位置执行：

```text
/gomoku admin create 房间名
/gomoku admin init 房间名
/gomoku admin inspect 房间名
```

## 上线检查

控制台应看到：

- `LeafGomoku v0.1.0` 启用。
- PlaceholderAPI 注册 `leafgomoku` expansion。
- DeluxeMenus 加载 8 个 GUI menus。

管理在控制台或游戏里执行：

```text
gomoku admin refresh main
gomoku admin inspect main
```

房间刷新会生成安全地板和玻璃外圈。玩家打开 `/menugomoku` 后，应先看到分页房间大厅；点击房间进入详情页，再选择 `加入对局` 或 `只观战`。
