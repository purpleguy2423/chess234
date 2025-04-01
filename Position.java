package com.chess.game;

public class Position {
    public final int rank;
    public final int file;

    public Position(int rank, int file) {
        this.rank = rank;
        this.file = file;
    }

    public static Position fromAlgebraic(String algebraic) {
        int file = algebraic.charAt(0) - 'a';
        int rank = Character.getNumericValue(algebraic.charAt(1)) - 1;
        return new Position(rank, file);
    }

    public String toAlgebraic() {
        char file = (char) ('a' + this.file);
        char rank = (char) ('1' + this.rank);
        return "" + file + rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return rank == position.rank && file == position.file;
    }

    @Override
    public int hashCode() {
        return 31 * rank + file;
    }
}