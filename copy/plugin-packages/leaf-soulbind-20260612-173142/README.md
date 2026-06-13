# LeafSoulbind 0.1.0 部署包

把本目录里的 `plugins/` 覆盖到服务器根目录。

包含文件：

```text
plugins/LeafSoulbind-0.1.0.jar
plugins/LeafSoulbind/config.yml
```

建议停服后覆盖 jar，再启动服务器。首次启动也会自动生成配置；这里附带 `config.yml` 是为了直接使用当前规则和中文提示。

## 玩家命令

```text
/soul lock
/soul bind
/soul unlock
/soul info
/soul recover
```

- `/soul lock`：锁定手持物品，防止误丢到地上。
- `/soul bind`：灵魂绑定给自己；只有本人或管理员可以解除。
- `/soul unlock`：解除锁定/绑定。
- `/soul recover`：死亡返还时背包满了，用这个再次取回。

## LuckPerms 控制台命令

```text
lp group default permission set leafsoulbind.use true
lp group default permission set leafsoulbind.lock true
lp group default permission set leafsoulbind.unlock true
lp group default permission set leafsoulbind.bind true
lp group default permission set leafsoulbind.recover true
lp group admin permission set leafsoulbind.admin true
lp group admin permission set leafsoulbind.reload true
lp group admin permission set leafsoulbind.bypass true
```

## 最短测试

```text
1. 手持钻石剑，输入 /soul lock。
2. 按 Q 或 Ctrl+Q，应该提示不能丢弃，物品仍在背包。
3. 把物品放进箱子，应该允许。
4. 箱子里有锁定物品时打掉箱子，应该被阻止。
5. 输入 /soul unlock，再按 Q，应该可以正常丢弃。
6. 手持重要物品输入 /soul bind，然后死亡，物品应该不掉地上，复活后返还。
```

## 当前规则

- 玩家主动丢弃锁定/绑定物品：阻止。
- 背包界面把锁定/绑定物品丢到窗口外：阻止。
- 死亡掉落：阻止，并暂存后返还。
- 放进箱子/木桶/潜影盒：允许。
- 打掉含锁定/绑定物品的容器：阻止。
- 发射器/投掷器吐出锁定/绑定物品：阻止。
- 非绑定者拾取灵魂绑定物品：阻止。
- 默认不允许 `leafsoulbind.bypass` 绕过这些保护，避免管理员 `*` 权限测试时误以为保护失效；需要临时打开时改 `rules.allow-permission-bypass: true`。
