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

## 本次口径

- 默认全部关闭。
- 开关分两层：
  - 管理员设置“全服允许哪些功能/材料”。
  - 玩家设置“自己是否启用”。
- A 玩家自己开启，不会让 B 玩家跟着开启。
- 如果管理员全服关闭，玩家个人开启也不会生效。

## 玩家命令

```text
/leafchain self farm
/leafchain self chain
/leafchain self all
/leafchain self off
```

- `self farm`：自己开启农作物收集、批量播种、批量耕地、批量施肥。
- `self chain`：自己开启木头和矿物连锁。
- `self all`：自己全部开启。
- `self off`：自己全部关闭。

## 管理员命令

```text
/leafchain preset farm
/leafchain preset chain
/leafchain preset all
/leafchain preset off
/leafchain menu
/leafchain reload
```

- `preset farm`：全服允许农作物收集、播种、耕地、施肥和农作物材料。
- `preset chain`：全服允许木头和矿物材料。
- `preset all`：全服全部允许。
- `preset off`：全服全部关闭。

## 安装

1. 停服。
2. 把 `plugins/LeafChainHarvest-0.1.0.jar` 放到服务器 `plugins/`。
3. 把 `plugins/LeafChainHarvest/config.yml` 放到服务器 `plugins/LeafChainHarvest/config.yml`。
4. 如果要把线上旧状态重置为全关，也覆盖 `plugins/LeafChainHarvest/settings.yml`；如果要保留线上已设置的全服/玩家状态，就不要覆盖它。
5. 启动服务器。
6. 在后台执行 `LUCKPERMS-COMMANDS.txt` 里的命令。

## 最短测试

1. 管理员执行 `/leafchain preset farm`。
2. 玩家 A 执行 `/leafchain self farm`。
3. 玩家 A 手持种子右键空农田旁，应批量播种。
4. 玩家 A 手持锄头右键草方块/泥土，应批量变成耕地。
5. 玩家 B 不执行 `self farm` 时，不应触发这些功能。
6. 管理员执行 `/leafchain preset chain`，玩家 A 执行 `/leafchain self chain` 后，A 才能触发木头/矿物连锁。
7. 管理员执行 `/leafchain preset off` 后，所有玩家都不应触发功能。
