# GUI plugin selection, 2026-06-08

## Recommendation

Use CommandGUI 3.3.0 for the first YuHua服务器 player menu.

Reason: this server only needs a safe, lightweight command launcher for `/menu`
and a clock shortcut. CommandGUI directly supports Paper/Leaf 1.21-1.21.11,
has the exact permission split needed for normal players, and does not require
PlaceholderAPI or a larger menu framework.

## Options Checked

| Plugin | Current relevant version checked | Fit for YuHua服务器 1.21.11 | Notes |
| --- | --- | --- | --- |
| CommandGUI | 3.3.0 | Best first choice | Official Hangar page lists Paper 1.21-1.21.11 support. Provides `/commandgui`, `/commandgui tool`, configurable items, player/console execution, cooldowns, and separate permissions for use, tool, give, reload, and bypass. |
| CommandPanels | 4.1.6 for 1.21.11 | Best upgrade path for complex menus | More powerful GUI framework with inventory/dialog/Floodgate GUI support and an online editor. Good if YuHua服务器 later needs shops, quests, animations, conditions, or Bedrock-specific menus. It is more than needed for the current simple player utility menu. |
| DeluxeMenus | 1.14.1 | Mature, but not selected | Very mature and widely used, but the checked Hangar listing only advertises Paper 1.16-1.21.8. It also requires PlaceholderAPI. For a 1.21.11 server, it adds more dependency and version risk than CommandGUI. |

## Sources

- CommandGUI Hangar: https://hangar.papermc.io/Alfie51m/CommandGUI
- CommandGUI Modrinth v3.3.0: https://modrinth.com/plugin/commandgui/version/v3.3.0
- CommandPanels CurseForge: https://www.curseforge.com/minecraft/bukkit-plugins/commandpanels
- CommandPanels Modrinth versions: https://modrinth.com/plugin/commandpanels/versions
- DeluxeMenus Hangar: https://hangar.papermc.io/HelpChat/DeluxeMenus
- DeluxeMenus Spigot: https://www.spigotmc.org/resources/deluxemenus.11734/

## Implementation Decision

Keep the current implementation on CommandGUI:

- `/menu` opens the menu through CMI CustomAlias.
- `/menutool` gives the player a replacement clock tool.
- Normal players only receive `commandgui.use` and `commandgui.tool`.
- Do not grant `commandgui.reload`, `commandgui.give`, or
  `commandgui.bypass` to the default group.
- Run normal utility actions as the player whenever possible, so the menu does
  not bypass LuckPerms.

Revisit CommandPanels only if the server later needs multi-page menus, Bedrock
dialog support, online visual editing, advanced conditions, or animated menus.
