package net.leafmc.gomoku;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MatchController {
    private final GomokuBoard board;
    private final GomokuRules rules = new GomokuRules();

    private GameState state = GameState.IDLE;
    private UUID blackPlayer;
    private UUID whitePlayer;
    private Stone currentTurn = Stone.EMPTY;
    private Stone winner = Stone.EMPTY;
    private boolean draw;

    public MatchController() {
        this(GomokuBoard.DEFAULT_SIZE);
    }

    public MatchController(int boardSize) {
        this.board = new GomokuBoard(boardSize);
    }

    public JoinResult join(UUID playerId) {
        if (state == GameState.ENDED) {
            return new JoinResult(JoinStatus.MATCH_ENDED, sideFor(playerId), state, currentTurn);
        }
        if (playerId.equals(blackPlayer)) {
            return new JoinResult(JoinStatus.ALREADY_JOINED, Stone.BLACK, state, currentTurn);
        }
        if (playerId.equals(whitePlayer)) {
            return new JoinResult(JoinStatus.ALREADY_JOINED, Stone.WHITE, state, currentTurn);
        }
        if (blackPlayer == null) {
            blackPlayer = playerId;
            currentTurn = Stone.BLACK;
            state = GameState.WAITING_FOR_WHITE;
            return new JoinResult(JoinStatus.JOINED_BLACK, Stone.BLACK, state, currentTurn);
        }
        if (whitePlayer == null) {
            whitePlayer = playerId;
            state = GameState.PLAYING;
            currentTurn = Stone.BLACK;
            return new JoinResult(JoinStatus.JOINED_WHITE, Stone.WHITE, state, currentTurn);
        }
        return new JoinResult(JoinStatus.MATCH_FULL, Stone.EMPTY, state, currentTurn);
    }

    public boolean leave(UUID playerId) {
        if (!playerId.equals(blackPlayer) && !playerId.equals(whitePlayer)) {
            return false;
        }
        reset();
        return true;
    }

    public MoveResult play(UUID playerId, int row, int column) {
        if (!board.isInside(row, column)) {
            return MoveResult.rejected(MoveStatus.OUT_OF_BOUNDS, currentTurn);
        }
        if (state == GameState.ENDED) {
            return MoveResult.rejected(MoveStatus.ENDED, currentTurn);
        }
        if (state != GameState.PLAYING) {
            return MoveResult.rejected(MoveStatus.NOT_STARTED, currentTurn);
        }

        Stone side = sideFor(playerId);
        if (side == Stone.EMPTY) {
            return MoveResult.rejected(MoveStatus.NOT_PLAYING, currentTurn);
        }
        if (side != currentTurn) {
            return MoveResult.rejected(MoveStatus.WRONG_TURN, currentTurn);
        }
        if (!board.isEmpty(row, column)) {
            return MoveResult.rejected(MoveStatus.OCCUPIED, currentTurn);
        }

        board.place(row, column, side);
        List<GridCell> winningLine = rules.winningLineAfterMove(board, row, column);
        winner = winningLine.isEmpty() ? Stone.EMPTY : side;
        draw = winner == Stone.EMPTY && board.isFull();
        if (winner != Stone.EMPTY || draw) {
            state = GameState.ENDED;
        } else {
            currentTurn = currentTurn.opposite();
        }
        return new MoveResult(MoveStatus.ACCEPTED, side, row, column, winner, draw, currentTurn, winningLine);
    }

    public UndoResult undoLastMove(Stone side, int row, int column) {
        if (!board.isInside(row, column)) {
            return UndoResult.rejected(UndoStatus.OUT_OF_BOUNDS, currentTurn);
        }
        if (state == GameState.ENDED) {
            return UndoResult.rejected(UndoStatus.ENDED, currentTurn);
        }
        if (state != GameState.PLAYING) {
            return UndoResult.rejected(UndoStatus.NOT_STARTED, currentTurn);
        }
        if (side == Stone.EMPTY) {
            return UndoResult.rejected(UndoStatus.INVALID_STONE, currentTurn);
        }

        Stone existing = board.get(row, column);
        if (existing == Stone.EMPTY) {
            return UndoResult.rejected(UndoStatus.EMPTY_CELL, currentTurn);
        }
        if (existing != side) {
            return UndoResult.rejected(UndoStatus.STONE_MISMATCH, currentTurn);
        }

        board.clearCell(row, column);
        winner = Stone.EMPTY;
        draw = false;
        currentTurn = side;
        return new UndoResult(UndoStatus.ACCEPTED, side, row, column, currentTurn);
    }

    public void reset() {
        board.clear();
        state = GameState.IDLE;
        blackPlayer = null;
        whitePlayer = null;
        currentTurn = Stone.EMPTY;
        winner = Stone.EMPTY;
        draw = false;
    }

    public void forceEnd(Stone winner, boolean draw) {
        if (state != GameState.PLAYING && state != GameState.WAITING_FOR_WHITE) {
            return;
        }
        this.winner = winner;
        this.draw = draw;
        state = GameState.ENDED;
        currentTurn = Stone.EMPTY;
    }

    public Stone sideFor(UUID playerId) {
        if (playerId == null) {
            return Stone.EMPTY;
        }
        if (playerId.equals(blackPlayer)) {
            return Stone.BLACK;
        }
        if (playerId.equals(whitePlayer)) {
            return Stone.WHITE;
        }
        return Stone.EMPTY;
    }

    public Optional<UUID> playerFor(Stone side) {
        return switch (side) {
            case BLACK -> Optional.ofNullable(blackPlayer);
            case WHITE -> Optional.ofNullable(whitePlayer);
            case EMPTY -> Optional.empty();
        };
    }

    public GomokuBoard board() {
        return board;
    }

    public int boardSize() {
        return board.size();
    }

    public GameState state() {
        return state;
    }

    public Stone currentTurn() {
        return currentTurn;
    }

    public Stone winner() {
        return winner;
    }

    public boolean draw() {
        return draw;
    }
}
