package com.chess.game;

public class Move {
    private final Position start;
    private final Position end;
    private final MoveType type;

    public Move(Position start, Position end) {
        this(start, end, MoveType.NORMAL);
    }

    public Move(Position start, Position end, MoveType type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

    public MoveType getType() {
        return type;
    }
}