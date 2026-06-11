# Drop Item Cleanup 20260610-1508

用途：通过 CMI 定时清理地面掉落物，缓解玩家机器不回收产物导致的世界 TPS 压力。

## 复制方式

1. 停服。
2. 把本目录里的 `plugins/` 整个复制到服务器根目录，覆盖同名文件。
3. 启动服务器。

## 包含内容

- `plugins/CMI/Settings/Modules.yml`
- `plugins/CMI/Settings/Schedules.yml`

## 生效内容

- 打开 CMI `schedule` 模块。
- 新增 `drop_item_cleanup` 定时任务。
- 每 10 分钟触发一次。
- 在线人数至少 1 人才执行。
- 清理前 60 秒、30 秒、10 秒广播提醒。
- 清理前 5/3/2/1 秒 actionbar 倒计时。
- 实际清理命令为 `cmi groundclean -w:<world> -s`，覆盖 `world`、`world_nether`、`world_the_end`。

## 说明

当前 CMI 9.8.7.7 的本地帮助里，`killall` 是清怪命令，掉落物清理对应 `groundclean`。因此这里用 `groundclean -w:<world> -s` 执行实际掉落物清理，避免上线后 `killall item` 参数不匹配。

## 启动后检查

- 控制台不应出现 CMI schedule 配置解析错误。
- 游戏内可用管理员执行 `/cmi schedule drop_item_cleanup` 手动触发一次倒计时测试。
- 触发后应看到清理提醒，并在倒计时结束后清理地面掉落物。
