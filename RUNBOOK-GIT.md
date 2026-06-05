# Git Runbook

## 结论

服务器上可以用 `git pull`，但建议不要裸跑。推荐用：

```powershell
cd D:\LeafMC\current\leafmc-server
.\scripts\git-pull-update.ps1
```

这个脚本会先检查服务端是否还在运行，做一次备份，然后执行 `git pull --ff-only`。如果你已经手动停服并备份，裸命令也可以：

```powershell
cd D:\LeafMC\current\leafmc-server
git pull --ff-only
```

不要在服务端运行时 `git pull`，尤其是这次提交里包含 `*.jar`、`server.properties`、`plugins/CMI/`、`plugins/AuthMe/`、`plugins/LuckPerms/` 或启动脚本变更时。

## Git 管什么

Git 管这些：

- `README.md`
- `RUNBOOK-GIT.md`
- `VERSION_MANIFEST.md`
- `CHANGELOG.md`
- `CHECKSUMS.txt`
- `start-windows.bat`
- `start.sh`
- `server.properties`
- `bukkit.yml`
- `spigot.yml`
- `paper` / `leaf` / `gale` / `purpur` 配置
- `plugins/*.jar`
- 主要插件配置
- LuckPerms 的 `groups/*.yml`
- MiniMOTD 图标和配置

Git 不管这些：

- `world/`
- `world_nether/`
- `world_the_end/`
- `logs/`
- `cache/`
- `libraries/`
- `versions/`
- `plugins/CoreProtect/database.db`
- `plugins/AuthMe/authme.db`
- `plugins/CMI/cmi.sqlite.db`
- `plugins/NobleWhitelist/data.db`
- 备份 ZIP

世界、数据库、玩家登录信息、CoreProtect 记录只能走备份，不能靠 Git。

## 第一次建仓库

在服务端目录里执行：

```powershell
cd D:\LeafMC\current\leafmc-server
git init
git branch -M main
git add .
git status --short
```

确认 `git status --short` 里面没有这些路径：

```text
world/
world_nether/
world_the_end/
logs/
cache/
libraries/
versions/
*.db
*.sqlite
```

确认没问题后提交：

```powershell
git commit -m "chore: initial leafmc server package"
git remote add origin <你的Git仓库地址>
git push -u origin main
```

当前核心 jar 是 `80M`，低于 GitHub 单文件 `100M` 限制。先不用 Git LFS 也可以。如果后续某个 jar 超过远端限制，再改用 release artifact 或 Git LFS。

## 常规更新流程

1. 在本地修改服务端配置或替换插件 jar。
2. 更新 `VERSION_MANIFEST.md`、`CHANGELOG.md`、`CHECKSUMS.txt`。
3. 提交并推送。
4. 服务器停服。
5. 在服务器执行：

```powershell
cd D:\LeafMC\current\leafmc-server
.\scripts\git-pull-update.ps1
```

6. 启动：

```powershell
.\start-windows.bat
```

7. 看控制台是否出现 `Done`。

## 如果 pull 后出问题

如果是玩家破坏建筑，用 CoreProtect：

```text
co inspect
co lookup u:玩家名 t:1h r:20
co rollback u:玩家名 t:1h r:20
```

如果是更新导致服务端启动失败，用备份回滚：

```powershell
cd D:\LeafMC\current\leafmc-server
.\scripts\backup-before-pull.ps1 -ServerDir . -BackupRoot D:\LeafMC\backups\manual
```

真正回滚时，把 `backups/pre-pull/` 里对应时间的 ZIP 解压回 `current\leafmc-server`。不要用 `git checkout` 回滚世界和数据库，因为 Git 没有管理这些运行期数据。
