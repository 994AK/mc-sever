# LeafGomoku Room Platform Runtime Smoke Test

Use this after copying `plugins/LeafGomoku-0.1.0.jar` to the server and restarting. This test changes blocks in the configured room footprint, so run it on a backed-up local copy or an intended test build first.

## Preflight

- Stop the server before replacing `plugins/LeafGomoku-0.1.0.jar`.
- Back up `plugins/LeafGomoku/config.yml`, `plugins/LeafGomoku/rooms.yml`, `plugins/LeafGomoku/data.db`, and legacy `plugins/LeafGomoku/stats.yml` if they already exist.
- Do not copy a test `data.db` over a real server. `data.db` owns player stats, ranking data, match history, appearance unlocks, preferences, and point ledger rows.
- Confirm PlaceholderAPI is optional: the plugin should enable whether `plugins/PlaceholderAPI-2.12.2.jar` is present or absent.
- Start the server and confirm the console reaches `Done`.
- Confirm LeafGomoku enables without a Java exception. A disabled-room warning is acceptable only for a deliberately invalid test room.

## Legacy Main Migration

As an admin:

```text
/gomoku admin reload
/gomoku admin inspect main
/gomoku admin init main
/gomoku status main
```

Expected:

- If `rooms.yml` did not exist, the legacy `config.yml` `arena:` block is migrated into room `main`.
- `/gomoku admin inspect main` shows state, black/white seats, spectator count, current turn, and config status.
- `/gomoku admin init main` clears the 15x15 entity board to the configured board theme. No preview wall is generated.
- `/gomoku status main` reports the room as open or ready.

## GUI Integration

As a default player:

```text
/menugomoku
/gomokumenu
/wzq
/gomoku gui
/gomoku
```

Expected:

- `/menugomoku`, `/gomokumenu`, `/wzq`, `/gomoku gui`, and player `/gomoku` open the LeafGomoku built-in dynamic room lobby and list every room from `plugins/LeafGomoku/rooms.yml`.
- DeluxeMenus `/menu` integration is optional. LeafGomoku updates do not require editing `plugins/DeluxeMenus/gui_menus/*.yml`.
- `%leafgomoku_room_count%` can still be used by any external menu when PlaceholderAPI is installed.
- The lobby is paged and can show up to 28 room cards per page.
- Clicking a room card opens a room detail page.
- The detail page has separate `加入对局`, `只观战`, and `输出状态` buttons.
- The detail page has `棋盘主题` and `棋子皮肤` entries.
- The skin menu lets default players select free skins or exchange premium skins with Gomoku points.
- The theme menu lets a player switch a non-active room to an unlocked board theme; locked themes show their point cost.
- If the room cannot accept participants, `加入对局` is disabled while `只观战` remains available when the room is usable.
- GUI actions do not bypass permissions: player buttons still execute `/gomoku ...`, and admin setup/reset/release remains guarded by `leafgomoku.admin.*`.

## Create A Room From Admin Position

Stand at a safe test location and face the intended board direction. Use the right-click placement flow first.

```text
/gomoku admin place testroom
# right-click the floor/block face where the room anchor should be placed
/gomoku admin init testroom
/gomoku admin inspect testroom
```

Expected:

- `testroom` is added to `plugins/LeafGomoku/rooms.yml`.
- The generated board uses the clicked anchor, current block Y, and the admin player's facing direction.
- The generated black seat, white seat, spectator spawn, emitters, board, safety floor, and glass room frame do not overlap unsafe terrain.
- The generated room config does not contain a `preview` section.
- The placement flow does not consume the player's held item and does not use the held block as a material source.
- Duplicate `/gomoku admin create testroom` is rejected.

## Participant And Spectator Flow

Use two default-player accounts and one observer.

```text
# Player A
/gomoku join testroom

# Observer
/gomoku spectate testroom

# Player B
/gomoku join testroom
```

Expected:

- Player A becomes black and teleports to the black seat.
- Observer teleports to the spectator spawn and does not occupy black or white.
- Player B becomes white and teleports to the white seat.
- The match starts with black's turn.
- Participants and observers are temporarily in the `testroom` chat channel.
- Observer clicks on a board cell and is rejected without changing the board.
- A fourth player cannot replace either participant while the match is active.

## Move Flow

1. White clicks an empty board cell before black moves.
2. Black clicks an empty board cell.
3. White clicks the same occupied cell.
4. White clicks a different empty cell.

Expected:

- Step 1 is rejected with no board change.
- Step 2 launches black's selected piece skin from the black emitter, then writes the clicked board cell.
- Step 3 is rejected with no turn change.
- Step 4 launches white's selected piece skin from the white emitter, then writes the clicked board cell.
- Legal moves broadcast the room id, move coordinate, and next turn only to the room channel.
- A player outside `testroom` does not receive move broadcasts or room chat.

## Lifecycle Operations

Run these as an admin on a non-production room.

```text
/gomoku admin refresh testroom
/gomoku admin inspect testroom
/gomoku admin release testroom black
/gomoku admin reset testroom
/gomoku admin delete testroom
```

Expected:

- `refresh` re-renders the current board state without releasing seats, spectators, turn state, or stats.
- `release` refuses during active play unless using a forfeit/force path.
- `reset` refuses during active play unless `force` is used by a sender with `leafgomoku.admin.force`.
- `delete` refuses while waiting/playing/ended unless `force` is used by a sender with `leafgomoku.admin.force`.
- After a forced reset/delete, players, spectators, room channel membership, and current match skins are cleaned up; `rooms.yml` changes only for delete.

## End, Stats, And Variables

Create a quick five-in-a-row using legal alternating moves, or use an admin forfeit after validating several moves.

```text
/gomoku leaderboard points
/gomoku stats <winner>
/gomoku var room_main_state
/gomoku var player_points <winner>
```

Expected:

- A five-in-a-row announces the winning side and locks further moves.
- A winning match launches configured fireworks.
- Normal win/draw/forfeit writes stats once.
- Normal win/draw/forfeit writes a `match_history` row and `match_moves` rows in `plugins/LeafGomoku/data.db`.
- Admin stop/abort, refresh, and forced reset do not write wins/losses/draws.
- After `gameplay.auto-reset-ticks`, the room clears board blocks, releases seats and spectators, clears the room channel, and returns to open/ready.
- `/gomoku var ...` returns room/player values without requiring PlaceholderAPI.
- If PlaceholderAPI is installed, `%leafgomoku_room_main_state%` and `%leafgomoku_player_points%` resolve.

## Protection

As a default player:

- Try to break a board cell.
- Try to place a block on a board cell.
- Try to break or place on the glass frame and safety floor.
- Try to break or place outside the configured Gomoku footprint.

Expected:

- Board, room shell, floor, and emitter edits are cancelled.
- Outside-footprint edits behave normally.
- Admins need `leafgomoku.admin.bypass-protection` for direct build edits in protected areas.

## Notes

- The plugin intentionally does not provide economy payouts, item rewards, flight rewards, or combat-power rewards.
- Active unfinished matches are runtime state; server restart does not restore an unfinished board as a resumable match.
- `stats.yml` is a legacy migration source. v2 writes dynamic data to `data.db`.
