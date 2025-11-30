package com.akhilesh.chessapp.service;

import com.akhilesh.chessapp.model.Board;
import com.akhilesh.chessapp.model.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
@Profile("oop") // <--- This allows us to swap it out later!
public class OopEngine implements ChessEngine {

    private Board board;
    // ... inside OopEngine class ...
    public Board getBoard() {
        return this.board;
    }

    public OopEngine() {
        this.board = new Board(); // Initialize your existing OOP Board
    }

    @Override
    public void resetBoard() {
        this.board = new Board();
    }

    @Override
    public boolean makeMove(Pair from, Pair to, String promotion) {
        // Use your existing Board.java logic
        // We must calculate legal moves first as your Board relies on them
        HashMap<Pair, HashSet<Pair>> legalMoves = board.getAllLegalMoves(board.getBoard());
        board.setLegalMoves(legalMoves);

        if (board.getLegalMoves().containsKey(from) &&
                board.getLegalMoves().get(from).contains(to)) {

            if (promotion != null) {
                board.setPendingPromotion(promotion);
            }

            boolean moved = board.move(from, to);
            if (moved) {
                board.switchTurn();
            }
            return moved;
        }
        return false;
    }

    @Override
    public Map<Pair, HashSet<Pair>> getLegalMoves() {
        // Recalculate legal moves using your existing logic
        return board.getAllLegalMoves(board.getBoard());
    }

    @Override
    public HashSet<Pair> getLegalMovesForSquare(Pair square) {
        return board.highlightMove(square.getX(), square.getY());
    }

    @Override
    public String getFen() {
        return board.toFEN();
    }

    @Override
    public Map<String, String> getPieceMap() {
        return board.toPieceMap();
    }

    @Override
    public String getEngineType() {
        return "OOP_ENGINE";
    }
}