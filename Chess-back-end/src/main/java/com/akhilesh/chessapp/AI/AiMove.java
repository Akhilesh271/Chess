package com.akhilesh.chessapp.AI;

import com.akhilesh.chessapp.model.Pair;
import com.akhilesh.chessapp.model.Pieces;

public class AiMove {
    private Pair from;
    private Pair to;
    private Pieces movedPiece; // Used by Legacy AI
    private int score;         // Used by Bitboard AI

    // 1. LEGACY CONSTRUCTOR (Keep this so ChessAI.java doesn't break)
    public AiMove(Pair from, Pair to, Pieces movedPiece) {
        this.from = from;
        this.to = to;
        this.movedPiece = movedPiece;
    }

    // 2. NEW BITBOARD CONSTRUCTOR (Add this for BitboardAI.java)
    public AiMove(Pair from, Pair to, int score) {
        this.from = from;
        this.to = to;
        this.score = score;
    }

    public Pair getFrom() {
        return from;
    }

    public Pair getTo() {
        return to;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "AiMove{" +
                "from=" + from +
                ", to=" + to +
                ", score=" + score +
                '}';
    }
}