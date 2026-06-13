# FakePlayer 使用文档

适用插件：`fakeplayer 0.3.19` 补丁版，服务器版本 Leaf `1.21.11`。  
主命令：`/fp`，完整命令名：`/fakeplayer`。

## 基本规则

- 普通玩家只能看到、选择、操作自己创建的假人。
- OP 可以看到和操作全服所有假人。
- 默认每个玩家最多创建 `1` 个假人。
- 默认不指定名字时，假人会按 `<玩家名>_bot_1`、`<玩家名>_bot_2` 这种格式生成，方便区分归属。
- 默认会从玩家 Tab 列表隐藏假人，并让服务器列表 ping 的在线人数扣除假人。
- 如果玩家有多个假人，建议先用 `/fp select <假人名>` 选中一个，后续命令就可以省略假人名。
- 假人名建议只用英文、数字、下划线，避免和原版命令或其他插件冲突。
- 不建议给普通玩家 `/fp cmd`，这个命令会让假人执行服务器命令，风险高。

## 玩家常用命令

| 用途 | 命令 |
| --- | --- |
| 创建假人 | `/fp spawn` |
| 创建指定名称假人 | `/fp spawn <名字>`，需要 `fakeplayer.command.spawn.name` |
| 查看自己的假人 | `/fp list` |
| 选择一个假人 | `/fp select <假人名>` |
| 查看当前选中的假人 | `/fp selection` |
| 移除当前或唯一假人 | `/fp kill` |
| 移除指定假人 | `/fp kill <假人名>` |
| 移除自己所有假人 | `/fp kill -a` |
| 查看与假人的距离 | `/fp distance [假人名]` |
| 查看假人状态 | `/fp status [假人名]` |
| 打开假人背包 | `/fp invsee [假人名]` |
| 设置假人手持栏位 | `/fp hold <1-9> [假人名]` |
| 给假人换皮肤 | `/fp skin <玩家名> [假人名]` |

`[假人名]` 表示可选。如果玩家只有一个假人，或者已经 `/fp select` 选中了假人，可以不写。

## 移动和传送

| 用途 | 命令 |
| --- | --- |
| 传送到假人 | `/fp tp [假人名]` |
| 把假人传送到自己身边 | `/fp tphere [假人名]` |
| 和假人交换位置 | `/fp tps [假人名]` |
| 向前走一步 | `/fp move forward [假人名]` |
| 向后走一步 | `/fp move backward [假人名]` |
| 向左走一步 | `/fp move left [假人名]` |
| 向右走一步 | `/fp move right [假人名]` |
| 开关潜行 | `/fp sneak [假人名] true` / `/fp sneak [假人名] false` |
| 开关疾跑 | `/fp sprint [假人名] true` / `/fp sprint [假人名] false` |

## 朝向和视角

| 用途 | 命令 |
| --- | --- |
| 面向东南西北 | `/fp look east [假人名]`、`/fp look west [假人名]`、`/fp look south [假人名]`、`/fp look north [假人名]` |
| 面向上/下 | `/fp look up [假人名]`、`/fp look down [假人名]` |
| 看向自己 | `/fp look me [假人名]` |
| 看向坐标 | `/fp look at <x> <y> <z> [假人名]` |
| 左转/右转/后转 | `/fp turn left [假人名]`、`/fp turn right [假人名]`、`/fp turn back [假人名]` |

## 动作命令

以下动作都支持 `once`、`continuous`、`interval <ticks>`、`stop`：

```text
/fp attack once [假人名]
/fp mine continuous [假人名]
/fp use interval 20 [假人名]
/fp jump once [假人名]
/fp drop once [假人名]
/fp dropstack once [假人名]
/fp dropinv once [假人名]
/fp attack stop [假人名]
```

常用解释：

- `once`：执行一次。
- `continuous`：持续执行。
- `interval <ticks>`：每隔多少 tick 执行一次，`20 tick = 1 秒`。
- `stop`：停止这个动作。
- `/fp stop [假人名]`：停止假人当前动作。

## 假人配置

查看自己可配置的功能：

```text
/fp config
```

设置以后新假人的默认功能：

```text
/fp config set autofish true
/fp config set replenish true
```

设置当前假人的功能：

```text
/fp set collidable false [假人名]
/fp set invulnerable true [假人名]
/fp set pickup_items true [假人名]
/fp set autofish true [假人名]
```

可用功能包括：

- `collidable`：是否有碰撞箱。
- `invulnerable`：是否无敌。
- `wolverine`：高再生效果。
- `look_at_entity`：自动看向附近实体。
- `pickup_items`：是否拾取物品。
- `skin`：是否使用皮肤。
- `replenish`：自动补货。
- `autofish`：自动钓鱼。

## OP 命令

OP 除了能操作自己的假人，还能查看和操作全服所有假人。

| 用途 | 命令 |
| --- | --- |
| 查看全服假人 | `/fp list` |
| 移除指定假人 | `/fp kill <假人名>` |
| 移除全服所有假人 | `/fp killall` |
| 重载 FakePlayer 配置 | `/fp reload` |
| 重载语言文件 | `/fp reload-translation` |
| 指定位置生成假人 | `/fp spawn <名字> <world> <x> <y> <z>` |

注意：`/fp killall`、`/fp reload`、`/fp reload-translation` 是 OP 级命令，`fakeplayer.*` 权限本身不等于 OP。

## LuckPerms 权限配置

### 普通玩家

推荐先给普通玩家最小可用权限：

```bash
lp group default permission set fakeplayer.spawn true
lp group default permission set fakeplayer.action true
lp group default permission set fakeplayer.limit.1 true
```

这会允许：

- 创建、查看、选择、移除自己的假人。
- 查看状态、背包、距离。
- 控制攻击、挖掘、使用、移动、潜行、疾跑、睡觉等基础动作。
- 每人最多 `1` 个假人。

如果不想让普通玩家用假人传送，不要给 `fakeplayer.tp`。

如果要允许普通玩家自定义假人名，再额外给：

```bash
lp group default permission set fakeplayer.command.spawn.name true
```

### VIP 或可信玩家

示例：允许更多假人，并允许传送：

```bash
lp group vip permission set fakeplayer.spawn true
lp group vip permission set fakeplayer.action true
lp group vip permission set fakeplayer.tp true
lp group vip permission set fakeplayer.limit.3 true
```

`fakeplayer.limit.3` 表示最多 `3` 个。  
`fakeplayer.limit.0` 表示不限制数量，只建议给管理员或非常可信的玩家。

### 管理员权限

给非 OP 管理员全部 FakePlayer 插件权限：

```bash
lp group admin permission set fakeplayer.* true
lp group admin permission set fakeplayer.limit.0 true
```

如果这个管理员还需要 `/fp killall`、`/fp reload`、`/fp reload-translation`，需要设为 OP，或由控制台执行：

```bash
op <玩家名>
```

### 高风险权限

这些权限不要默认给普通玩家：

```text
fakeplayer.command.spawn.name
fakeplayer.command.spawn.location
fakeplayer.command.cmd
fakeplayer.bypass.limit
fakeplayer.bypass.player-policy
fakeplayer.*
```

说明：

- `fakeplayer.command.spawn.name`：允许自定义假人名。
- `fakeplayer.command.spawn.location`：允许指定世界和坐标生成。
- `fakeplayer.command.cmd`：允许假人执行命令，风险高。
- `fakeplayer.bypass.limit`：绕过服务器、玩家、IP 数量限制。
- `fakeplayer.bypass.player-policy`：绕过玩家级禁用策略。
- `fakeplayer.*`：包含全部 FakePlayer 插件权限，适合管理员，不适合普通玩家。

## 权限节点速查

| 权限 | 作用 |
| --- | --- |
| `fakeplayer.spawn` | 基础生成和管理权限集合 |
| `fakeplayer.action` | 假人动作控制权限集合 |
| `fakeplayer.tp` | 传送相关权限集合 |
| `fakeplayer.exp` | 经验相关权限集合 |
| `fakeplayer.command.spawn` | `/fp spawn` |
| `fakeplayer.command.kill` | `/fp kill` |
| `fakeplayer.command.list` | `/fp list` |
| `fakeplayer.command.select` | `/fp select` |
| `fakeplayer.command.selection` | `/fp selection` |
| `fakeplayer.command.distance` | `/fp distance` |
| `fakeplayer.command.status` | `/fp status` |
| `fakeplayer.command.invsee` | `/fp invsee` |
| `fakeplayer.command.skin` | `/fp skin` |
| `fakeplayer.command.tp` | `/fp tp` |
| `fakeplayer.command.tphere` | `/fp tphere` |
| `fakeplayer.command.tps` | `/fp tps` |
| `fakeplayer.command.attack` | `/fp attack` |
| `fakeplayer.command.mine` | `/fp mine` |
| `fakeplayer.command.use` | `/fp use` |
| `fakeplayer.command.jump` | `/fp jump` |
| `fakeplayer.command.move` | `/fp move` |
| `fakeplayer.command.sneak` | `/fp sneak` |
| `fakeplayer.command.sprint` | `/fp sprint` |
| `fakeplayer.command.look` | `/fp look` |
| `fakeplayer.command.turn` | `/fp turn` |
| `fakeplayer.command.stop` | `/fp stop` |
| `fakeplayer.command.cmd` | `/fp cmd` |
| `fakeplayer.limit.<数字>` | 玩家额度，`0` 为无限 |

## 单独限制或禁用玩家

如果某个玩家违规，不建议只改组权限，因为他可能还会继承其他组权限。推荐直接在 `plugins/fakeplayer/config.yml` 里加 `player-policies`。

禁用某个玩家生成假人：

```yaml
player-policies:
  Steve:
    spawn-enabled: false
    reason: '假人使用违规，已临时禁用'
```

按 UUID 禁用更稳定：

```yaml
player-policies:
  00000000-0000-0000-0000-000000000000:
    spawn-enabled: false
    reason: '假人使用违规，已临时禁用'
```

单独调整某个玩家额度：

```yaml
player-policies:
  Alex:
    spawn-enabled: true
    player-limit: 2
```

配置生效：

```text
/fp reload
```

优先级：

1. `fakeplayer.bypass.player-policy` 可以绕过 `player-policies`。
2. `player-policies` 优先于 `fakeplayer.limit.<数字>`。
3. `fakeplayer.limit.<数字>` 优先于全局 `player-limit`。
4. 多个 `fakeplayer.limit.<数字>` 同时存在时，取最大值。

## 有登录插件时

这套本地服有 `AuthMe`。本复制包已经把 `plugins/fakeplayer/config.yml` 配成 AuthMe 兼容模式：

```yaml
prevent-kicking: ALWAYS

pre-spawn-commands:
  - 'authme register %p fp_%u'

after-spawn-commands:
  - 'authme forcelogin %p'
```

说明：

- `%p` 是假人名称。
- `%u` 是假人 UUID。
- `authme register %p fp_%u` 会给假人注册一个固定密码。
- `authme forcelogin %p` 会让 AuthMe 把假人视为已登录。
- 如果日志里提示假人账号已注册，可以保留这行；它只会产生提示，不影响已注册假人后续 `forcelogin`。
- 如果目标服不是 AuthMe，把这两条 `authme ...` 命令替换成你的登录插件提供的控制台注册/强制登录命令。
- 如果目标服没有登录插件，删除这两条 `authme ...` 命令，或把对应命令列表改为空。

改完执行：

```text
/fp reload
```

如果服务器还启用了白名单插件，优先确认白名单是否真的开启。当前本地 `NobleWhitelist` 配置里 `whitelist.enabled: false`，不拦截假人。若目标服务器开启了白名单，需要把假人名加入白名单，或用对应白名单插件的命令在 `pre-spawn-commands` 里自动放行。

## 推荐服务器默认策略

```bash
lp group default permission set fakeplayer.spawn true
lp group default permission set fakeplayer.action true
lp group default permission set fakeplayer.limit.1 true

lp group vip permission set fakeplayer.spawn true
lp group vip permission set fakeplayer.action true
lp group vip permission set fakeplayer.tp true
lp group vip permission set fakeplayer.limit.3 true

lp group admin permission set fakeplayer.* true
lp group admin permission set fakeplayer.limit.0 true
```

`config.yml` 建议保持：

```yaml
server-limit: 1000
player-limit: 1
detect-ip: true
kale-tps: 0
hide-from-player-list: true
hide-from-server-list-ping-count: true
player-policies: {}
```

如果假人明显影响服务器性能，先不要立刻开自动清理。建议先降低玩家额度，处理违规玩家，再考虑设置 `kale-tps`。

说明：这两个隐藏配置不会把假人从 Bukkit 在线玩家集合里移除。少数依赖 `Bukkit.getOnlinePlayers()` 的插件仍可能把假人算作在线玩家；硬移除会破坏假人动作和事件链，不建议这样做。
