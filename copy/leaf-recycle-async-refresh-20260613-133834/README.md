# LeafRecycle Async Refresh 20260613-133834

用途：替换之前直接删除掉落物的 CMI 定时清理方案，改为 LeafRecycle 回收站。

## 复制方式

1. 停服。
2. 把本目录里的 `plugins/` 整个复制到服务器根目录，覆盖同名文件。
3. 启动服务器。
4. 如果 LuckPerms 没有这些权限，按 `LUCKPERMS-COMMANDS.txt` 执行一次。

## 包含内容

- `plugins/LeafRecycle-0.1.0.jar`
- `plugins/LeafRecycle/config.yml`
- `plugins/CMI/Settings/Alias.yml`
- `plugins/CMI/Settings/Modules.yml`
- `plugins/CMI/Settings/Schedules.yml`

## 不包含内容

- `plugins/LeafRecycle/recycle.yml`

`recycle.yml` 是线上回收池数据文件，不放进覆盖包，避免以后覆盖掉服务器已有回收物。

## 命令行为

- `/recycle` 打开公共回收站，点物品直接领取到背包。
- `/recycle collect` 管理员倒计时回收所有已加载世界的地面掉落物，进入公共回收站。
- `/recycle reload` 重载文案配置和回收池数据。

## 本次改动

- 回收池数据落盘改为异步队列，减少提交/领取/全图回收时主线程写文件压力。
- 玩家投放、管理员 collect、玩家领取、管理员删除后，会自动刷新所有已经打开的 `/recycle` 和 `/recycle admin` 界面。
- CMI 的 `/recycle` alias 保持关闭，避免抢 LeafRecycle 命令。
- CMI `drop_item_cleanup` 定时删除方案撤回，掉落物不再直接删除，改由 `/recycle collect` 回收到公共池。

## 启动后测试

1. 玩家 A 执行 `/recycle`，保持界面打开。
2. 玩家 B 执行 `/recycle submit`，放入圆石并关闭界面。
3. 玩家 A 的回收站界面应自动刷新并看到圆石，不需要退出重进。
4. 玩家 A 点击圆石，应直接领取到背包。
5. 管理员在地上丢几组物品后执行 `/recycle collect`。
6. 倒计时结束后，打开中的回收站界面应自动出现这批掉落物。
7. 管理员改 `plugins/LeafRecycle/config.yml` 文案后执行 `/recycle reload`，新文案应生效。
