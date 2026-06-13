# FakePlayer Leaf 1.21.11 服务器复制包

适用服务器：Leaf `1.21.11`，Java `21`。

## 内容

- `plugins/fakeplayer.jar`
  - 已补 Leaf `1.21.11` 兼容。
  - 已加入玩家级额度和禁用策略。
  - 已加入隐藏 Tab 列表和服务器列表 ping 在线人数扣除策略。
- `plugins/CommandAPI-11.1.0-Paper.jar`
  - FakePlayer 依赖。
- `plugins/fakeplayer/config.yml`
  - 已内置登录插件兼容配置：`prevent-kicking: ALWAYS`。
  - 已按 AuthMe 配置自动注册和强制登录 hook。
  - 已启用 `hide-from-player-list` 和 `hide-from-server-list-ping-count`。
  - 复制到服务器时会覆盖目标服务器当前 FakePlayer 配置。
- `docs/operations/fakeplayer-usage.md`
  - 给玩家、OP、管理员看的使用和权限文档。
- `docs/operations/config.yml`
  - 你提供的 FakePlayer 配置文件副本，已同步登录插件兼容配置，方便留档或对照。

## 部署

在服务器根目录执行：

```bash
backup_dir="plugins-disabled/fakeplayer-backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$backup_dir"
find plugins -maxdepth 1 -type f \( -iname 'fakeplayer*.jar' -o -iname 'CommandAPI*.jar' \) -exec mv {} "$backup_dir"/ \;
[ -d plugins/fakeplayer ] && cp -R plugins/fakeplayer "$backup_dir/fakeplayer-config"
cp -R copy/fakeplayer-leaf-1.21.11-server-copy-20260612/* .
```

然后重启服务器。

注意：这个包包含 `plugins/fakeplayer/config.yml`。如果你的服务器已经有自定义 FakePlayer 配置，先对照备份目录里的 `fakeplayer-config` 合并；如果目标服不是 AuthMe，请把配置里的 `authme register` / `authme forcelogin` 两条命令替换成对应登录插件的命令。

## 验证

重启后检查日志：

```bash
grep -Ei "fakeplayer|CommandAPI|Unsupported Minecraft version|UnsupportedClassVersionError|NoClassDefFoundError|ClassNotFoundException|Exception" logs/latest.log
```

游戏内或控制台测试：

```text
fp list
fp spawn TestBot
fp list
fp kill TestBot
fp list
```

期望结果：

- `CommandAPI v11.1.0` 正常启用。
- `fakeplayer v0.3.19` 正常启用。
- `fp spawn TestBot` 可以生成假人。
- `fp kill TestBot` 可以移除假人。
- 不出现 `Unsupported Minecraft version: 1.21.11`、`UnsupportedClassVersionError`、`NoClassDefFoundError`。

## 使用文档

玩家和权限说明在：

```text
docs/operations/fakeplayer-usage.md
```

如果目标服务器有 AuthMe 这类登录插件，先看使用文档里的“有登录插件时”章节。本复制包已经默认启用 AuthMe 兼容：`prevent-kicking: ALWAYS`、`authme register %p fp_%u`、`authme forcelogin %p`。
