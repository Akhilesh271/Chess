package com.akhilesh.chessapp.service;

import com.akhilesh.chessapp.model.Pair;
import java.util.HashSet;
import java.util.Map;

public interface ChessEngine {
    // Resets the board to the starting position
    void resetBoard();

    // Makes a move. Returns true if successful.
    boolean makeMove(Pair from, Pair to, String promotion);

    // Returns all legal moves for the current board state
    Map<Pair, HashSet<Pair>> getLegalMoves();

    // Returns the legal moves for a specific piece (for highlighting)
    HashSet<Pair> getLegalMovesForSquare(Pair square);

    // Returns the board state (FEN string is best for frontend sync)
    String getFen();

    // Returns the board visual map (for your current API compatibility)
    Map<String, String> getPieceMap();

    // To identify which engine is running
    String getEngineType();
}