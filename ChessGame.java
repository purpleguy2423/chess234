package com.chess.game;

public class ChessGame {
    private ChessBoard board;
    private ChessAI ai;
    private boolean isPlayerTurn;
    
    public ChessGame() {
        this.board = new ChessBoard();
        this.ai = new ChessAI();
        this.isPlayerTurn = true;
    }
    
    public void initializeGame() {
        board.setupInitialPosition();
    }
    
    public boolean makeMove(String from, String to) {
        if (!isPlayerTurn) {
            return false;
        }
        
        boolean moveSuccess = board.makeMove(from, to);
        if (moveSuccess) {
            isPlayerTurn = false;
            makeAIMove();
        }
        return moveSuccess;
    }
    
    private void makeAIMove() {
        String aiMove = ai.calculateBestMove(board);
        board.makeMove(aiMove.substring(0, 2), aiMove.substring(2, 4));
        isPlayerTurn = true;
    }
}