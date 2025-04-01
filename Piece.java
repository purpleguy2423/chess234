package com.chess.game;

public class Piece {
    private final PieceType type;
    private final boolean isWhite;

    public Piece(PieceType type, boolean isWhite) {
        this.type = type;
        this.isWhite = isWhite;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isWhite() {
        return isWhite;
    }
}