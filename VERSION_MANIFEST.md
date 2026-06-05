# Version Manifest

## Release

- Server release: `leafmc-2026.06.05-r1`
- Date: `2026-06-05`
- Minecraft version: `1.21.11`
- Core: `Leaf 1.21.11 build 158`
- Java target: `Java 21`
- Host profile: `Windows Server 2022, 8 GB RAM`
- Startup script: `start-windows.bat`
- Startup memory: `-Xms4G -Xmx6G`
- Package mode: `Git-managed configs/scripts/jars, backup-managed worlds/databases`

## Runtime Policy

- `online-mode=false`
- AuthMe enabled for offline login/register.
- NobleWhitelist installed but disabled.
- LuckPerms storage is YAML.
- CoreProtect CE uses local SQLite.
- MiniMOTD owns server-list text and icon.

## Core And Plugins

| Component | Version | File |
| --- | --- | --- |
| Leaf | 1.21.11 build 158 | `leaf-1.21.11-158.jar` |
| AuthMeReloaded | 6.0.0 Paper | `plugins/AuthMe-6.0.0-Paper.jar` |
| PacketEvents | 2.12.2 | `plugins/packetevents-spigot-2.12.2.jar` |
| NobleWhitelist | 1.2.23 | `plugins/NobleWhitelist-1.2.23.jar` |
| Chunky | 1.4.40 | `plugins/Chunky-Bukkit-1.4.40.jar` |
| LuckPerms | 5.5.53 | `plugins/LuckPerms-Bukkit-5.5.53.jar` |
| Vault | 1.7.4 jar | `plugins/Vault-1.7.4.jar` |
| CMILib | 1.5.9.6 | `plugins/CMILib1.5.9.6.jar` |
| CMI | 9.8.7.7 | `plugins/CMI-9.8.7.7.jar` |
| WorldEdit | 7.4.2 | `plugins/worldedit-bukkit-7.4.2.jar` |
| ViaVersion | 5.9.1 | `plugins/ViaVersion-5.9.1.jar` |
| ViaBackwards | 5.9.1 | `plugins/ViaBackwards-5.9.1.jar` |
| ViaRewind | 4.1.1 | `plugins/ViaRewind-4.1.1.jar` |
| CoreProtect CE | 23.2 | `plugins/CoreProtect-CE-23.2.jar` |
| MiniMOTD | 2.2.3 | `plugins/minimotd-paper-2.2.3.jar` |

## Required Checks

- Verify `CHECKSUMS.txt` after pulling changes.
- Stop the server before pulling jar or config changes.
- Create a backup before pulling changes that touch jars, worlds, plugin configs, permissions, or startup scripts.
- Start the server and confirm the console reaches `Done`.
