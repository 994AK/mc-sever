# LeafGomoku v2 内置菜单迁移包

把本目录里的内容复制到服务器根目录，保持目录结构覆盖。

## 包含内容

- `plugins/LeafGomoku-0.1.0.jar`
- `plugins/LeafGomoku/config.yml`
- `plugins/LeafGomoku/rooms.yml`
- `docs/operations/gomoku/2026-06-10-runtime-smoke-test.md`
- `CHECKSUMS.txt`

本包不包含 DeluxeMenus、PlaceholderAPI，也不要求更新 `plugins/DeluxeMenus/gui_menus/*.yml`。

## 内置入口

玩家直接使用插件内置菜单：

```text
/gomoku
/gomoku gui
/menugomoku
/gomokumenu
/wzq
```

控制台执行无参数 `gomoku` 仍然是状态查询，不会尝试打开 GUI。

## 迁移步骤

1. 停服。
2. 备份服务器上的 `plugins/LeafGomoku/config.yml`、`plugins/LeafGomoku/rooms.yml`、`plugins/LeafGomoku/data.db` 和旧 `plugins/LeafGomoku/stats.yml`。
3. 把本目录里的文件复制到服务器根目录，覆盖同路径文件。
4. 不要把任何测试服 `data.db` 覆盖到正式服。`data.db` 保存战绩、排行榜、历史对局、外观解锁、玩家偏好和积分流水。
5. 启动服务器。
6. 进服测试 `/gomoku` 或 `/menugomoku`。

## 外部主菜单

如果仍想让全服 `/menu` 出现一个五子棋入口，只需要外部菜单执行：

```text
player_command: 'gomoku'
```

这只是可选入口。以后 LeafGomoku 自身更新不需要同步 DeluxeMenus 配置。

## 房间坐标注意

`plugins/LeafGomoku/rooms.yml` 是本地测试坐标。只有服务器世界和坐标一致时才直接覆盖。

如果目标服务器坐标不同，先不要覆盖线上 `rooms.yml`，启动后用：

```text
/gomoku admin place 房间名
# 右键地面锚点
/gomoku admin init 房间名
/gomoku admin inspect 房间名
```
