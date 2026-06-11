# Guild Project Runtime Smoke Test

Created: 2026-06-09

Run this after copying `deploy/guild-project-system-20260609-233455/` into the server directory and restarting the server.

## Console Checks

Confirm the server reaches `Done`, then check startup logs for:

- BetterTeams jar remains disabled unless guild/team features are explicitly re-enabled.
- Quests enabled.
- CMI loaded CustomAlias without errors.
- DeluxeMenus loaded all `gui_menus/*.yml` menus without errors.
- PlaceholderAPI loaded before DeluxeMenus or DeluxeMenus reported PlaceholderAPI as available.
- LuckPerms loaded the `default` group.

Do not continue if Quests fails to load. If BetterTeams was intentionally re-enabled for this test, do not continue if it fails to load.

## Default Player Checks

Use a normal `default` test account, not `admin` or `builder`.

### In-Game Entry

Run:

```text
/menu
/projects
/daily
/guildhelp
/proposal
/projectrewards
/projectsubmit
/rules
```

Expected:

- `/menu` opens the modular hub and can enter `项目日活`, `领地保护`, `规则与帮助`.
- `/projects` shows W1-O1 and W1-P1.
- `/daily` shows the three Quests task prompts and runs `/quests list`.
- `/guildhelp` shows the current guild pause notice and replacement route.
- `/projectrewards` clearly says no flight, no overpowered gear, no admin powers.
- `/rules` points to project/guild/task commands, not flight commands.

### Quests

Run:

```text
/quests list
/quests take 每日项目签到
```

Then type in chat:

```text
项目已读
```

Expected:

- The daily project check-in can be accepted.
- The task completes after the phrase.
- No items, money, exp, flight, permissions, or strong rewards are granted.

Optional weekly checks:

```text
/quests take 每周项目墙协助
/quests take 每周红石材料协助
```

Expected:

- Tasks can be accepted.
- Completion messages point back to project lead / management confirmation.
- Completing a Quests task alone does not count as guild contribution.

### BetterTeams

Run with two normal test players only after guild features are explicitly re-enabled:

```text
/team create TestTeam
/team invite <otherPlayer>
/team join TestTeam
/team chat 测试
/team info TestTeam
/team list
```

Expected:

- A team can be created and joined.
- Team chat works.
- Team identity exists for project records.

Then verify blocked features:

```text
/team home
/team sethome
/team warp
/team setwarp test
/team echest
/team baltop
/team rankup
/team pvp
```

Expected:

- These commands are denied or unavailable for default players.
- No team teleport, team bank, team chest, PvP toggle, or rankup gameplay is available.

Clean up:

```text
/team disband
/team disband
```

BetterTeams may require the command twice for confirmation.

### Flight Boundary

Run:

```text
/fly
/flyc
/flightcharge
/flyspeed 3
/tfly
```

Expected:

- `/fly` and `/tfly` are denied or unavailable for the default player.
- `/flyc`, `/flightcharge`, and `/flyspeed 3` are available for the default player.
- Online time grants normal consumable flight charge.
- `/menu`, `/rules`, and `/projects` do not advertise free `/fly`, admin flight, or extra flight charge as a project reward.

## Pass Criteria

Pass the rollout only if:

- Players can find projects from `/menu` and `/projects`.
- BetterTeams remains disabled, or if explicitly re-enabled for a later guild test, players can create or join a BetterTeams team.
- Players can accept at least the daily Quests task.
- Project rewards remain messages/recognition only at the plugin level.
- Free `/fly`, admin flight, and BetterTeams convenience power features are blocked for default players.
- There are no console errors from BetterTeams, Quests, DeluxeMenus, PlaceholderAPI, CMI CustomAlias, or LuckPerms.
