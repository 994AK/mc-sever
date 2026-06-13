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

## 本次更新

- 新增手持锄头右键批量耕地：草方块、泥土、砂土、土径会在范围内变成耕地。
- 保留原来的批量播种：手持种子右键空农田附近，范围播种。
- 新增快捷预设命令，减少 GUI 操作：
  - `/leafchain preset farm` 一键开启农作物收集、播种、耕地、施肥和全部农作物材料。
  - `/leafchain preset chain` 一键开启木头和矿物材料。
  - `/leafchain preset all` 全部开启。
  - `/leafchain preset off` 全部关闭。

## 安装

1. 停服。
2. 把 `plugins/LeafChainHarvest-0.1.0.jar` 放到服务器 `plugins/`。
3. 把 `plugins/LeafChainHarvest/config.yml` 放到服务器 `plugins/LeafChainHarvest/config.yml`。
4. 如果要把线上旧菜单状态重置为全关，也覆盖 `plugins/LeafChainHarvest/settings.yml`；如果要保留线上已经点亮的状态，就不要覆盖它。
5. 启动服务器。
6. 在后台执行 `LUCKPERMS-COMMANDS.txt` 里的命令。

## 推荐启用方式

玩家觉得 GUI 麻烦时，管理员直接执行：

```text
/leafchain preset farm
/leafchain preset chain
```

如果想全部开启：

```text
/leafchain preset all
```

如果要临时关掉全部：

```text
/leafchain preset off
```

## 最短测试

1. 后台执行 LuckPerms 命令后，管理员执行 `/leafchain preset farm`。
2. 普通玩家手持小麦种子右键空农田旁，应批量播种。
3. 普通玩家手持锄头右键草方块/泥土，应批量变成耕地。
4. 普通玩家手持骨粉右键作物附近，应批量施肥。
5. 管理员执行 `/leafchain preset chain` 后，普通玩家用斧头砍相连原木、用镐子挖相连矿物，应触发连锁。
6. 石头、深板岩、泥土、树叶、木板不在默认连锁目录中。
