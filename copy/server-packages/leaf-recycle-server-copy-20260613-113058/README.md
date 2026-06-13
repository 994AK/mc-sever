# LeafRecycle 回收站插件复制包

复制方式：把本目录里的 `plugins/` 文件夹合并到服务器根目录。

包含文件：

- `plugins/LeafRecycle-0.1.0.jar`
- `plugins/LeafRecycle/config.yml`
- `LUCKPERMS-COMMANDS.txt`
- `PACKAGE_CHECKSUMS.txt`

## 功能

- 玩家输入 `/recycle` 打开公共资源回收站 GUI，点击物品直接领取，先到先得。
- 玩家输入 `/recycle submit` 打开投放 GUI，把不需要的资源放进去，关闭界面后自动进入公共池。
- 管理员输入 `/recycle collect`，倒计时后把主世界、地狱、末地等所有已加载世界里的地上掉落物回收到公共 GUI。
- 管理员输入 `/recycle admin` 打开管理 GUI，左键取出，右键删除。
- 清理倒计时和提示文案在 `plugins/LeafRecycle/config.yml` 里改，执行 `/recycle reload` 后生效。

## 安装

1. 停服。
2. 把 `plugins/LeafRecycle-0.1.0.jar` 放到服务器 `plugins/` 目录。
3. 把 `plugins/LeafRecycle/config.yml` 放到服务器 `plugins/LeafRecycle/config.yml`。
4. 启动服务器。
5. 在后台执行 `LUCKPERMS-COMMANDS.txt` 里的命令。

## 最短测试

1. 普通玩家执行 `/recycle`，应打开“资源回收站”GUI。
2. 普通玩家执行 `/recycle submit`，放入一些圆石，关闭 GUI。
3. 另一个玩家执行 `/recycle`，点击刚才的圆石，应直接领取；如果两人抢同一组，谁先点到就是谁的。
4. 管理员在主世界、地狱或末地丢几组物品，执行 `/recycle collect`。
5. 所有在线玩家应看到倒计时提示；倒计时结束后地上掉落物进入回收站。
6. 普通玩家再次执行 `/recycle`，应看到管理员回收进去的物品。
7. 管理员执行 `/recycle admin`，左键可取出，右键可删除。

## 数据文件

运行后动态数据会保存在：

- `plugins/LeafRecycle/recycle.yml`

这个文件记录当前公共回收池里的物品，备份服务器时一起备份即可。
