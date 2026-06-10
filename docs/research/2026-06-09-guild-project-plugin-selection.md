# Guild Project Plugin Selection

Date: 2026-06-09

## Decision

Use two mature plugins as support pieces:

- BetterTeams 5.1.2 for light guild/team identity.
- Quests 5.3.1 for daily/weekly task entry.

Do not use them as the product shape. The server's activity loop remains a public project system with manual participation confirmation and non-combat rewards.

## Why BetterTeams

BetterTeams fits the "people are social" direction without forcing a heavy town/nation system. It gives players a team/guild name, invite/join/leave flows, and team chat.

Configured boundaries:

- `prefix: false` so it does not rewrite chat prefixes.
- `useTeams: false` and `displayTeamName: false` so it does not conflict with TAB scoreboard teams.
- `useVault: false`, `maxBal: 0`, and bank permissions denied.
- `noTeleport: true`, team home/warp/anchor permissions denied.
- `maxChests: 0` and team chest claim permissions denied.
- kill/death score set to 0 and rankup/top permissions denied.

## Why Quests

Quests is useful for player-facing daily/weekly prompts and simple progress objectives. It is not used for guild contribution scoring because public project contribution is not reliably measurable by blocks, kills, online time, or item counts.

Configured boundaries:

- Chinese language enabled.
- Default example quests removed.
- No money, exp, strong items, gear, or flight rewards.
- Admin/editor/action/condition/top/compass permissions denied for default players.
- Tasks only remind and lightly guide players: daily project check-in, project wall support, redstone material support.

## Rejected Directions

- Towny/Lands/HuskTowns: too heavy for the current identity; they introduce land politics, taxes, claims, nations, or war-shaped assumptions.
- mcMMO/AuraSkills/Jobs Reborn: pushes the server toward personal progression, skill power, job economy, and grind loops.
- EliteMobs/RPG-style packs: directly conflicts with "not combat power" and "no overpowered weapons".
- CommandPanels: useful later for richer menus, but current CommandGUI can carry the v1 entry points without replacing the menu stack.

## Sources

- BetterTeams Spigot page: https://www.spigotmc.org/resources/better-teams.17129/
- BetterTeams team-level configuration: https://betterteams.booksaw.dev/docs/configuration/Team-Levels/
- Quests GitHub: https://github.com/PikaMug/Quests
- Quests Modrinth page: https://modrinth.com/plugin/quests
