# GUI plugin selection, updated 2026-06-12

## Recommendation

No replacement menu engine is selected right now. Keep the existing
DeluxeMenus YAML only as historical reference until a simpler direction is
chosen.

## Options Checked

| Plugin | Current relevant version checked | Fit for YuHua服务器 1.21.11 | Notes |
| --- | --- | --- | --- |
| DeluxeMenus | 1.14.1-Release | Historical reference only | Mature menu framework. `open_command` must be a single word, `register_command: true` registers player-facing commands after restart, external files are loaded through `gui_menus.<id>.file`. Menu buttons use action tags such as `[player]`, `[message]`, `[openguimenu]`, and `[close]`. PlaceholderAPI 2.12.2 is installed for the documented dependency, even though the current menus avoid custom `%...%` expansions. Hangar currently advertises Paper 1.16-1.21.8, so Leaf 1.21.11 still needs runtime confirmation. |
| CommandGUI | 3.3.0 | Removed | Previously used for the first simple menu and menu clock, but now deleted from active plugins/configs by operator decision. |
| CommandPanels | 4.1.6 for 1.21.11 | Later upgrade path | More powerful GUI/dialog framework, still useful if the server later needs shops, Bedrock/Floodgate dialogs, or online visual editing. Not used in the current migration. |

## Sources

- DeluxeMenus GUI options: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/gui
- DeluxeMenus item options: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/item
- DeluxeMenus actions: https://wiki.helpch.at/helpchat-plugins/deluxemenus/options-and-configurations/actions
- DeluxeMenus Hangar: https://hangar.papermc.io/HelpChat/DeluxeMenus
- CommandGUI Hangar: https://hangar.papermc.io/Alfie51m/CommandGUI
- CommandPanels CurseForge: https://www.curseforge.com/minecraft/bukkit-plugins/commandpanels

## Historical DeluxeMenus Implementation Notes

- `plugins/DeluxeMenus/config.yml` loaded all menu files from
  `plugins/DeluxeMenus/gui_menus/` via `gui_menus.<id>.file`.
- Every player menu used a single-word `open_command` and `register_command:
  true`, so normal players did not need `/dm open`.
- Menu-to-menu navigation used `[openguimenu] <menu>`.
- Direct utility actions ran as the player with `[player] <command>` to preserve
  LuckPerms boundaries.
- Parameterized commands did not auto-run; the menu sent a `[message]` prompt
  with the command the player should type.
- `/menutool` remained a CMI alias that gave a reminder clock and pointed
  players back to `/menu`; it no longer depended on CommandGUI's right-click
  tool.
