# LeafGomoku Consent Undo Smoke Test

Use this after deploying a build that includes `/gomoku undo`. The feature only reverts the last move, and the opponent must approve it before the board changes.

## Preflight

- Start the server and confirm LeafGomoku enables without exceptions.
- Confirm `plugins/LeafGomoku/config.yml` has `arena.gameplay.undo-request-timeout-ticks`. The default is `300` ticks, which is 15 seconds at 20 TPS.
- Use a non-production room or a copied test server.
- Join with two default-player accounts.

```text
/gomoku admin init testroom

# Player A
/gomoku join testroom

# Player B
/gomoku join testroom
```

Expected:

- Player A is black, Player B is white.
- The match starts with black's turn.
- Both players can see room-channel messages.

## Opponent Approval Flow

```text
# Black
# right-click any empty cell, for example H8
/gomoku undo testroom

# White
/gomoku undo accept testroom
```

Expected:

- The request does not change the board immediately.
- White receives an approval prompt.
- After White accepts, the last black stone is removed.
- The move is removed from the current match history.
- The current turn returns to black.
- Room members see the accepted undo message.

## Self-Approval Is Rejected

```text
# Black places one move
/gomoku undo testroom
/gomoku undo accept testroom
```

Expected:

- Black cannot approve their own request.
- The pending undo request remains waiting for White.
- The board and turn do not change.

## Wrong Player Cannot Request

```text
# Black places one move; it is now White's turn
# White tries to request undo
/gomoku undo testroom
```

Expected:

- White is rejected because only the last mover can request undo.
- The board and turn do not change.

## Opponent Denial Flow

```text
# Black places one move
/gomoku undo testroom

# White
/gomoku undo deny testroom
```

Expected:

- The pending request is cleared.
- The board and turn do not change.
- Room members see the denied undo message.

## Timeout Flow

```text
# Black places one move
/gomoku undo testroom
# wait longer than arena.gameplay.undo-request-timeout-ticks
/gomoku undo accept testroom
```

Expected:

- The request expires automatically.
- The late accept command reports that no pending request exists.
- The board and turn do not change.

## Continuing Play Cancels Pending Undo

```text
# Black places one move
/gomoku undo testroom

# White ignores the request and places a legal move
```

Expected:

- The pending undo request is cancelled because the board progressed.
- The white move remains on the board.
- Black must make a new request only after making the next last move.

## Lifecycle Cancellation

Create a pending undo request, then test each operation separately:

```text
/gomoku admin reset testroom force
/gomoku admin stop testroom
/gomoku leave
```

Expected:

- Pending undo is cancelled by reset, stop, forfeit, seat release through match reset, room delete, and plugin shutdown.
- A later `/gomoku undo accept testroom` does not apply an old request.

## Ended Match Is Locked

Finish a normal five-in-a-row or use an admin forfeit after validating move flow.

```text
/gomoku undo testroom
```

Expected:

- Undo is rejected after the room enters `ENDED`.
- Match stats and reset countdown are not rolled back.
- The final result remains the source of truth.

## GUI Check

Open the room detail page during an active match:

```text
/gomoku gui
```

Expected:

- A participant can see an `申请悔棋` button during active play when there is no pending request.
- The requester sees a waiting state after requesting undo.
- The opponent sees `同意悔棋` and `拒绝悔棋` buttons while the request is pending.
- Spectators cannot approve or reject undo requests.
