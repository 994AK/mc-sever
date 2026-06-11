# GUI plugin selection, updated 2026-06-10

## Recommendation

Use DeluxeMenus 1.14.1 for the YuHua服务器 player menu.

This supersedes the earlier 2026-06-08 CommandGUI recommendation. The current
implementation removes CommandGUI completely and lets DeluxeMenus register the
player menu commands directly: `/menu`, `/menunav`, `/menuteleport`,
`/menuhome`, `/menuland`, `/menuprojects`, `/menuprofile`, `/menusocial`, and
`/menuhelp`.

## Options Checked

| Plugin | Current relevant version checked | Fit for YuHua服务器 1.21.11 | Notes |
| --- | --- | --- | --- |
| DeluxeMenus | 1.14.1-Release | Current implementation | Mature menu framework. `open_command` must be a single word, `register_command: true` registers player-facing commands after restart, external files are loaded through `gui_menus.<id>.file`. Menu buttons use action tags such as `[player]`, `[message]`, `[openguimenu]`, and `[close]`. PlaceholderAPI 2.12.2 is installed for the documented dependency, even though the current menus avoid custom `%...%` expansions. Hangar currently advertises Paper 1.16-1.21.8, so Leaf 1.21.11 still needs runtime confirmation. |
| CommandGUI | 3.3.0 | Removed | Previously used for the first simple menu and menu clock, but now deleted from active plugins/configs by operator decision. |
| CommandPanels | 4.1.6 for 1.21.11 | Later upgrade path | More powerful GUI/dialog framework, still useful if the server later needs shops, Bedrock/Floodgate dialogs, or online visual editing. Not used in the current migration. |

## Sources

- DeluxeMenus GUI options: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/gui
- DeluxeMenus item options: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/item
- DeluxeMenus actions: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/actions
- DeluxeMenus Hangar: https://hangar.papermc.io/HelpChat/DeluxeMenus
- CommandGUI Hangar: https://hangar.papermc.io/Alfie51m/CommandGUI
- CommandPanels CurseForge: https://www.curseforge.com/minecraft/bukkit-plugins/commandpanels

## Implementation Decision

- `plugins/DeluxeMenus/config.yml` loads all menu files from
  `plugins/DeluxeMenus/gui_menus/` via `gui_menus.<id>.file`.
- Every player menu uses a single-word `open_command` and `register_command:
  true`, so normal players do not need `/dm open`.
- Menu-to-menu navigation uses `[openguimenu] <menu>`.
- Direct utility actions run as the player with `[player] <command>` to preserve
  LuckPerms boundaries.
- Parameterized commands do not auto-run; the menu sends a `[message]` prompt
  with the command the player should type.
- `/menutool` remains a CMI alias that gives a reminder clock and points players
  back to `/menu`; it no longer depends on CommandGUI's right-click tool.
