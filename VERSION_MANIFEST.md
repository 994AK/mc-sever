# Version Manifest

## Release

- Server release: `leafmc-2026.06.07-r3`
- Date: `2026-06-07`
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
- CommandGUI owns `/commandgui` and `/cg`; CMI CustomAlias provides `/menu`, `/menutool`, and `/menu*` submenu entries.
- CMI CustomAlias provides `/projects`, `/daily`, `/guildhelp`, `/proposal`, `/projectrewards`, and `/projectsubmit`; CMI does not intercept `/guild`.
- BetterTeams owns light guild/team identity only; team teleport, bank, chest claims, score rewards, PvP toggles, rankup, and scoreboard team integration are disabled or permission-blocked.
- Quests owns daily/weekly light task prompts only; automatic rewards and contribution scoring are not used.
- CMI flight charge is enabled for default players through `/flyc`, `/flightcharge`, and `/flyspeed 1-3`; plain `/fly` and flight admin permissions remain blocked.
- CMI shulkerBackpack is enabled for crouch shulker opening; OpenShulk remains installed.
- SimpleChat owns public chat formatting; CMI chat formatting and click/hover chat are disabled.
- SkinsRestorer owns `/skin`; CMI skin module is disabled.
- TAB owns the in-game player list; CMI tablist remains disabled.
- Residence owns player land claims. GriefPrevention is retained only as disabled legacy data/config.
- BlueMap and LeafResidenceWeb are currently disabled; land claims use normal Residence in-game selection.
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
| SimpleChat | 1.2.0 | `plugins/SimpleChat-1.2.0.jar` |
| CommandGUI | 3.3.0 | `plugins/CommandGUI-3.3.0.jar` |
| SkinsRestorer | 15.12.0 | `plugins/SkinsRestorer-15.12.0.jar` |
| Residence | 6.0.1.8 | `plugins/Residence6.0.1.8.jar` |
| TAB | 6.0.3 Vanilla | `plugins/TAB-6.0.3-Vanilla.jar` |
| BetterTeams | 5.1.2 | `plugins/BetterTeams-5.1.2.jar` |
| Quests | 5.3.1-b564 | `plugins/Quests-5.3.1.jar` |
| OpenShulk | 1.21.x | `plugins/OpenShulk-1.21.x.jar` |
| JEI Recipe Bridge | 1.0.0 | `plugins/JEI-Recipe-Bridge-1.0.0.jar` |
| WorldEdit | 7.4.2 | `plugins/worldedit-bukkit-7.4.2.jar` |
| ViaVersion | 5.9.1 | `plugins/ViaVersion-5.9.1.jar` |
| ViaBackwards | 5.9.1 | `plugins/ViaBackwards-5.9.1.jar` |
| ViaRewind | 4.1.1 | `plugins/ViaRewind-4.1.1.jar` |
| CoreProtect CE | 23.2 | `plugins/CoreProtect-CE-23.2.jar` |
| MiniMOTD | 2.2.3 | `plugins/minimotd-paper-2.2.3.jar` |

Disabled jars: `plugins/GriefPrevention-16.18.7.jar.disabled`, `plugins/LeafResidenceWeb-1.0.0.jar.disabled`, `plugins/bluemap-5.16-paper.jar.disabled`.

## Required Checks

- Verify `CHECKSUMS.txt` after pulling changes.
- Stop the server before pulling jar or config changes.
- Create a backup before pulling changes that touch jars, worlds, plugin configs, permissions, or startup scripts.
- Start the server and confirm the console reaches `Done`.
