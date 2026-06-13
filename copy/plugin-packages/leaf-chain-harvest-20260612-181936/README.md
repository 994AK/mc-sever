# LeafChainHarvest 0.1.0 部署包

把本目录里的 `plugins/` 合并到服务器根目录。建议停服后覆盖 jar，再启动服务器。

包含文件：

```text
plugins/LeafChainHarvest-0.1.0.jar
plugins/LeafChainHarvest/config.yml
plugins/LeafChainHarvest/settings.yml
LUCKPERMS-COMMANDS.txt
PACKAGE_CHECKSUMS.txt
```

## 功能

- 右键成熟作物收割并自动补种。
- 范围播种、范围施肥，生存模式按成功数量消耗种子或骨粉。
- 斧头连锁砍原木、木头、菌柄、菌核及去皮变种。
- 镐子连锁挖矿石和远古残骸。
- 内置 `/leafchain menu` 管理菜单；绿色表示允许，红色表示关闭。
- 默认所有动作和材料都是关闭状态，需要管理员在菜单里点亮后才生效。
- 默认不包含石头、深板岩、泥土、沙子、砂砾、树叶、木板等基础或建筑方块。

## 安装

1. 停服。
2. 把 `plugins/LeafChainHarvest-0.1.0.jar` 放到服务器 `plugins/`。
3. 把 `plugins/LeafChainHarvest/config.yml` 放到服务器 `plugins/LeafChainHarvest/config.yml`。
4. 如果要把线上旧菜单状态重置为全关，也覆盖 `plugins/LeafChainHarvest/settings.yml`。
5. 启动服务器。
6. 在后台执行 `LUCKPERMS-COMMANDS.txt` 里的命令。

## 管理命令

```text
/leafchain menu
/leafchain reload
/lch menu
```

菜单点击状态会保存到 `plugins/LeafChainHarvest/settings.yml`。`config.yml` 负责默认目录、限制、提示和 Residence 降级策略。自动补种、播种和施肥都需要通过保护检查。

## 最短测试

1. 管理员执行 `/leafchain menu`，确认收集、播种、施肥和材料默认都是关闭状态。
2. 点亮 `收集`，进入农作物页点亮小麦，再让普通玩家右键成熟小麦，应掉落并自动补种。
3. 普通玩家右键未成熟小麦，应无动作。
4. 点亮 `播种` 和小麦，普通玩家手持小麦种子在空农田旁右键，应范围播种并消耗种子。
5. 点亮 `施肥` 和小麦，普通玩家手持骨粉右键作物附近，应范围施肥并消耗骨粉。
6. 点亮对应木头材料，普通玩家手持斧头破坏相连原木，应连锁木头但不破坏树叶/木板。
7. 点亮对应矿物材料，普通玩家手持镐子破坏相连钻石矿，应连锁矿物但不破坏石头/深板岩。
8. 在无 build 权限的 Residence 领地里测试播种/施肥，应被跳过或提示受保护。

完整步骤见 `docs/operations/leaf-chain-harvest-smoke-test.md`。
