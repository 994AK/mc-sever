# Copy 交付包索引

`copy/` 只放给运维复制、上线、公告使用的交付物，不放源码、不放运行日志。

## 分类

| 目录 | 用途 |
| --- | --- |
| `plugin-packages/` | 单插件上线包，通常包含 `plugins/<Plugin>.jar`、插件配置、README、校验文件和 LuckPerms 命令 |
| `server-packages/` | 整服级复制包或第三方插件补丁包，可能包含多个插件、配置和操作文档 |
| `announcements/` | 上线公告、CMI 广播命令、运营发布素材 |

## 当前包

### plugin-packages

- `leaf-chain-harvest-20260612-181936/`：LeafChainHarvest 连锁采集 / 农作物工具上线包。
- `leaf-recycle-20260612-162142/`：LeafRecycle 回收系统上线包。
- `leaf-soulbind-20260612-173142/`：LeafSoulbind 物品锁 / 灵魂绑定初版上线包。
- `leaf-soulbind-20260613-113045/`：LeafSoulbind 后续上线包。

### server-packages

- `fakeplayer-leaf-1.21.11-server-copy-20260612/`：FakePlayer Leaf 1.21.11 补丁复制包。
- `fakeplayer-leaf-1.21.11-server-copy-20260612.zip`：同上压缩包。
- `leaf-recycle-server-copy-20260613-113058/`：LeafRecycle 面向服务器覆盖的复制包。

### announcements

- `leaf-chain-harvest-announcement-20260613/`：LeafChainHarvest 公告 / CMI 广播命令。

## 命名约定

- 单插件包：`<plugin-or-feature>-YYYYMMDD-HHMMSS/`。
- 只含公告：`<feature>-announcement-YYYYMMDD/`。
- 整服复制包：`<feature>-server-copy-YYYYMMDD-HHMMSS/`。
