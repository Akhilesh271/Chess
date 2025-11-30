package com.akhilesh.chessapp.service;

import com.akhilesh.chessapp.model.Board;
import com.akhilesh.chessapp.model.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Gameplay {

    public enum GameStatus {
        ONGOING,
        CHECKMATE,
        STALEMATE
    }

    private final ChessEngine engine;
    private GameStatus status = GameStatus.ONGOING;

    @Autowired
    public Gameplay(ChessEngine engine) {
        this.engine = engine;
    }

    public void startNewGame() {
        engine.resetBoard();
        this.status = GameStatus.ONGOING;
    }

    public boolean makeMove(Pair from, Pair to, String promotion) {
        boolean moved = engine.makeMove(from, to, promotion);
        updateGameStatus(); // Check for checkmate after move
        return moved;
    }

    // --- COMPATIBILITY FIXES FOR CONTROLLER & AI ---

    // 1. Restore the game() method
    // Previously this calculated moves. Now the Engine does that.
    // We keep this method to satisfy the Controller, but it just updates status.
    public void game() {
        updateGameStatus();
    }

    // 2. Restore getBoard() for the AI
    public Board getBoard() {
        // The Strategy Pattern in action:
        // If we are using the OOP engine, we can return the Board object.
        if (engine instanceof OopEngine oopEngine) {
            return oopEngine.getBoard();
        }
        // If we are using Bitboards, the legacy AI won't work yet.
        System.err.println("WARNING: AI requested Board object but BitboardEngine is active.");
        return null;
    }

    // 3. Restore getStatus()
    public GameStatus getStatus() {
        return this.status;
    }

    // Helper to calculate status based on available moves
    private void updateGameStatus() {
        var legalMoves = engine.getLegalMoves();
        if (legalMoves.isEmpty()) {
            // If no legal moves, it's either Mate or Stalemate
            // (Simplification: We assume Checkmate for now, or you can ask Engine if in check)
            this.status = GameStatus.CHECKMATE;
        } else {
            this.status = GameStatus.ONGOING;
        }
    }

    public ChessEngine getEngine() {
        return engine;
    }
}