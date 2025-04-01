package com.chess.game;

import java.util.*;

public class ChessBoard {
    private Piece[][] board;
    private boolean whiteToMove;
    private Position whiteKingPos;
    private Position blackKingPos;
    private boolean[] castlingRights; // [whiteKingside, whiteQueenside, blackKingside, blackQueenside]
    private Position enPassantTarget;
    private int halfMoveClock;
    private List<Move> moveHistory;

    public ChessBoard() {
        board = new Piece[8][8];
        castlingRights = new boolean[]{true, true, true, true};
        whiteToMove = true;
        moveHistory = new ArrayList<>();
        initializeBoard();
    }

    private void initializeBoard() {
        // Initialize pawns
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece(PieceType.PAWN, true);
            board[6][i] = new Piece(PieceType.PAWN, false);
        }

        // Initialize other pieces
        setupBackRank(0, true);
        setupBackRank(7, false);

        // Store initial king positions
        whiteKingPos = new Position(0, 4);
        blackKingPos = new Position(7, 4);
    }

    private void setupBackRank(int rank, boolean isWhite) {
        board[rank][0] = new Piece(PieceType.ROOK, isWhite);
        board[rank][1] = new Piece(PieceType.KNIGHT, isWhite);
        board[rank][2] = new Piece(PieceType.BISHOP, isWhite);
        board[rank][3] = new Piece(PieceType.QUEEN, isWhite);
        board[rank][4] = new Piece(PieceType.KING, isWhite);
        board[rank][5] = new Piece(PieceType.BISHOP, isWhite);
        board[rank][6] = new Piece(PieceType.KNIGHT, isWhite);
        board[rank][7] = new Piece(PieceType.ROOK, isWhite);
    }

    public boolean makeMove(String from, String to) {
        Position start = Position.fromAlgebraic(from);
        Position end = Position.fromAlgebraic(to);
        
        Move move = validateMove(start, end);
        if (move == null) {
            return false;
        }

        // Execute the move
        executeMove(move);
        return true;
    }

    private Move validateMove(Position start, Position end) {
        if (!isValidPosition(start) || !isValidPosition(end)) {
            return null;
        }

        Piece piece = board[start.rank][start.file];
        if (piece == null || piece.isWhite() != whiteToMove) {
            return null;
        }

        List<Move> legalMoves = generateLegalMoves(start);
        for (Move move : legalMoves) {
            if (move.getEnd().equals(end)) {
                return move;
            }
        }

        return null;
    }

    private List<Move> generateLegalMoves(Position pos) {
        List<Move> pseudoLegalMoves = generatePseudoLegalMoves(pos);
        List<Move> legalMoves = new ArrayList<>();

        for (Move move : pseudoLegalMoves) {
            if (!wouldBeInCheck(move)) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    private List<Move> generatePseudoLegalMoves(Position pos) {
        List<Move> moves = new ArrayList<>();
        Piece piece = board[pos.rank][pos.file];

        switch (piece.getType()) {
            case PAWN:
                addPawnMoves(pos, moves);
                break;
            case KNIGHT:
                addKnightMoves(pos, moves);
                break;
            case BISHOP:
                addBishopMoves(pos, moves);
                break;
            case ROOK:
                addRookMoves(pos, moves);
                break;
            case QUEEN:
                addQueenMoves(pos, moves);
                break;
            case KING:
                addKingMoves(pos, moves);
                break;
        }

        return moves;
    }

    private void addPawnMoves(Position pos, List<Move> moves) {
        int direction = board[pos.rank][pos.file].isWhite() ? 1 : -1;
        int startRank = board[pos.rank][pos.file].isWhite() ? 1 : 6;

        // Forward move
        Position oneForward = new Position(pos.rank + direction, pos.file);
        if (isValidPosition(oneForward) && board[oneForward.rank][oneForward.file] == null) {
            moves.add(new Move(pos, oneForward));

            // Double forward on first move
            if (pos.rank == startRank) {
                Position twoForward = new Position(pos.rank + 2 * direction, pos.file);
                if (board[twoForward.rank][twoForward.file] == null) {
                    moves.add(new Move(pos, twoForward));
                }
            }
        }

        // Captures
        for (int fileOffset : new int[]{-1, 1}) {
            Position capturePos = new Position(pos.rank + direction, pos.file + fileOffset);
            if (isValidPosition(capturePos)) {
                Piece targetPiece = board[capturePos.rank][capturePos.file];
                if (targetPiece != null && targetPiece.isWhite() != board[pos.rank][pos.file].isWhite()) {
                    moves.add(new Move(pos, capturePos));
                }
            }
        }

        // En passant
        if (enPassantTarget != null) {
            if (pos.rank == (board[pos.rank][pos.file].isWhite() ? 4 : 3)) {
                if (Math.abs(pos.file - enPassantTarget.file) == 1) {
                    moves.add(new Move(pos, enPassantTarget, MoveType.EN_PASSANT));
                }
            }
        }
    }

    private void addKnightMoves(Position pos, List<Move> moves) {
        int[][] knightOffsets = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] offset : knightOffsets) {
            Position newPos = new Position(pos.rank + offset[0], pos.file + offset[1]);
            if (isValidPosition(newPos)) {
                Piece targetPiece = board[newPos.rank][newPos.file];
                if (targetPiece == null || targetPiece.isWhite() != board[pos.rank][pos.file].isWhite()) {
                    moves.add(new Move(pos, newPos));
                }
            }
        }
    }

    private void addSlidingMoves(Position pos, List<Move> moves, int[][] directions) {
        for (int[] direction : directions) {
            Position current = new Position(
                pos.rank + direction[0],
                pos.file + direction[1]
            );

            while (isValidPosition(current)) {
                Piece targetPiece = board[current.rank][current.file];
                if (targetPiece == null) {
                    moves.add(new Move(pos, current));
                } else {
                    if (targetPiece.isWhite() != board[pos.rank][pos.file].isWhite()) {
                        moves.add(new Move(pos, current));
                    }
                    break;
                }
                current = new Position(
                    current.rank + direction[0],
                    current.file + direction[1]
                );
            }
        }
    }

    private void addBishopMoves(Position pos, List<Move> moves) {
        int[][] bishopDirections = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        addSlidingMoves(pos, moves, bishopDirections);
    }

    private void addRookMoves(Position pos, List<Move> moves) {
        int[][] rookDirections = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        addSlidingMoves(pos, moves, rookDirections);
    }

    private void addQueenMoves(Position pos, List<Move> moves) {
        addBishopMoves(pos, moves);
        addRookMoves(pos, moves);
    }

    private void addKingMoves(Position pos, List<Move> moves) {
        int[][] kingOffsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };

        // Regular moves
        for (int[] offset : kingOffsets) {
            Position newPos = new Position(pos.rank + offset[0], pos.file + offset[1]);
            if (isValidPosition(newPos)) {
                Piece targetPiece = board[newPos.rank][newPos.file];
                if (targetPiece == null || targetPiece.isWhite() != board[pos.rank][pos.file].isWhite()) {
                    moves.add(new Move(pos, newPos));
                }
            }
        }

        // Castling
        if (!isInCheck(whiteToMove)) {
            addCastlingMoves(pos, moves);
        }
    }

    private void addCastlingMoves(Position pos, List<Move> moves) {
        boolean isWhite = board[pos.rank][pos.file].isWhite();
        int rank = isWhite ? 0 : 7;

        // Kingside castling
        if (castlingRights[isWhite ? 0 : 2]) {
            if (board[rank][5] == null && board[rank][6] == null) {
                if (!isSquareAttacked(new Position(rank, 5), !isWhite) &&
                    !isSquareAttacked(new Position(rank, 6), !isWhite)) {
                    moves.add(new Move(pos, new Position(rank, 6), MoveType.CASTLE_KINGSIDE));
                }
            }
        }

        // Queenside castling
        if (castlingRights[isWhite ? 1 : 3]) {
            if (board[rank][1] == null && board[rank][2] == null && board[rank][3] == null) {
                if (!isSquareAttacked(new Position(rank, 2), !isWhite) &&
                    !isSquareAttacked(new Position(rank, 3), !isWhite)) {
                    moves.add(new Move(pos, new Position(rank, 2), MoveType.CASTLE_QUEENSIDE));
                }
            }
        }
    }

    private void executeMove(Move move) {
        Position start = move.getStart();
        Position end = move.getEnd();
        Piece piece = board[start.rank][start.file];

        // Update castling rights
        updateCastlingRights(start, end);

        // Handle special moves
        switch (move.getType()) {
            case CASTLE_KINGSIDE:
                executeCastling(true);
                break;
            case CASTLE_QUEENSIDE:
                executeCastling(false);
                break;
            case EN_PASSANT:
                executeEnPassant(start, end);
                break;
            default:
                // Standard move
                board[end.rank][end.file] = piece;
                board[start.rank][start.file] = null;
        }

        // Update king position if king moved
        if (piece.getType() == PieceType.KING) {
            if (piece.isWhite()) {
                whiteKingPos = end;
            } else {
                blackKingPos = end;
            }
        }

        // Update en passant target
        if (piece.getType() == PieceType.PAWN && Math.abs(end.rank - start.rank) == 2) {
            enPassantTarget = new Position((start.rank + end.rank) / 2, start.file);
        } else {
            enPassantTarget = null;
        }

        moveHistory.add(move);
        whiteToMove = !whiteToMove;
    }

    private boolean wouldBeInCheck(Move move) {
        // Make a copy of the current board state
        Piece[][] tempBoard = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            tempBoard[i] = board[i].clone();
        }

        // Make the move on the temporary board
        Position start = move.getStart();
        Position end = move.getEnd();
        tempBoard[end.rank][end.file] = tempBoard[start.rank][start.file];
        tempBoard[start.rank][start.file] = null;

        // Check if the king would be in check
        Position kingPos = board[start.rank][start.file].isWhite() ? whiteKingPos : blackKingPos;
        if (board[start.rank][start.file].getType() == PieceType.KING) {
            kingPos = end;
        }

        return isSquareAttacked(kingPos, !board[start.rank][start.file].isWhite());
    }

    private boolean isSquareAttacked(Position pos, boolean byWhite) {
        // Check for pawn attacks
        int pawnRank = byWhite ? pos.rank - 1 : pos.rank + 1;
        for (int fileOffset : new int[]{-1, 1}) {
            if (isValidPosition(pawnRank, pos.file + fileOffset)) {
                Piece piece = board[pawnRank][pos.file + fileOffset];
                if (piece != null && piece.getType() == PieceType.PAWN && piece.isWhite() == byWhite) {
                    return true;
                }
            }
        }

        // Check for knight attacks
        int[][] knightOffsets = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };
        for (int[] offset : knightOffsets) {
            int rank = pos.rank + offset[0];
            int file = pos.file + offset[1];
            if (isValidPosition(rank, file)) {
                Piece piece = board[rank][file];
                if (piece != null && piece.getType() == PieceType.KNIGHT && piece.isWhite() == byWhite) {
                    return true;
                }
            }
        }

        // Check for sliding piece attacks (bishop, rook, queen)
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
        };

        for (int[] direction : directions) {
            int rank = pos.rank + direction[0];
            int file = pos.file + direction[1];
            
            while (isValidPosition(rank, file)) {
                Piece piece = board[rank][file];
                if (piece != null) {
                    if (piece.isWhite() == byWhite) {
                        boolean isDiagonal = direction[0] != 0 && direction[1] != 0;
                        boolean isOrthogonal = direction[0] == 0 || direction[1] == 0;
                        
                        if ((isDiagonal && (piece.getType() == PieceType.BISHOP || piece.getType() == PieceType.QUEEN)) ||
                            (isOrthogonal && (piece.getType() == PieceType.ROOK || piece.getType() == PieceType.QUEEN))) {
                            return true;
                        }
                    }
                    break;
                }
                rank += direction[0];
                file += direction[1];
            }
        }

        return false;
    }

    private boolean isValidPosition(Position pos) {
        return isValidPosition(pos.rank, pos.file);
    }

    private boolean isValidPosition(int rank, int file) {
        return rank >= 0 && rank < 8 && file >= 0 && file < 8;
    }

    private void updateCastlingRights(Position start, Position end) {
        Piece piece = board[start.rank][start.file];
        
        // King move
        if (piece.getType() == PieceType.KING) {
            if (piece.isWhite()) {
                castlingRights[0] = false; // White kingside
                castlingRights[1] = false; // White queenside
            } else {
                castlingRights[2] = false; // Black kingside
                castlingRights[3] = false; // Black queenside
            }
        }
        
        // Rook move or capture
        if (start.rank == 0 && start.file == 0) castlingRights[1] = false; // White queenside rook
        if (start.rank == 0 && start.file == 7) castlingRights[0] = false; // White kingside rook
        if (start.rank == 7 && start.file == 0) castlingRights[3] = false; // Black queenside rook
        if (start.rank == 7 && start.file == 7) castlingRights[2] = false; // Black kingside rook
    }

    public boolean isInCheck(boolean white) {
        Position kingPos = white ? whiteKingPos : blackKingPos;
        return isSquareAttacked(kingPos, !white);
    }

    public boolean isCheckmate() {
        if (!isInCheck(whiteToMove)) {
            return false;
        }

        return getAllLegalMoves().isEmpty();
    }

    public boolean isStalemate() {
        if (isInCheck(whiteToMove)) {
            return false;
        }

        return getAllLegalMoves().isEmpty();
    }

    private List<Move> getAllLegalMoves() {
        List<Move> allMoves = new ArrayList<>();
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                Piece piece = board[rank][file];
                if (piece != null && piece.isWhite() == whiteToMove) {
                    allMoves.addAll(generateLegalMoves(new Position(rank, file)));
                }
            }
        }
        return allMoves;
    }
}
