package com.akhilesh.chessapp.service;

import com.akhilesh.chessapp.model.BitBoard;
import com.akhilesh.chessapp.model.ChessConstants;
import com.akhilesh.chessapp.model.Pair;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@Profile("bitboard") // <--- ACTIVATE THIS PROFILE TO SWITCH ENGINES
public class BitboardEngine implements ChessEngine {

    private final BitBoard bitBoard;

    public BitboardEngine(BitBoard bitBoard) {
        this.bitBoard = bitBoard;
    }

    @Override
    public void resetBoard() {
        bitBoard.resetBoard();
    }

    @Override
    public boolean makeMove(Pair from, Pair to, String promotion) {
        int fromIdx = from.getX() * 8 + from.getY();
        int toIdx = to.getX() * 8 + to.getY();
        int move = (fromIdx << 6) | toIdx;

        List<Integer> legalMoves = bitBoard.generateLegalMoves();
        if (legalMoves.contains(move)) {

            // Map String "n" -> ChessConstants.KNIGHT
            int promoType = ChessConstants.QUEEN; // Default
            if (promotion != null) {
                switch (promotion.toLowerCase()) {
                    case "n" -> promoType = ChessConstants.KNIGHT;
                    case "r" -> promoType = ChessConstants.ROOK;
                    case "b" -> promoType = ChessConstants.BISHOP;
                }
            }

            bitBoard.performBitwiseMove(fromIdx, toIdx, promoType);
            return true;
        }
        return false;
    }

    @Override
    public String getFen() {
        return bitBoard.toFEN(); // No longer hardcoded!
    }

    @Override
    public Map<Pair, HashSet<Pair>> getLegalMoves() {
        // --- THE BRIDGE ADAPTER ---
        // Converts Bitboard "Int" moves back to OOP "Pair" moves for the frontend

        Map<Pair, HashSet<Pair>> moveMap = new HashMap<>();
        List<Integer> rawMoves = bitBoard.generateLegalMoves();

        for (int move : rawMoves) {
            // Decode the move
            int fromIdx = (move >> 6) & 0x3F;
            int toIdx = move & 0x3F;

            // Convert int -> Pair
            Pair fromPair = indexToPair(fromIdx);
            Pair toPair = indexToPair(toIdx);

            // Add to Map
            moveMap.computeIfAbsent(fromPair, k -> new HashSet<>()).add(toPair);
        }
        return moveMap;
    }

    private Pair indexToPair(int index) {
        int row = index / 8;
        int col = index % 8;
        return new Pair(row, col);
    }

    @Override
    public HashSet<Pair> getLegalMovesForSquare(Pair square) {
        return getLegalMoves().getOrDefault(square, new HashSet<>());
    }

    @Override
    public Map<String, String> getPieceMap() {
        // We can leverage the FEN we already wrote!
        // Or we can add a helper to BitBoard. Let's do the BitBoard helper approach (Step 3).
        return bitBoard.toPieceMap();
    }

    @Override
    public String getEngineType() {
        return "BITBOARD_ENGINE";
    }
}